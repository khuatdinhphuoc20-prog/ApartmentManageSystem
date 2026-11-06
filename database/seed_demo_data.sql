-- Development-only sample dataset. Run once after schema.sql and seed_admin.sql.
-- It refuses to run against a database that already contains business data.
-- The seeded USER accounts all use the development password documented in database/README.md.

USE [RentalApartmentDev];

SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;

DECLARE @admin_id BIGINT = (SELECT id FROM dbo.users WHERE email = N'admin@rental.local' AND role = 'ADMIN');

IF @admin_id IS NULL OR @admin_id <> 1 OR (SELECT COUNT(*) FROM dbo.users) <> 1
    THROW 51010, 'Expected the fresh database with only the seeded Admin at users.id = 1.', 1;

IF EXISTS (SELECT 1 FROM dbo.rooms)
   OR EXISTS (SELECT 1 FROM dbo.rental_applications)
   OR EXISTS (SELECT 1 FROM dbo.contracts)
   OR EXISTS (SELECT 1 FROM dbo.contract_co_occupants)
   OR EXISTS (SELECT 1 FROM dbo.billing_cycles)
   OR EXISTS (SELECT 1 FROM dbo.billing_cycle_items)
   OR EXISTS (SELECT 1 FROM dbo.invoices)
   OR EXISTS (SELECT 1 FROM dbo.issues)
   OR EXISTS (SELECT 1 FROM dbo.notifications)
    THROW 51011, 'Sample data already exists. This script is additive and must not be rerun.', 1;

DECLARE @now DATETIME2(3) = SYSUTCDATETIME();
DECLARE @tenant_password_hash VARCHAR(255) = '$2a$10$Mxpa9G5LYl8KO6kRC6tjXugY.0COplU3W7TbprJAZBIpvTAFOAknC';
DECLARE @issued_overdue DATETIME2(3) = DATEADD(HOUR, -72, @now);
DECLARE @due_overdue DATETIME2(3) = DATEADD(HOUR, 48, @issued_overdue);
DECLARE @issued_unpaid DATETIME2(3) = DATEADD(HOUR, -24, @now);
DECLARE @due_unpaid DATETIME2(3) = DATEADD(HOUR, 48, @issued_unpaid);

BEGIN TRY
    BEGIN TRANSACTION;

    SET IDENTITY_INSERT dbo.users ON;
    INSERT INTO dbo.users (
        id, email, password_hash, full_name, phone, identity_document_no, role, created_at, updated_at
    ) VALUES
        (2, N'tenant01@rental.local', @tenant_password_hash, N'Nguyễn An', '0910000001', N'100000000001', 'USER', @now, @now),
        (3, N'tenant02@rental.local', @tenant_password_hash, N'Trần Bình', '0910000002', N'100000000002', 'USER', @now, @now),
        (4, N'tenant03@rental.local', @tenant_password_hash, N'Lê Chi', '0910000003', N'100000000003', 'USER', @now, @now),
        (5, N'tenant04@rental.local', @tenant_password_hash, N'Phạm Dũng', '0910000004', N'100000000004', 'USER', @now, @now),
        (6, N'tenant05@rental.local', @tenant_password_hash, N'Võ Hà', '0910000005', N'100000000005', 'USER', @now, @now),
        (7, N'tenant06@rental.local', @tenant_password_hash, N'Đặng Khoa', '0910000006', N'100000000006', 'USER', @now, @now),
        (8, N'tenant07@rental.local', @tenant_password_hash, N'Bùi Lan', '0910000007', N'100000000007', 'USER', @now, @now),
        (9, N'tenant08@rental.local', @tenant_password_hash, N'Đỗ Minh', '0910000008', N'100000000008', 'USER', @now, @now),
        (10, N'tenant09@rental.local', @tenant_password_hash, N'Hoàng Ngọc', '0910000009', N'100000000009', 'USER', @now, @now);
    SET IDENTITY_INSERT dbo.users OFF;

    SET IDENTITY_INSERT dbo.rooms ON;
    INSERT INTO dbo.rooms (
        id, room_code, description, area_m2, floor_number, cover_image_path, capacity,
        listed_monthly_rent, status, created_at, updated_at
    ) VALUES
        (1, N'P101', N'Phòng đang thuê, hợp đồng ACTIVE', 25.00, 1, N'/images/rooms/p101.jpg', 3, 3000000, 'RENTED', @now, @now),
        (2, N'P102', N'Phòng có hợp đồng bắt đầu trong tương lai', 32.00, 1, N'/images/rooms/p102.jpg', 3, 3500000, 'RENTED', @now, @now),
        (3, N'P201', N'Phòng vừa hết hợp đồng tự nhiên', 28.00, 2, N'/images/rooms/p201.jpg', 2, 3200000, 'AVAILABLE', @now, @now),
        (4, N'P202', N'Phòng có lịch sử Tenant trả sớm', 29.50, 2, N'/images/rooms/p202.jpg', 2, 3500000, 'AVAILABLE', @now, @now),
        (5, N'P301', N'Phòng có lịch sử Admin ngừng cung cấp', 24.00, 3, N'/images/rooms/p301.jpg', 2, 2500000, 'AVAILABLE', @now, @now),
        (6, N'P302', N'Phòng có lịch sử trả đúng biên ngày 10', 30.00, 3, N'/images/rooms/p302.jpg', 3, 4000000, 'AVAILABLE', @now, @now),
        (7, N'P401', N'Phòng có hợp đồng đã hủy trước ngày bắt đầu', 26.00, 4, N'/images/rooms/p401.jpg', 2, 2800000, 'AVAILABLE', @now, @now),
        (8, N'P402', N'Phòng ngừng cho thuê', 27.00, 4, N'/images/rooms/p402.jpg', 2, 3000000, 'INACTIVE', @now, @now),
        (9, N'P501', N'Edge case: prorate một ngày làm tròn thành 0 đồng', 22.00, 5, N'/images/rooms/p501.jpg', 2, 10, 'RENTED', @now, @now),
        (10, N'P502', N'Phòng có lịch sử hợp đồng kết thúc', 35.00, 5, N'/images/rooms/p502.jpg', 4, 4500000, 'AVAILABLE', @now, @now);
    SET IDENTITY_INSERT dbo.rooms OFF;

    SET IDENTITY_INSERT dbo.rental_applications ON;
    INSERT INTO dbo.rental_applications (
        id, applicant_id, room_id, requested_start_date, requested_end_date, status,
        processed_at, processed_by_user_id, processing_note, created_at, updated_at
    ) VALUES
        (1, 2, 1, '2026-04-10', '2027-04-10', 'APPROVED', '2026-04-01T10:00:00', 1, N'Đã duyệt và tạo hợp đồng.', @now, @now),
        (2, 3, 2, '2026-11-10', '2027-11-10', 'APPROVED', '2026-10-20T10:00:00', 1, N'Đã duyệt, chờ ngày bắt đầu.', @now, @now),
        (3, 4, 3, '2025-10-10', '2026-10-10', 'APPROVED', '2025-10-01T10:00:00', 1, N'Đã duyệt.', @now, @now),
        (4, 5, 4, '2026-03-10', '2027-03-10', 'REJECTED', '2026-03-01T10:00:00', 1, N'Phòng đã được dành cho khách khác.', @now, @now),
        (5, 6, 5, '2026-02-10', '2027-02-10', 'WITHDRAWN', '2026-02-05T10:00:00', 6, N'Người nộp đã rút đơn.', @now, @now),
        (6, 7, 6, '2026-04-10', '2027-04-10', 'APPROVED', '2026-04-01T11:00:00', 1, N'Đã duyệt.', @now, @now),
        (7, 8, 7, '2026-11-10', '2027-11-10', 'REJECTED', '2026-10-20T11:00:00', 1, N'Đơn cạnh tranh bị từ chối.', @now, @now),
        (8, 9, 8, '2025-10-10', '2026-10-10', 'APPROVED', '2025-10-01T11:00:00', 1, N'Đã duyệt.', @now, @now),
        (9, 10, 9, '2026-03-09', '2027-07-10', 'APPROVED', '2026-03-01T11:00:00', 1, N'Đã duyệt.', @now, @now),
        (10, 2, 10, '2026-02-10', '2027-02-10', 'PENDING', NULL, NULL, NULL, @now, @now);
    SET IDENTITY_INSERT dbo.rental_applications OFF;

    SET IDENTITY_INSERT dbo.contracts ON;
    INSERT INTO dbo.contracts (
        id, application_id, room_id, tenant_id, agreed_monthly_rent, start_date, planned_end_date,
        effective_end_date, status, end_reason, activated_at, ended_at, cancelled_at,
        created_by_user_id, closed_by_user_id, closing_note, final_billing_period_end_date,
        final_settlement_result, settled_at, created_at, updated_at
    ) VALUES
        (1, 1, 1, 2, 3000000, '2026-04-10', '2027-04-10', NULL, 'ACTIVE', NULL, '2026-04-10T00:00:00', NULL, NULL, 1, NULL, NULL, NULL, NULL, NULL, @now, @now),
        (2, 2, 2, 3, 3500000, '2026-11-10', '2027-11-10', NULL, 'SCHEDULED', NULL, NULL, NULL, NULL, 1, NULL, NULL, NULL, NULL, NULL, @now, @now),
        (3, 3, 3, 4, 3200000, '2025-10-10', '2026-10-10', '2026-10-10', 'ENDED', 'NATURAL_EXPIRY', '2025-10-10T00:00:00', '2026-10-10T00:00:00', NULL, 1, NULL, NULL, NULL, NULL, NULL, @now, @now),
        (4, NULL, 4, 5, 3500000, '2026-03-10', '2027-03-10', '2026-10-04', 'ENDED', 'TENANT_EARLY_EXIT', '2026-03-10T00:00:00', '2026-10-04T10:00:00', NULL, 1, 1, N'Tenant trả phòng sớm.', '2026-10-10', 'INVOICED', '2026-10-04T10:00:00', @now, @now),
        (5, NULL, 5, 6, 2500000, '2026-02-10', '2027-02-10', '2026-10-04', 'ENDED', 'ADMIN_STOP_SERVICE', '2026-02-10T00:00:00', '2026-10-04T11:00:00', NULL, 1, 1, N'Admin ngừng cung cấp dịch vụ.', '2026-10-10', 'INVOICED', '2026-10-04T11:00:00', @now, @now),
        (6, 6, 6, 7, 4000000, '2026-04-10', '2027-04-10', '2026-10-10', 'ENDED', 'TENANT_EARLY_EXIT', '2026-04-10T00:00:00', '2026-10-10T10:00:00', NULL, 1, 1, N'Tenant trả đúng biên kỳ ngày 10.', NULL, NULL, NULL, @now, @now),
        (7, NULL, 7, 8, 2800000, '2026-11-10', '2027-11-10', NULL, 'CANCELLED', NULL, NULL, NULL, '2026-10-20T10:00:00', 1, 1, N'Hủy trước ngày bắt đầu.', NULL, NULL, NULL, @now, @now),
        (8, 8, 8, 9, 3000000, '2025-10-10', '2026-10-10', '2026-10-10', 'ENDED', 'NATURAL_EXPIRY', '2025-10-10T00:00:00', '2026-10-10T00:00:00', NULL, 1, NULL, NULL, NULL, NULL, NULL, @now, @now),
        (9, 9, 9, 10, 10, '2026-03-09', '2027-07-10', NULL, 'ACTIVE', NULL, '2026-03-09T00:00:00', NULL, NULL, 1, NULL, NULL, NULL, NULL, NULL, @now, @now),
        (10, NULL, 10, 2, 4500000, '2025-02-10', '2026-10-10', '2026-10-10', 'ENDED', 'NATURAL_EXPIRY', '2025-02-10T00:00:00', '2026-10-10T00:00:00', NULL, 1, NULL, NULL, NULL, NULL, NULL, @now, @now);
    SET IDENTITY_INSERT dbo.contracts OFF;

    SET IDENTITY_INSERT dbo.contract_co_occupants ON;
    INSERT INTO dbo.contract_co_occupants (
        id, contract_id, full_name, identity_document_no, phone, created_at, updated_at
    ) VALUES
        (1, 1, N'Nguyễn Người Ở Cùng 01', N'CO0000000001', '0920000001', @now, @now),
        (2, 1, N'Nguyễn Người Ở Cùng 02', N'CO0000000002', NULL, @now, @now),
        (3, 2, N'Trần Người Ở Cùng 03', N'CO0000000003', '0920000003', @now, @now),
        (4, 2, N'Trần Người Ở Cùng 04', N'CO0000000004', NULL, @now, @now),
        (5, 3, N'Lê Người Ở Cùng 05', N'CO0000000005', '0920000005', @now, @now),
        (6, 4, N'Phạm Người Ở Cùng 06', N'CO0000000006', NULL, @now, @now),
        (7, 5, N'Võ Người Ở Cùng 07', N'CO0000000007', '0920000007', @now, @now),
        (8, 6, N'Đặng Người Ở Cùng 08', N'CO0000000008', NULL, @now, @now),
        (9, 9, N'Hoàng Người Ở Cùng 09', N'CO0000000009', '0920000009', @now, @now),
        (10, 10, N'Nguyễn Người Ở Cùng 10', N'CO0000000010', NULL, @now, @now);
    SET IDENTITY_INSERT dbo.contract_co_occupants OFF;

    SET IDENTITY_INSERT dbo.billing_cycles ON;
    INSERT INTO dbo.billing_cycles (id, collection_date, status, completed_at, created_at, updated_at) VALUES
        (1, '2026-01-10', 'COMPLETED', '2026-01-10T15:00:00', @now, @now),
        (2, '2026-02-10', 'COMPLETED', '2026-02-10T15:00:00', @now, @now),
        (3, '2026-03-10', 'COMPLETED', '2026-03-10T15:00:00', @now, @now),
        (4, '2026-04-10', 'COMPLETED', '2026-04-10T15:00:00', @now, @now),
        (5, '2026-05-10', 'COMPLETED', '2026-05-10T15:00:00', @now, @now),
        (6, '2026-06-10', 'COMPLETED', '2026-06-10T15:00:00', @now, @now),
        (7, '2026-07-10', 'COMPLETED', '2026-07-10T15:00:00', @now, @now),
        (8, '2026-08-10', 'COMPLETED', '2026-08-10T15:00:00', @now, @now),
        (9, '2026-09-10', 'COMPLETED', '2026-09-10T15:00:00', @now, @now),
        (10, '2026-10-10', 'PENDING_INPUT', NULL, @now, @now);
    SET IDENTITY_INSERT dbo.billing_cycles OFF;

    SET IDENTITY_INSERT dbo.billing_cycle_items ON;
    INSERT INTO dbo.billing_cycle_items (
        id, billing_cycle_id, contract_id, usage_from, usage_to, electricity_amount, water_amount,
        input_completed_at, entered_by_user_id, rent_amount, processing_result, processed_at,
        created_at, updated_at
    ) VALUES
        (1, 1, 3, '2025-12-10', '2026-01-10', 100000, 50000, '2026-01-10T09:00:00', 1, 3200000, 'INVOICED', '2026-01-10T10:00:00', @now, @now),
        (2, 2, 3, '2026-01-10', '2026-02-10', 110000, 55000, '2026-02-10T09:00:00', 1, 3200000, 'INVOICED', '2026-02-10T10:00:00', @now, @now),
        (3, 3, 9, '2026-03-09', '2026-03-10', 0, 0, '2026-03-10T09:00:00', 1, 0, 'NO_CHARGE', '2026-03-10T10:00:00', @now, @now),
        (4, 4, 3, '2026-03-10', '2026-04-10', 120000, 60000, '2026-04-10T09:00:00', 1, 3200000, 'INVOICED', '2026-04-10T10:00:00', @now, @now),
        (5, 5, 3, '2026-04-10', '2026-05-10', 130000, 65000, '2026-05-10T09:00:00', 1, 3200000, 'INVOICED', '2026-05-10T10:00:00', @now, @now),
        (6, 6, 3, '2026-05-10', '2026-06-10', 140000, 70000, '2026-06-10T09:00:00', 1, 3200000, 'INVOICED', '2026-06-10T10:00:00', @now, @now),
        (7, 7, 3, '2026-06-10', '2026-07-10', 150000, 75000, '2026-07-10T09:00:00', 1, 3200000, 'INVOICED', '2026-07-10T10:00:00', @now, @now),
        (8, 8, 3, '2026-07-10', '2026-08-10', 160000, 80000, '2026-08-10T09:00:00', 1, 3200000, 'INVOICED', '2026-08-10T10:00:00', @now, @now),
        (9, 9, 3, '2026-08-10', '2026-09-10', 170000, 85000, '2026-09-10T09:00:00', 1, 3200000, 'INVOICED', '2026-09-10T10:00:00', @now, @now),
        (10, 10, 3, '2026-09-10', '2026-10-10', NULL, NULL, NULL, NULL, NULL, 'PENDING', NULL, @now, @now);
    SET IDENTITY_INSERT dbo.billing_cycle_items OFF;

    SET IDENTITY_INSERT dbo.invoices ON;
    INSERT INTO dbo.invoices (
        id, contract_id, billing_cycle_item_id, billing_period_end_date, invoice_kind, usage_from, usage_to,
        monthly_rent_snapshot, rent_calculation_rule, prorated_days, rent_amount, electricity_amount,
        water_amount, total_amount, status, issued_at, due_at, paid_at, payment_method,
        payment_reference, confirmed_by_user_id, created_at, updated_at
    ) VALUES
        (1, 3, 1, '2026-01-10', 'REGULAR', '2025-12-10', '2026-01-10', 3200000, 'MONTHLY_FULL', NULL, 3200000, 100000, 50000, 3350000, 'PAID', '2026-01-10T10:00:00', '2026-01-12T10:00:00', '2026-01-11T10:00:00', 'BANK_TRANSFER', N'TXN-DEMO-0001', NULL, @now, @now),
        (2, 3, 2, '2026-02-10', 'REGULAR', '2026-01-10', '2026-02-10', 3200000, 'MONTHLY_FULL', NULL, 3200000, 110000, 55000, 3365000, 'PAID', '2026-02-10T10:00:00', '2026-02-12T10:00:00', '2026-02-11T10:00:00', 'CASH', N'RECEIPT-DEMO-0002', 1, @now, @now),
        (3, 3, 4, '2026-04-10', 'REGULAR', '2026-03-10', '2026-04-10', 3200000, 'MONTHLY_FULL', NULL, 3200000, 120000, 60000, 3380000, 'PAID', '2026-04-10T10:00:00', '2026-04-12T10:00:00', '2026-04-11T10:00:00', 'BANK_TRANSFER', N'BANK-DEMO-0003', 1, @now, @now),
        (4, 3, 5, '2026-05-10', 'REGULAR', '2026-04-10', '2026-05-10', 3200000, 'MONTHLY_FULL', NULL, 3200000, 130000, 65000, 3395000, 'PAID', '2026-05-10T10:00:00', '2026-05-12T10:00:00', '2026-05-11T10:00:00', 'BANK_TRANSFER', N'TXN-DEMO-0004', NULL, @now, @now),
        (5, 3, 6, '2026-06-10', 'REGULAR', '2026-05-10', '2026-06-10', 3200000, 'MONTHLY_FULL', NULL, 3200000, 140000, 70000, 3410000, 'PAID', '2026-06-10T10:00:00', '2026-06-12T10:00:00', '2026-06-11T10:00:00', 'CASH', N'RECEIPT-DEMO-0005', 1, @now, @now),
        (6, 3, 7, '2026-07-10', 'REGULAR', '2026-06-10', '2026-07-10', 3200000, 'MONTHLY_FULL', NULL, 3200000, 150000, 75000, 3425000, 'PAID', '2026-07-10T10:00:00', '2026-07-12T10:00:00', '2026-07-11T10:00:00', 'BANK_TRANSFER', N'BANK-DEMO-0006', 1, @now, @now),
        (7, 3, 8, '2026-08-10', 'REGULAR', '2026-07-10', '2026-08-10', 3200000, 'MONTHLY_FULL', NULL, 3200000, 160000, 80000, 3440000, 'OVERDUE', @issued_overdue, @due_overdue, NULL, NULL, NULL, NULL, @now, @now),
        (8, 3, 9, '2026-09-10', 'REGULAR', '2026-08-10', '2026-09-10', 3200000, 'MONTHLY_FULL', NULL, 3200000, 170000, 85000, 3455000, 'UNPAID', @issued_unpaid, @due_unpaid, NULL, NULL, NULL, NULL, @now, @now),
        (9, 4, NULL, '2026-10-10', 'EARLY_SETTLEMENT', '2026-09-10', '2026-10-04', 3500000, 'TENANT_EARLY_FULL', NULL, 3500000, 120000, 50000, 3670000, 'PAID', '2026-10-04T10:00:00', '2026-10-06T10:00:00', '2026-10-05T10:00:00', 'BANK_TRANSFER', N'TXN-DEMO-LATE-0009', 1, @now, @now),
        (10, 5, NULL, '2026-10-10', 'EARLY_SETTLEMENT', '2026-09-10', '2026-10-04', 2500000, 'ADMIN_STOP_PRORATED', 24, 2000000, 50000, 20000, 2070000, 'PAID', '2026-10-04T11:00:00', '2026-10-06T11:00:00', '2026-10-05T11:00:00', 'BANK_TRANSFER', N'BANK-DEMO-0010', 1, @now, @now);
    SET IDENTITY_INSERT dbo.invoices OFF;

    SET IDENTITY_INSERT dbo.issues ON;
    INSERT INTO dbo.issues (
        id, contract_id, reporter_id, title, description, status, handled_by_user_id,
        admin_response, in_progress_at, resolved_at, created_at, updated_at
    ) VALUES
        (1, 1, 2, N'Vòi nước bị rò', N'Nước rỉ dưới bồn rửa.', 'OPEN', NULL, NULL, NULL, NULL, @now, @now),
        (2, 1, 2, N'Đèn hành lang', N'Bóng đèn gần phòng cần thay.', 'IN_PROGRESS', 1, N'Admin đã tiếp nhận.', @now, NULL, @now, @now),
        (3, 3, 4, N'Khóa cửa', N'Khóa cửa cần được điều chỉnh.', 'RESOLVED', 1, N'Đã sửa xong.', '2026-09-01T10:00:00', '2026-09-02T10:00:00', @now, @now),
        (4, 4, 5, N'Kiểm tra ổ cắm', N'Ổ cắm phát tiếng rè.', 'RESOLVED', 1, N'Đã thay ổ cắm.', '2026-08-01T10:00:00', '2026-08-02T10:00:00', @now, @now),
        (5, 5, 6, N'Vệ sinh máy lạnh', N'Máy lạnh cần vệ sinh định kỳ.', 'OPEN', NULL, NULL, NULL, NULL, @now, @now),
        (6, 6, 7, N'Rèm cửa', N'Rèm bị tuột móc.', 'RESOLVED', 1, N'Đã gắn lại móc rèm.', '2026-09-20T10:00:00', '2026-09-21T10:00:00', @now, @now),
        (7, 8, 9, N'Vết thấm tường', N'Ghi nhận vết thấm gần cửa sổ.', 'RESOLVED', 1, N'Đã xử lý trước khi bàn giao.', '2026-09-25T10:00:00', '2026-09-27T10:00:00', @now, @now),
        (8, 9, 10, N'Wifi tầng 5', N'Tín hiệu wifi yếu vào buổi tối.', 'IN_PROGRESS', 1, N'Đang kiểm tra bộ phát.', '2026-10-20T10:00:00', NULL, @now, @now),
        (9, 10, 2, N'Bàn giao phòng', N'Ghi nhận hạng mục cần xử lý sau khi kết thúc hợp đồng.', 'RESOLVED', 1, N'Đã hoàn tất.', '2026-09-15T10:00:00', '2026-09-16T10:00:00', @now, @now),
        (10, 1, 2, N'Kiểm tra cửa sổ', N'Tay nắm cửa sổ hơi lỏng.', 'OPEN', NULL, NULL, NULL, NULL, @now, @now);
    SET IDENTITY_INSERT dbo.issues OFF;

    SET IDENTITY_INSERT dbo.notifications ON;
    INSERT INTO dbo.notifications (
        id, recipient_user_id, source_type, sender_user_id, title, body, status, read_at,
        event_key, application_id, contract_id, invoice_id, issue_id, created_at, updated_at
    ) VALUES
        (1, 4, 'SYSTEM', NULL, N'Hóa đơn đã phát hành', N'Invoice #1 đã được phát hành.', 'UNREAD', NULL, 'INVOICE_ISSUED:1', NULL, NULL, 1, NULL, @now, @now),
        (2, 4, 'SYSTEM', NULL, N'Đã nhận thanh toán', N'Invoice #1 đã được ghi nhận thanh toán.', 'READ', @now, 'INVOICE_PAID:1', NULL, NULL, 1, NULL, @now, @now),
        (3, 2, 'MANUAL', 1, N'Nhắc bổ sung thông tin', N'Vui lòng cập nhật thông tin liên hệ.', 'UNREAD', NULL, NULL, NULL, 1, NULL, NULL, @now, @now),
        (4, 5, 'SYSTEM', NULL, N'Đối chiếu giao dịch', N'Giao dịch đã được Admin đối chiếu.', 'READ', @now, 'PAYMENT_RECONCILED:9', NULL, NULL, 9, NULL, @now, @now),
        (5, 6, 'MANUAL', 1, N'Thông tin kết thúc thuê', N'Admin đã gửi thông tin về quyết toán kỳ cuối.', 'UNREAD', NULL, NULL, NULL, 5, NULL, NULL, @now, @now),
        (6, 7, 'SYSTEM', NULL, N'Hợp đồng đã kết thúc', N'Contract #6 đã kết thúc tại ngày 10.', 'READ', @now, 'CONTRACT_ENDED:6', NULL, 6, NULL, NULL, @now, @now),
        (7, 2, 'MANUAL', 1, N'Phản hồi yêu cầu sửa chữa', N'Admin đã cập nhật yêu cầu sửa chữa của bạn.', 'UNREAD', NULL, NULL, NULL, NULL, NULL, 1, @now, @now),
        (8, 4, 'SYSTEM', NULL, N'Hóa đơn quá hạn', N'Invoice #7 đã quá hạn thanh toán.', 'UNREAD', NULL, 'INVOICE_OVERDUE:7', NULL, NULL, 7, NULL, @now, @now),
        (9, 10, 'MANUAL', 1, N'Cập nhật yêu cầu sửa chữa', N'Yêu cầu của bạn đang được xử lý.', 'READ', @now, NULL, NULL, NULL, NULL, 8, @now, @now),
        (10, 2, 'SYSTEM', NULL, N'Đã nhận đơn thuê', N'Đơn thuê #10 đang chờ Admin xử lý.', 'UNREAD', NULL, 'APPLICATION_RECEIVED:10', 10, NULL, NULL, NULL, @now, @now);
    SET IDENTITY_INSERT dbo.notifications OFF;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
