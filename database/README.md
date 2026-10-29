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

The tracked `application.properties` reads `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD` from the environment. For a local SQL Server login, set them in
the PowerShell session that runs Maven or the application:

```powershell
$env:DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=RentalApartmentDev;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME = 'YOUR_SQL_LOGIN'
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'SQL password' -AsSecureString)).Password
.\mvnw.cmd test
```

Replace the server and port for your own machine. These
PowerShell variables last only for that terminal session. When starting from
an IDE, set the same three variables in its run configuration. Spring Boot
does not load a `.env` file automatically. The database integration tests are
skipped when `DB_URL` is absent. Do not commit credentials or local overrides.
