# Database Foundation

The shared local development database name is `RentalApartmentDev`. On each
developer's SQL Server instance, run `schema.sql` once using SSMS or `sqlcmd`.
The script creates this database if it does not exist and then creates the ten
tables and their constraints/indexes. Select the server explicitly:

```powershell
sqlcmd -S .\YOUR_INSTANCE -E -d master -b -i database/schema.sql
```

The table/index work runs in one transaction. The script stops if the database
already has tables, so do not run it again after initialization. Spring Boot
does not run it; Hibernate uses `ddl-auto=validate`.

Existing databases that were created with the earlier payment constraint need
the one-time `migration_payment_methods_cash_bank_transfer.sql` update before
VNPAY can save a new successful transaction as `BANK_TRANSFER`. It changes
the Invoice check constraint and adds a trigger; it does not update existing
Invoice rows. Historical `SANDBOX` values remain readable, while the database
blocks new SANDBOX values and the application writes only `CASH` and
`BANK_TRANSFER` for new payments.

Run `seed_admin.sql` once after `schema.sql`. It creates the shared development
Admin sample (`admin@rental.local`) with non-real phone/identity data and a
Spring Security BCrypt password hash. It does not contain the plaintext Admin
password, and it does not reset an existing Admin. M01 must use
`BCryptPasswordEncoder` for this login. Share the plaintext development Admin
password with teammates outside Git.

```powershell
sqlcmd -S .\YOUR_INSTANCE -E -d master -b -i database/seed_admin.sql
```

The Admin password is shared separately from this repository. It is unrelated
to `DB_PASSWORD`, which authenticates the application's SQL Server login.

For a complete local dataset, run `seed_demo_data.sql` once after both
`schema.sql` and `seed_admin.sql`. It requires a fresh `RentalApartmentDev`
database containing only the seeded Admin and refuses to run if business data
already exists. It adds nine USER accounts and ten rows to each of the other
nine tables; it never deletes or truncates data. This dataset exercises
application/contract statuses, all room states, completed and pending billing
cycles, `NULL` versus zero utility input, `NO_CHARGE`, unpaid/overdue/paid
Invoices, payment methods, issues, and notifications.

The sample USER accounts are `tenant01@rental.local` through
`tenant09@rental.local`; they share the development-only password
`TenantDev123!`. The Admin remains `admin@rental.local` and keeps the password
shared separately with the team.

```powershell
sqlcmd -S .\YOUR_INSTANCE -E -d master -b -f 65001 -i database/seed_demo_data.sql
```

The demo seed uses fixed example dates from 2025–2026 and is intended for local
development only. The UTF-8 input flag preserves Vietnamese sample names when
`sqlcmd` reads the seed file. If the demo accounts already show garbled names,
run `fix_demo_tenant_names.sql` once; it updates only the nine seeded USER rows
and refuses to run if their IDs/emails do not match the expected sample data.

```powershell
sqlcmd -S .\YOUR_INSTANCE -E -d master -b -i database/fix_demo_tenant_names.sql
```

If the three seeded invoice notifications show garbled Vietnamese, run
`fix_demo_invoice_notifications.sql`. It repairs only the sample notifications
for invoice #1 and invoice #7; its Vietnamese text is constructed with `NCHAR`,
so it does not depend on `sqlcmd`'s input code page.

```powershell
sqlcmd -S .\YOUR_INSTANCE -E -d master -b -i database/fix_demo_invoice_notifications.sql
```

The tracked `application.properties` reads `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD` from the environment. For a local SQL Server login, set them in
the PowerShell session that runs Maven or the application:

```powershell
$env:DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=RentalApartmentDev;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME = 'YOUR_SQL_LOGIN'
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'SQL password' -AsSecureString)).Password
.\mvnw.cmd spring-boot:run
```

Replace the server and port for your own machine. These
PowerShell variables last only for that terminal session. When starting from
an IDE, set the same three variables in its run configuration. Spring Boot
does not load a `.env` file automatically. The application validates the
existing database schema at startup. Do not commit credentials or local overrides.
