-- One-time development password reset for admin@rental.local.
-- The plaintext password is shared with the user outside this file.
USE [RentalApartmentDev];
GO

SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @admin_password_hash VARCHAR(255) = '$2a$12$IOjc6CFP90FT0AhEdoZFv.cGLtvy110vJW5TAhRMQnA2y0UlejXua';

UPDATE dbo.users
   SET password_hash = @admin_password_hash,
       updated_at = SYSUTCDATETIME()
 WHERE email = N'admin@rental.local'
   AND role = 'ADMIN';

IF @@ROWCOUNT <> 1
BEGIN
    ROLLBACK TRANSACTION;
    THROW 51010, 'Expected exactly one admin@rental.local ADMIN account.', 1;
END;

COMMIT TRANSACTION;
