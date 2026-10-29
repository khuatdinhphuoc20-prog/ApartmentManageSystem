-- Run once per developer SQL Server instance. This script creates the shared
-- development database name when needed, then initializes an empty schema.
USE [master];
GO
IF DB_ID(N'RentalApartmentDev') IS NULL
    EXEC(N'CREATE DATABASE [RentalApartmentDev]');
GO
USE [RentalApartmentDev];
GO
IF EXISTS (SELECT 1 FROM sys.tables WHERE schema_id = SCHEMA_ID(N'dbo'))
    THROW 51003, 'RentalApartmentDev already has tables; schema.sql must run only once.', 1;

-- Filtered indexes require these SQL Server session options.
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

CREATE TABLE dbo.users (
 id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_users PRIMARY KEY,
 email NVARCHAR(254) COLLATE Latin1_General_100_CS_AS NOT NULL,
 password_hash VARCHAR(255) NOT NULL,
 full_name NVARCHAR(150) NOT NULL,
 phone VARCHAR(20) NOT NULL,
 identity_document_no NVARCHAR(30) NOT NULL,
 role VARCHAR(16) NOT NULL,
 created_at DATETIME2(3) NOT NULL,
 updated_at DATETIME2(3) NOT NULL,
 CONSTRAINT ck_users_role CHECK (role IN ('USER','ADMIN')),
 CONSTRAINT ck_users_required CHECK (LEN(LTRIM(RTRIM(email))) > 0 AND LEN(LTRIM(RTRIM(password_hash))) > 0 AND LEN(LTRIM(RTRIM(full_name))) > 0 AND LEN(LTRIM(RTRIM(phone))) > 0 AND LEN(LTRIM(RTRIM(identity_document_no))) > 0),
 CONSTRAINT uq_users_email UNIQUE (email)
);
CREATE UNIQUE INDEX ux_users_single_admin ON dbo.users(role) WHERE role = 'ADMIN';

CREATE TABLE dbo.rooms (
 id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_rooms PRIMARY KEY,
 room_code NVARCHAR(30) NOT NULL,
 description NVARCHAR(2000) NULL,
 area_m2 DECIMAL(8,2) NULL,
 floor_number SMALLINT NOT NULL,
 cover_image_path NVARCHAR(500) NOT NULL,
 capacity SMALLINT NOT NULL,
 listed_monthly_rent DECIMAL(19,0) NOT NULL,
 status VARCHAR(16) NOT NULL,
 created_at DATETIME2(3) NOT NULL,
 updated_at DATETIME2(3) NOT NULL,
 CONSTRAINT uq_rooms_room_code UNIQUE (room_code),
 CONSTRAINT ck_rooms_values CHECK ((area_m2 IS NULL OR area_m2 > 0) AND floor_number > 0 AND capacity > 0 AND listed_monthly_rent > 0),
 CONSTRAINT ck_rooms_status CHECK (status IN ('AVAILABLE','RENTED','INACTIVE')),
 CONSTRAINT ck_rooms_required CHECK (LEN(LTRIM(RTRIM(room_code))) > 0 AND LEN(LTRIM(RTRIM(cover_image_path))) > 0)
);

CREATE TABLE dbo.billing_cycles (
 id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_billing_cycles PRIMARY KEY,
 collection_date DATE NOT NULL,
 status VARCHAR(20) NOT NULL,
 completed_at DATETIME2(3) NULL,
 created_at DATETIME2(3) NOT NULL,
 updated_at DATETIME2(3) NOT NULL,
 CONSTRAINT uq_billing_cycles_collection_date UNIQUE (collection_date),
 CONSTRAINT ck_billing_cycles_day CHECK (DAY(collection_date) = 10),
 CONSTRAINT ck_billing_cycles_status CHECK ((status = 'PENDING_INPUT' AND completed_at IS NULL) OR (status = 'COMPLETED' AND completed_at IS NOT NULL))
);

CREATE TABLE dbo.rental_applications (
 id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_rental_applications PRIMARY KEY,
 applicant_id BIGINT NOT NULL,
 room_id BIGINT NOT NULL,
 requested_start_date DATE NOT NULL,
 requested_end_date DATE NOT NULL,
 status VARCHAR(16) NOT NULL,
 processed_at DATETIME2(3) NULL,
 processed_by_user_id BIGINT NULL,
 processing_note NVARCHAR(1000) NULL,
 created_at DATETIME2(3) NOT NULL,
 updated_at DATETIME2(3) NOT NULL,
 CONSTRAINT ck_rental_applications_dates CHECK (requested_end_date > requested_start_date AND DAY(requested_end_date) = 10),
 CONSTRAINT ck_rental_applications_status CHECK ((status = 'PENDING' AND processed_at IS NULL AND processed_by_user_id IS NULL) OR (status IN ('APPROVED','REJECTED','WITHDRAWN') AND processed_at IS NOT NULL))
);
CREATE UNIQUE INDEX ux_rental_applications_pending_user_room ON dbo.rental_applications(applicant_id,room_id) WHERE status = 'PENDING';

CREATE TABLE dbo.contracts (
 id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_contracts PRIMARY KEY,
 application_id BIGINT NULL,
 room_id BIGINT NOT NULL,
 tenant_id BIGINT NOT NULL,
 agreed_monthly_rent DECIMAL(19,0) NOT NULL,
 start_date DATE NOT NULL,
 planned_end_date DATE NOT NULL,
 effective_end_date DATE NULL,
 status VARCHAR(16) NOT NULL,
 end_reason VARCHAR(32) NULL,
 activated_at DATETIME2(3) NULL,
 ended_at DATETIME2(3) NULL,
 cancelled_at DATETIME2(3) NULL,
 created_by_user_id BIGINT NOT NULL,
 closed_by_user_id BIGINT NULL,
 closing_note NVARCHAR(1000) NULL,
 final_billing_period_end_date DATE NULL,
 final_settlement_result VARCHAR(16) NULL,
 settled_at DATETIME2(3) NULL,
 created_at DATETIME2(3) NOT NULL,
 updated_at DATETIME2(3) NOT NULL,
 CONSTRAINT ck_contracts_dates CHECK (planned_end_date > start_date AND DAY(planned_end_date) = 10 AND (effective_end_date IS NULL OR (effective_end_date >= start_date AND effective_end_date <= planned_end_date))),
 CONSTRAINT ck_contracts_rent CHECK (agreed_monthly_rent > 0),
 CONSTRAINT ck_contracts_end_reason CHECK (end_reason IS NULL OR end_reason IN ('NATURAL_EXPIRY','TENANT_EARLY_EXIT','ADMIN_STOP_SERVICE')),
 CONSTRAINT ck_contracts_settlement_result CHECK (final_settlement_result IS NULL OR final_settlement_result IN ('INVOICED','NO_CHARGE')),
 CONSTRAINT ck_contracts_state CHECK (
   (status = 'SCHEDULED' AND activated_at IS NULL AND effective_end_date IS NULL AND end_reason IS NULL AND ended_at IS NULL AND cancelled_at IS NULL AND closed_by_user_id IS NULL)
   OR (status = 'ACTIVE' AND activated_at IS NOT NULL AND effective_end_date IS NULL AND end_reason IS NULL AND ended_at IS NULL AND cancelled_at IS NULL AND closed_by_user_id IS NULL)
   OR (status = 'ENDED' AND activated_at IS NOT NULL AND effective_end_date IS NOT NULL AND end_reason IS NOT NULL AND ended_at IS NOT NULL AND cancelled_at IS NULL
       AND ((end_reason = 'NATURAL_EXPIRY' AND effective_end_date = planned_end_date AND closed_by_user_id IS NULL)
         OR (end_reason IN ('TENANT_EARLY_EXIT','ADMIN_STOP_SERVICE') AND effective_end_date < planned_end_date AND closed_by_user_id IS NOT NULL)))
   OR (status = 'CANCELLED' AND activated_at IS NULL AND effective_end_date IS NULL AND end_reason IS NULL AND ended_at IS NULL AND cancelled_at IS NOT NULL AND closed_by_user_id IS NOT NULL)
 ),
 CONSTRAINT ck_contracts_settlement CHECK (
   (final_billing_period_end_date IS NULL AND final_settlement_result IS NULL AND settled_at IS NULL
     AND (status <> 'ENDED' OR end_reason = 'NATURAL_EXPIRY' OR (end_reason = 'TENANT_EARLY_EXIT' AND DAY(effective_end_date) = 10)))
   OR (status = 'ENDED' AND end_reason IN ('TENANT_EARLY_EXIT','ADMIN_STOP_SERVICE')
     AND final_billing_period_end_date IS NOT NULL AND DAY(final_billing_period_end_date) = 10
     AND final_settlement_result IS NOT NULL AND settled_at IS NOT NULL)
 )
);
CREATE UNIQUE INDEX ux_contracts_application ON dbo.contracts(application_id) WHERE application_id IS NOT NULL;
CREATE UNIQUE INDEX ux_contracts_occupying_room ON dbo.contracts(room_id) WHERE status IN ('SCHEDULED','ACTIVE');

CREATE TABLE dbo.contract_co_occupants (
 id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_contract_co_occupants PRIMARY KEY,
 contract_id BIGINT NOT NULL,
 full_name NVARCHAR(150) NOT NULL,
 identity_document_no NVARCHAR(30) NOT NULL,
 phone VARCHAR(20) NULL,
 created_at DATETIME2(3) NOT NULL,
 updated_at DATETIME2(3) NOT NULL,
 CONSTRAINT uq_contract_co_occupants_identity UNIQUE (contract_id,identity_document_no),
 CONSTRAINT ck_contract_co_occupants_required CHECK (LEN(LTRIM(RTRIM(full_name))) > 0 AND LEN(LTRIM(RTRIM(identity_document_no))) > 0)
);

CREATE TABLE dbo.billing_cycle_items (
 id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_billing_cycle_items PRIMARY KEY,
 billing_cycle_id BIGINT NOT NULL,
 contract_id BIGINT NOT NULL,
 usage_from DATE NOT NULL,
 usage_to DATE NOT NULL,
 electricity_amount DECIMAL(19,0) NULL,
 water_amount DECIMAL(19,0) NULL,
 input_completed_at DATETIME2(3) NULL,
 entered_by_user_id BIGINT NULL,
 rent_amount DECIMAL(19,0) NULL,
 processing_result VARCHAR(16) NOT NULL CONSTRAINT df_billing_cycle_items_result DEFAULT 'PENDING',
 processed_at DATETIME2(3) NULL,
 created_at DATETIME2(3) NOT NULL,
 updated_at DATETIME2(3) NOT NULL,
 CONSTRAINT uq_billing_cycle_items_cycle_contract UNIQUE (billing_cycle_id,contract_id),
 CONSTRAINT uq_billing_cycle_items_id_contract UNIQUE (id,contract_id),
 CONSTRAINT ck_billing_cycle_items_usage CHECK (usage_from <= usage_to),
 CONSTRAINT ck_billing_cycle_items_amounts CHECK ((electricity_amount IS NULL OR electricity_amount >= 0) AND (water_amount IS NULL OR water_amount >= 0) AND (rent_amount IS NULL OR rent_amount >= 0)),
 CONSTRAINT ck_billing_cycle_items_input CHECK (input_completed_at IS NULL OR (electricity_amount IS NOT NULL AND water_amount IS NOT NULL AND entered_by_user_id IS NOT NULL)),
 CONSTRAINT ck_billing_cycle_items_result CHECK (
   (processing_result = 'PENDING' AND rent_amount IS NULL AND processed_at IS NULL)
   OR (processing_result = 'INVOICED' AND rent_amount IS NOT NULL AND electricity_amount IS NOT NULL AND water_amount IS NOT NULL AND input_completed_at IS NOT NULL AND processed_at IS NOT NULL AND rent_amount + electricity_amount + water_amount > 0)
   OR (processing_result = 'NO_CHARGE' AND rent_amount IS NOT NULL AND electricity_amount IS NOT NULL AND water_amount IS NOT NULL AND rent_amount = 0 AND electricity_amount = 0 AND water_amount = 0 AND input_completed_at IS NOT NULL AND processed_at IS NOT NULL)
 )
);

CREATE TABLE dbo.invoices (
 id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_invoices PRIMARY KEY,
 contract_id BIGINT NOT NULL,
 billing_cycle_item_id BIGINT NULL,
 billing_period_end_date DATE NOT NULL,
 invoice_kind VARCHAR(24) NOT NULL,
 usage_from DATE NOT NULL,
 usage_to DATE NOT NULL,
 monthly_rent_snapshot DECIMAL(19,0) NOT NULL,
 rent_calculation_rule VARCHAR(32) NOT NULL,
 prorated_days INT NULL,
 rent_amount DECIMAL(19,0) NOT NULL,
 electricity_amount DECIMAL(19,0) NOT NULL,
 water_amount DECIMAL(19,0) NOT NULL,
 total_amount DECIMAL(19,0) NOT NULL,
 status VARCHAR(16) NOT NULL,
 issued_at DATETIME2(3) NOT NULL,
 due_at DATETIME2(3) NOT NULL,
 paid_at DATETIME2(3) NULL,
 payment_method VARCHAR(24) NULL,
 payment_reference NVARCHAR(128) NULL,
 confirmed_by_user_id BIGINT NULL,
 created_at DATETIME2(3) NOT NULL,
 updated_at DATETIME2(3) NOT NULL,
 CONSTRAINT uq_invoices_contract_period UNIQUE (contract_id,billing_period_end_date),
 CONSTRAINT ck_invoices_period CHECK (DAY(billing_period_end_date) = 10 AND usage_from <= usage_to AND usage_to <= billing_period_end_date),
 CONSTRAINT ck_invoices_money CHECK (monthly_rent_snapshot > 0 AND rent_amount >= 0 AND electricity_amount >= 0 AND water_amount >= 0 AND total_amount = rent_amount + electricity_amount + water_amount AND total_amount > 0),
 CONSTRAINT ck_invoices_kind_rule CHECK (
   (invoice_kind = 'REGULAR' AND billing_cycle_item_id IS NOT NULL AND ((rent_calculation_rule = 'FIRST_PRORATED' AND prorated_days IS NOT NULL AND prorated_days BETWEEN 1 AND 13) OR (rent_calculation_rule = 'MONTHLY_FULL' AND prorated_days IS NULL)))
   OR (invoice_kind = 'EARLY_SETTLEMENT' AND ((rent_calculation_rule = 'TENANT_EARLY_FULL' AND prorated_days IS NULL) OR (rent_calculation_rule = 'ADMIN_STOP_PRORATED' AND prorated_days IS NOT NULL AND prorated_days >= 0)))
 ),
 CONSTRAINT ck_invoices_due CHECK (due_at = DATEADD(HOUR,48,issued_at)),
 CONSTRAINT ck_invoices_payment CHECK (
   (status IN ('UNPAID','OVERDUE') AND paid_at IS NULL AND payment_method IS NULL AND payment_reference IS NULL AND confirmed_by_user_id IS NULL)
   OR (status = 'PAID' AND paid_at IS NOT NULL AND payment_method IS NOT NULL AND payment_method IN ('SANDBOX','CASH','BANK_TRANSFER')
       AND (payment_method = 'SANDBOX' OR confirmed_by_user_id IS NOT NULL))
 )
);
CREATE UNIQUE INDEX ux_invoices_cycle_item ON dbo.invoices(billing_cycle_item_id) WHERE billing_cycle_item_id IS NOT NULL;

CREATE TABLE dbo.issues (
 id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_issues PRIMARY KEY,
 contract_id BIGINT NOT NULL,
 reporter_id BIGINT NOT NULL,
 title NVARCHAR(200) NOT NULL,
 description NVARCHAR(4000) NOT NULL,
 status VARCHAR(16) NOT NULL,
 handled_by_user_id BIGINT NULL,
 admin_response NVARCHAR(4000) NULL,
 in_progress_at DATETIME2(3) NULL,
 resolved_at DATETIME2(3) NULL,
 created_at DATETIME2(3) NOT NULL,
 updated_at DATETIME2(3) NOT NULL,
 CONSTRAINT ck_issues_required CHECK (LEN(LTRIM(RTRIM(title))) > 0 AND LEN(LTRIM(RTRIM(description))) > 0),
 CONSTRAINT ck_issues_status CHECK (
   (status = 'OPEN' AND handled_by_user_id IS NULL AND in_progress_at IS NULL AND resolved_at IS NULL)
   OR (status = 'IN_PROGRESS' AND handled_by_user_id IS NOT NULL AND in_progress_at IS NOT NULL AND resolved_at IS NULL)
   OR (status = 'RESOLVED' AND handled_by_user_id IS NOT NULL AND in_progress_at IS NOT NULL AND resolved_at IS NOT NULL)
 )
);

CREATE TABLE dbo.notifications (
 id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_notifications PRIMARY KEY,
 recipient_user_id BIGINT NOT NULL,
 source_type VARCHAR(16) NOT NULL,
 sender_user_id BIGINT NULL,
 title NVARCHAR(200) NOT NULL,
 body NVARCHAR(4000) NOT NULL,
 status VARCHAR(8) NOT NULL,
 read_at DATETIME2(3) NULL,
 event_key VARCHAR(200) NULL,
 application_id BIGINT NULL,
 contract_id BIGINT NULL,
 invoice_id BIGINT NULL,
 issue_id BIGINT NULL,
 created_at DATETIME2(3) NOT NULL,
 updated_at DATETIME2(3) NOT NULL,
 CONSTRAINT ck_notifications_required CHECK (LEN(LTRIM(RTRIM(title))) > 0 AND LEN(LTRIM(RTRIM(body))) > 0 AND (event_key IS NULL OR LEN(LTRIM(RTRIM(event_key))) > 0)),
 CONSTRAINT ck_notifications_source CHECK ((source_type = 'MANUAL' AND sender_user_id IS NOT NULL) OR (source_type = 'SYSTEM' AND sender_user_id IS NULL AND event_key IS NOT NULL)),
 CONSTRAINT ck_notifications_status CHECK ((status = 'UNREAD' AND read_at IS NULL) OR (status = 'READ' AND read_at IS NOT NULL)),
 CONSTRAINT ck_notifications_reference CHECK (
  (CASE WHEN application_id IS NULL THEN 0 ELSE 1 END) +
  (CASE WHEN contract_id IS NULL THEN 0 ELSE 1 END) +
  (CASE WHEN invoice_id IS NULL THEN 0 ELSE 1 END) +
  (CASE WHEN issue_id IS NULL THEN 0 ELSE 1 END) <= 1
 )
);
CREATE UNIQUE INDEX ux_notifications_event_recipient ON dbo.notifications(event_key,recipient_user_id) WHERE event_key IS NOT NULL;

ALTER TABLE dbo.rental_applications ADD
 CONSTRAINT fk_rental_applications_applicant FOREIGN KEY (applicant_id) REFERENCES dbo.users(id),
 CONSTRAINT fk_rental_applications_room FOREIGN KEY (room_id) REFERENCES dbo.rooms(id),
 CONSTRAINT fk_rental_applications_processed_by FOREIGN KEY (processed_by_user_id) REFERENCES dbo.users(id);
ALTER TABLE dbo.contracts ADD
 CONSTRAINT fk_contracts_application FOREIGN KEY (application_id) REFERENCES dbo.rental_applications(id),
 CONSTRAINT fk_contracts_room FOREIGN KEY (room_id) REFERENCES dbo.rooms(id),
 CONSTRAINT fk_contracts_tenant FOREIGN KEY (tenant_id) REFERENCES dbo.users(id),
 CONSTRAINT fk_contracts_created_by FOREIGN KEY (created_by_user_id) REFERENCES dbo.users(id),
 CONSTRAINT fk_contracts_closed_by FOREIGN KEY (closed_by_user_id) REFERENCES dbo.users(id);
ALTER TABLE dbo.contract_co_occupants ADD CONSTRAINT fk_contract_co_occupants_contract FOREIGN KEY (contract_id) REFERENCES dbo.contracts(id);
ALTER TABLE dbo.billing_cycle_items ADD
 CONSTRAINT fk_billing_cycle_items_cycle FOREIGN KEY (billing_cycle_id) REFERENCES dbo.billing_cycles(id),
 CONSTRAINT fk_billing_cycle_items_contract FOREIGN KEY (contract_id) REFERENCES dbo.contracts(id),
 CONSTRAINT fk_billing_cycle_items_entered_by FOREIGN KEY (entered_by_user_id) REFERENCES dbo.users(id);
ALTER TABLE dbo.invoices ADD
 CONSTRAINT fk_invoices_contract FOREIGN KEY (contract_id) REFERENCES dbo.contracts(id),
 CONSTRAINT fk_invoices_cycle_item_contract FOREIGN KEY (billing_cycle_item_id,contract_id) REFERENCES dbo.billing_cycle_items(id,contract_id),
 CONSTRAINT fk_invoices_confirmed_by FOREIGN KEY (confirmed_by_user_id) REFERENCES dbo.users(id);
ALTER TABLE dbo.issues ADD
 CONSTRAINT fk_issues_contract FOREIGN KEY (contract_id) REFERENCES dbo.contracts(id),
 CONSTRAINT fk_issues_reporter FOREIGN KEY (reporter_id) REFERENCES dbo.users(id),
 CONSTRAINT fk_issues_handled_by FOREIGN KEY (handled_by_user_id) REFERENCES dbo.users(id);
ALTER TABLE dbo.notifications ADD
 CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_user_id) REFERENCES dbo.users(id),
 CONSTRAINT fk_notifications_sender FOREIGN KEY (sender_user_id) REFERENCES dbo.users(id),
 CONSTRAINT fk_notifications_application FOREIGN KEY (application_id) REFERENCES dbo.rental_applications(id),
 CONSTRAINT fk_notifications_contract FOREIGN KEY (contract_id) REFERENCES dbo.contracts(id),
 CONSTRAINT fk_notifications_invoice FOREIGN KEY (invoice_id) REFERENCES dbo.invoices(id),
 CONSTRAINT fk_notifications_issue FOREIGN KEY (issue_id) REFERENCES dbo.issues(id);

-- Read indexes from database_design.md; unique indexes above also serve lookup queries.
CREATE INDEX ix_contracts_tenant ON dbo.contracts(tenant_id);
CREATE INDEX ix_contracts_activation ON dbo.contracts(status,start_date);
CREATE INDEX ix_contracts_expiry ON dbo.contracts(status,planned_end_date);
CREATE INDEX ix_rental_applications_expiry ON dbo.rental_applications(status,requested_start_date);
CREATE INDEX ix_rental_applications_room_status ON dbo.rental_applications(room_id,status);
CREATE INDEX ix_invoices_due ON dbo.invoices(status,due_at);
CREATE INDEX ix_invoices_history ON dbo.invoices(contract_id,issued_at);
CREATE INDEX ix_issues_reporter ON dbo.issues(reporter_id,created_at);
CREATE INDEX ix_notifications_recipient_status ON dbo.notifications(recipient_user_id,status,created_at);
CREATE INDEX ix_billing_cycles_status ON dbo.billing_cycles(status,collection_date);

COMMIT TRANSACTION;

