# Techstars Job Scraper

A Spring Boot application for scraping job listings from [jobs.techstars.com](https://jobs.techstars.com) by job function, storing them in a PostgreSQL database, and exposing the data via a RESTful API.

## Features
- Scrape jobs by job function (e.g., "Software Engineering")
- Store jobs, organizations, and tags in a relational database
- REST API for jobs, organizations, and tags (with filtering and sorting)
- Export the full database (schema + data) to a SQL file

## Tech Stack
- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Selenium WebDriver (for dynamic content scraping)
- Maven

## Quick Start
```bash
# Clone the repository
git clone [<repository-url>](https://github.com/4Vitalii5/jobs_scrapper)
cd techstars-job-scraper

# Build the application
mvn clean install

# Start the application
mvn spring-boot:run
# or
java -jar target/techstars-0.0.1-SNAPSHOT.jar

# Test the application
curl -X POST http://localhost:8080/scrape/Software%20Engineering
curl http://localhost:8080/jobs
```

## Main Endpoints
- `POST   /scrape/{jobFunction}` — Scrape and save jobs for a given function
- `GET    /jobs` — List all jobs (with optional filters)
- `GET    /jobs/{id}` — Get job by ID
- `GET    /organizations` — List all organizations
- `GET    /organizations/{id}` — Get organization by ID
- `GET    /tags` — List all tags
- `GET    /tags/{id}` — Get tag by ID
- `POST   /export-sql?filePath=...` — Export the database to a SQL file

## Usage Example
1. Start the application (see INSTALL.md for setup).
2. Scrape jobs:
   ```bash
   curl -X POST http://localhost:8080/scrape/Software%20Engineering
   ```
3. List jobs:
   ```bash
   curl http://localhost:8080/jobs
   ```
4. Export database:
   ```bash
   curl -X POST "http://localhost:8080/export-sql?filePath=./techstars_dump.sql"
   ```

## Requirements
- Java 21 or later
- PostgreSQL (running and accessible)
- `pg_dump` installed and in your PATH (for SQL export)

## Installation & Configuration
See [INSTALL.md](INSTALL.md) for detailed setup and configuration instructions. 
