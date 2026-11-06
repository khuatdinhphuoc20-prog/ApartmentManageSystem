-- Keep existing SANDBOX invoice rows unchanged, while allowing new verified
-- VNPAY payments to be stored as BANK_TRANSFER with no human confirmer.
-- CASH and Admin-confirmed BANK_TRANSFER still require an Admin user.
IF OBJECT_ID(N'dbo.ck_invoices_payment', N'C') IS NOT NULL
    ALTER TABLE dbo.invoices DROP CONSTRAINT ck_invoices_payment;
GO

ALTER TABLE dbo.invoices ADD CONSTRAINT ck_invoices_payment CHECK (
    (status IN ('UNPAID','OVERDUE')
        AND paid_at IS NULL
        AND payment_method IS NULL
        AND payment_reference IS NULL
        AND confirmed_by_user_id IS NULL)
    OR
    (status = 'PAID'
        AND paid_at IS NOT NULL
        AND payment_method IS NOT NULL
        AND payment_method IN ('SANDBOX','CASH','BANK_TRANSFER')
        AND (
            payment_method = 'SANDBOX' -- historical value; application no longer writes it
            OR confirmed_by_user_id IS NOT NULL
            OR (payment_method = 'BANK_TRANSFER' AND payment_reference IS NOT NULL) -- verified VNPAY
        ))
);
GO

-- Existing SANDBOX invoices are left as-is. Block only new inserts or changes
-- that try to write SANDBOX after this migration.
CREATE OR ALTER TRIGGER dbo.trg_invoices_block_new_sandbox_method
ON dbo.invoices
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted AS i
        LEFT JOIN deleted AS d ON d.id = i.id
        WHERE i.payment_method = 'SANDBOX'
          AND (d.id IS NULL OR d.payment_method IS NULL OR d.payment_method <> 'SANDBOX')
    )
    BEGIN
        THROW 51000, 'New invoices may use only CASH or BANK_TRANSFER payment methods.', 1;
    END;
END;
GO
