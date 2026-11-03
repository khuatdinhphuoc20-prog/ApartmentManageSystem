-- Repairs Vietnamese names in the local demo accounts if sqlcmd previously
-- read seed_demo_data.sql using a non-UTF-8 input code page.
USE [RentalApartmentDev];
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
SET NOCOUNT ON;

DECLARE @names TABLE (
    id BIGINT NOT NULL PRIMARY KEY,
    email NVARCHAR(254) NOT NULL,
    full_name NVARCHAR(150) NOT NULL
);

INSERT INTO @names (id, email, full_name) VALUES
    (2, 'tenant01@rental.local', NCHAR(78)+NCHAR(103)+NCHAR(117)+NCHAR(121)+NCHAR(7877)+NCHAR(110)+NCHAR(32)+NCHAR(65)+NCHAR(110)),
    (3, 'tenant02@rental.local', NCHAR(84)+NCHAR(114)+NCHAR(7847)+NCHAR(110)+NCHAR(32)+NCHAR(66)+NCHAR(236)+NCHAR(110)+NCHAR(104)),
    (4, 'tenant03@rental.local', NCHAR(76)+NCHAR(234)+NCHAR(32)+NCHAR(67)+NCHAR(104)+NCHAR(105)),
    (5, 'tenant04@rental.local', NCHAR(80)+NCHAR(104)+NCHAR(7841)+NCHAR(109)+NCHAR(32)+NCHAR(68)+NCHAR(361)+NCHAR(110)+NCHAR(103)),
    (6, 'tenant05@rental.local', NCHAR(86)+NCHAR(245)+NCHAR(32)+NCHAR(72)+NCHAR(224)),
    (7, 'tenant06@rental.local', NCHAR(272)+NCHAR(7863)+NCHAR(110)+NCHAR(103)+NCHAR(32)+NCHAR(75)+NCHAR(104)+NCHAR(111)+NCHAR(97)),
    (8, 'tenant07@rental.local', NCHAR(66)+NCHAR(249)+NCHAR(105)+NCHAR(32)+NCHAR(76)+NCHAR(97)+NCHAR(110)),
    (9, 'tenant08@rental.local', NCHAR(272)+NCHAR(7895)+NCHAR(32)+NCHAR(77)+NCHAR(105)+NCHAR(110)+NCHAR(104)),
    (10, 'tenant09@rental.local', NCHAR(72)+NCHAR(111)+NCHAR(224)+NCHAR(110)+NCHAR(103)+NCHAR(32)+NCHAR(78)+NCHAR(103)+NCHAR(7885)+NCHAR(99));

IF (SELECT COUNT(*)
    FROM @names expected
    INNER JOIN dbo.users actual ON actual.id = expected.id
        AND CONVERT(VARBINARY(508), actual.email) = CONVERT(VARBINARY(508), expected.email)
        AND actual.role = 'USER') <> 9
    THROW 51020, 'Expected all nine seeded USER accounts at their original IDs.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    UPDATE actual
    SET full_name = expected.full_name
    FROM dbo.users actual
    INNER JOIN @names expected ON expected.id = actual.id
        AND CONVERT(VARBINARY(508), expected.email) = CONVERT(VARBINARY(508), actual.email)
    WHERE actual.role = 'USER';

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

SELECT id, email, full_name
FROM dbo.users
WHERE id BETWEEN 2 AND 10
ORDER BY id;
