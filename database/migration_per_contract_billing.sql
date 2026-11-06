-- One-time migration from building-wide day-10 billing to contract-anchored billing periods.
-- Run against an initialized RentalApartmentDev database before starting the updated app.
USE [RentalApartmentDev];
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.billing_cycles', N'U') IS NULL
       OR OBJECT_ID(N'dbo.billing_cycle_items', N'U') IS NULL
       OR COL_LENGTH(N'dbo.invoices', N'billing_cycle_item_id') IS NULL
        THROW 51020, 'Expected the previous building-wide billing schema.', 1;

    ALTER TABLE dbo.invoices DROP CONSTRAINT fk_invoices_cycle_item_contract;
    ALTER TABLE dbo.invoices DROP CONSTRAINT ck_invoices_period;
    ALTER TABLE dbo.invoices DROP CONSTRAINT ck_invoices_kind_rule;
    DROP INDEX ux_invoices_cycle_item ON dbo.invoices;

    ALTER TABLE dbo.contracts DROP CONSTRAINT ck_contracts_settlement;
    ALTER TABLE dbo.billing_cycle_items DROP CONSTRAINT fk_billing_cycle_items_cycle;
    ALTER TABLE dbo.billing_cycle_items DROP CONSTRAINT fk_billing_cycle_items_contract;
    ALTER TABLE dbo.billing_cycle_items DROP CONSTRAINT fk_billing_cycle_items_entered_by;
    ALTER TABLE dbo.billing_cycle_items DROP CONSTRAINT uq_billing_cycle_items_cycle_contract;
    ALTER TABLE dbo.billing_cycle_items DROP CONSTRAINT uq_billing_cycle_items_id_contract;
    ALTER TABLE dbo.billing_cycle_items DROP CONSTRAINT ck_billing_cycle_items_usage;
    ALTER TABLE dbo.billing_cycle_items DROP CONSTRAINT ck_billing_cycle_items_amounts;
    ALTER TABLE dbo.billing_cycle_items DROP CONSTRAINT ck_billing_cycle_items_input;
    ALTER TABLE dbo.billing_cycle_items DROP CONSTRAINT ck_billing_cycle_items_result;

    ALTER TABLE dbo.billing_cycle_items ADD billing_period_end_date DATE NULL;
    UPDATE item
       SET billing_period_end_date = cycle.collection_date
    FROM dbo.billing_cycle_items AS item
    JOIN dbo.billing_cycles AS cycle ON cycle.id = item.billing_cycle_id;
    ALTER TABLE dbo.billing_cycle_items ALTER COLUMN billing_period_end_date DATE NOT NULL;
    ALTER TABLE dbo.billing_cycle_items DROP COLUMN billing_cycle_id;

    DROP INDEX ix_billing_cycles_status ON dbo.billing_cycles;
    DROP TABLE dbo.billing_cycles;

    EXEC sys.sp_rename N'dbo.billing_cycle_items', N'billing_periods';
    EXEC sys.sp_rename N'dbo.invoices.billing_cycle_item_id', N'billing_period_id', N'COLUMN';

    ALTER TABLE dbo.billing_periods ADD
        CONSTRAINT uq_billing_periods_contract_end UNIQUE (contract_id,billing_period_end_date),
        CONSTRAINT uq_billing_periods_id_contract UNIQUE (id,contract_id),
        CONSTRAINT ck_billing_periods_usage CHECK (usage_from <= usage_to AND usage_to <= billing_period_end_date),
        CONSTRAINT ck_billing_periods_amounts CHECK ((electricity_amount IS NULL OR electricity_amount >= 0) AND (water_amount IS NULL OR water_amount >= 0) AND (rent_amount IS NULL OR rent_amount >= 0)),
        CONSTRAINT ck_billing_periods_input CHECK (input_completed_at IS NULL OR (electricity_amount IS NOT NULL AND water_amount IS NOT NULL AND entered_by_user_id IS NOT NULL)),
        CONSTRAINT ck_billing_periods_result CHECK (
            (processing_result = 'PENDING' AND rent_amount IS NULL AND processed_at IS NULL)
            OR (processing_result = 'INVOICED' AND rent_amount IS NOT NULL AND electricity_amount IS NOT NULL AND water_amount IS NOT NULL AND input_completed_at IS NOT NULL AND processed_at IS NOT NULL AND rent_amount + electricity_amount + water_amount > 0)
            OR (processing_result = 'NO_CHARGE' AND rent_amount IS NOT NULL AND electricity_amount IS NOT NULL AND water_amount IS NOT NULL AND rent_amount = 0 AND electricity_amount = 0 AND water_amount = 0 AND input_completed_at IS NOT NULL AND processed_at IS NOT NULL)
        );
    ALTER TABLE dbo.billing_periods ADD
        CONSTRAINT fk_billing_periods_contract FOREIGN KEY (contract_id) REFERENCES dbo.contracts(id),
        CONSTRAINT fk_billing_periods_entered_by FOREIGN KEY (entered_by_user_id) REFERENCES dbo.users(id);

    ALTER TABLE dbo.invoices ADD
        CONSTRAINT ck_invoices_period CHECK (usage_from <= usage_to AND usage_to <= billing_period_end_date),
        CONSTRAINT ck_invoices_kind_rule CHECK (
            (invoice_kind = 'REGULAR' AND billing_period_id IS NOT NULL AND ((rent_calculation_rule IN ('FIRST_PRORATED','PERIOD_PRORATED') AND prorated_days IS NOT NULL AND prorated_days BETWEEN 1 AND 13) OR (rent_calculation_rule = 'MONTHLY_FULL' AND prorated_days IS NULL)))
            OR (invoice_kind = 'EARLY_SETTLEMENT' AND ((rent_calculation_rule = 'TENANT_EARLY_FULL' AND prorated_days IS NULL) OR (rent_calculation_rule = 'ADMIN_STOP_PRORATED' AND prorated_days IS NOT NULL AND prorated_days >= 0)))
        ),
        CONSTRAINT fk_invoices_billing_period_contract FOREIGN KEY (billing_period_id,contract_id) REFERENCES dbo.billing_periods(id,contract_id);
    CREATE UNIQUE INDEX ux_invoices_billing_period ON dbo.invoices(billing_period_id) WHERE billing_period_id IS NOT NULL;

    ALTER TABLE dbo.contracts ADD CONSTRAINT ck_contracts_settlement CHECK (
        (final_billing_period_end_date IS NULL AND final_settlement_result IS NULL AND settled_at IS NULL
         AND (status <> 'ENDED' OR end_reason IN ('NATURAL_EXPIRY','TENANT_EARLY_EXIT')))
        OR (status = 'ENDED' AND end_reason IN ('TENANT_EARLY_EXIT','ADMIN_STOP_SERVICE')
         AND final_billing_period_end_date IS NOT NULL AND final_settlement_result IS NOT NULL AND settled_at IS NOT NULL)
    );

    CREATE INDEX ix_billing_periods_contract_end ON dbo.billing_periods(contract_id,billing_period_end_date);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
