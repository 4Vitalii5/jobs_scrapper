# Techstars Job Scraper — Installation Guide

## Prerequisites

* Java 21+
* Maven 3.6+ (or use the included Maven Wrapper)
* Docker & Docker Compose
* curl (optional, for quick API testing)

## 1) Clone the repository
```bash
git clone https://github.com/4Vitalii5/jobs_scrapper.git
cd jobs_scrapper
```

## 2) Start PostgreSQL with Docker
```bash
docker-compose up -d
```
The database will be available on port `5433` with name `techstars_db` and credentials `postgres/postgres` (see `docker-compose.yml`).

## 3) Configure environment
The application loads environment variables from a local `.env` file (see `spring.config.import=optional:file:.env[.properties]`). Create `.env` in the project root with:
```properties
POSTGRES_URL=jdbc:postgresql://localhost:5433/techstars_db
POSTGRES_USERNAME=postgres
POSTGRES_PASSWORD=postgres

# Optional: Google Sheets integration
GOOGLE_SPREADSHEET_ID=
GOOGLE_CREDENTIALS_JSON=

# Optional: pg_dump path for SQL export (on Windows you can set an absolute path)
app.db.export.pg_dump_path=pg_dump
```

## 4) Run the application
On Windows (PowerShell):
```powershell
./mvnw.cmd spring-boot:run
```

Or build and run the jar:
```powershell
./mvnw.cmd -DskipTests package
java -jar target/techstars-0.0.1-SNAPSHOT.jar
```

## 5) Access the API
* Base URL: `http://localhost:8080/api`
* Swagger UI: `http://localhost:8080/api/swagger-ui/index.html`

## 6) Start scraping
Valid values for `{jobFunction}` (enum `Function`):
```text
ADMINISTRATION
MARKETING_COMMUNICATIONS
SOFTWARE_ENGINEERING
IT
ACCOUNTING_FINANCE
OTHER_ENGINEERING
PRODUCT
PEOPLE_HR
CUSTOMER_SERVICE
DESIGN
LEGAL
SALES_BUSINESS_DEVELOPMENT
OPERATIONS
DATA_SCIENCE
QUALITY_ASSURANCE
COMPLIANCE_REGULATORY
```
Example request:
```bash
curl -X POST "http://localhost:8080/api/scrape/SOFTWARE_ENGINEERING"
```

## Troubleshooting
* If the app cannot connect to the DB, make sure `docker-compose up -d` is running and `.env` values match the exposed port `5433`.
* On Windows, set `app.db.export.pg_dump_path` to the absolute path of `pg_dump.exe` if it is not in `PATH`.
* Logs are printed to the console; you can adjust verbosity via `logging.level.*` in `application.properties`.

## Stop services
```bash
docker-compose down
```