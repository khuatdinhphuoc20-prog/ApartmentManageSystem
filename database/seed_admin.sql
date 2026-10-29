-- Run manually once after schema.sql. Development-only sample Admin.
-- The password hash was generated and verified with Spring Security BCryptPasswordEncoder.
-- Share the plaintext development password with teammates outside Git.

USE [RentalApartmentDev];
GO

SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @admin_email NVARCHAR(254) = N'admin@rental.local';
DECLARE @admin_password_hash VARCHAR(255) = '$2a$10$SvFpnMzQIudAC4AnYKjchuxkjZK497w55okzg00zZ6qDv7rEWnGN2';
DECLARE @admin_full_name NVARCHAR(150) = N'Admin';
DECLARE @admin_phone VARCHAR(20) = '0900000000';
DECLARE @admin_identity_document_no NVARCHAR(30) = N'000000000000';

IF EXISTS (SELECT 1 FROM dbo.users WHERE role = 'ADMIN' AND email <> @admin_email)
    THROW 51000, 'An ADMIN already exists with a different email.', 1;

IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE role = 'ADMIN')
BEGIN
    INSERT INTO dbo.users (
        email, password_hash, full_name, phone, identity_document_no,
        role, created_at, updated_at
    ) VALUES (
        @admin_email, @admin_password_hash, @admin_full_name, @admin_phone,
        @admin_identity_document_no, 'ADMIN', SYSUTCDATETIME(), SYSUTCDATETIME()
    );
END;

COMMIT TRANSACTION;
