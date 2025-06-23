# Installation and Setup Guide

This guide provides instructions on how to set up and run the Techstars Job Scraper application.

## Prerequisites

- **Java Development Kit (JDK)**: Version 21 or higher.
- **Apache Maven**: For building the project and managing dependencies.
- **PostgreSQL**: The database used for storing scraped data (can be installed locally or run via Docker).
- **PostgreSQL Command Line Tools**: Specifically `pg_dump`, which must be available in your system's PATH or configured in `application.properties`.

## Database Setup Options

### Option 1: Using Docker for Database (Recommended)

If you have Docker installed, you can easily start PostgreSQL using the provided docker-compose.yml:

```bash
# Start PostgreSQL database
docker-compose up -d

# The database will be available at:
# - Host: localhost
# - Port: 5433
# - Database: techstars_db
# - Username: postgres
# - Password: postgres
```

To stop the database:
```bash
docker-compose down
```

### Option 2: Local PostgreSQL Installation

1. **Install and Start PostgreSQL**: If you haven't already, install PostgreSQL and ensure the database server is running.

2. **Create a Database**: Create a new database for the application. The default configuration expects a database named `techstars_db`.
    ```sql
    CREATE DATABASE techstars_db;
    ```

## Application Configuration

1.  **Clone the Repository**:
    ```sh
    git clone https://github.com/4Vitalii5/jobs_scrapper.git
    cd techstars-job-scraper
    ```

2.  **Configure Database Connection**:
    Open the `src/main/resources/application.properties` file and update the following properties to match your PostgreSQL setup:
    
    **For Docker database:**
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5433/techstars_db
    spring.datasource.username=postgres
    spring.datasource.password=postgres
    ```
    
    **For local PostgreSQL:**
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/techstars_db
    spring.datasource.username=your_db_username
    spring.datasource.password=your_db_password
    ```

3.  **Configure `pg_dump` Path (Important)**:
    This application uses the `pg_dump` utility to export the database. You need to tell the application where to find it.
    -   **Option A (Recommended)**: Add the `bin` directory of your PostgreSQL installation (e.g., `C:\Program Files\PostgreSQL\14\bin` on Windows) to your system's `PATH` environment variable.
    -   **Option B**: If you do not wish to modify your `PATH`, you can specify the full path to `pg_dump.exe` in `application.properties`:
        ```properties
        app.db.export.pg_dump_path=C:/Program Files/PostgreSQL/14/bin/pg_dump.exe
        ```

## Build and Run the Application

1.  **Build the Project**:
    Use Maven to build the application from the root directory:
    ```sh
    mvn clean install
    ```

2.  **Run the Application**:
    You can run the application using the Spring Boot Maven plugin:
    ```sh
    mvn spring-boot:run
    ```
    Alternatively, you can run the JAR file from the `target` directory:
    ```sh
    java -jar target/techstars-0.0.1-SNAPSHOT.jar
    ```

The application will start and be accessible at `http://localhost:8080`.

## How to Use

Once the application is running, you can interact with it via the REST API endpoints described in the `README.md` file.

- **To start scraping**: `curl -X POST http://localhost:8080/scrape/Software%20Engineering`
- **To view jobs**: `curl http://localhost:8080/jobs`
- **To export the database**: `curl -X POST "http://localhost:8080/export-sql"`

## Scrape Jobs
Trigger scraping for a job function (e.g., "Software Engineering"):
```bash
    curl -X POST http://localhost:8080/scrape/Software%20Engineering
```

## Access the API
- List jobs: `GET http://localhost:8080/jobs`
- Filter/sort jobs: `GET http://localhost:8080/jobs?location=Remote&sort=asc`
- Get job by ID: `GET http://localhost:8080/jobs/{id}`
- List organizations: `GET http://localhost:8080/organizations`
- List tags: `GET http://localhost:8080/tags`

## Export SQL Dump
Export the full database (schema + data) to a SQL file:
```bash
    curl -X POST "http://localhost:8080/export-sql?filePath=./techstars_dump.sql"
```
- The file will be created at the specified path.
- If you use a password, you may need to set the `PGPASSWORD` environment variable or edit the export service.

## Troubleshooting
- **pg_dump not found:** Ensure PostgreSQL tools are installed and `pg_dump` is in your system PATH.
- **Database connection errors:** Double-check your `application.properties` for correct URL, username, and password.
- **Port conflicts:** Make sure PostgreSQL is running on the port specified (default: 5432 for local, 5433 for Docker).
- **Docker database issues:** Ensure Docker is running and ports are not in use.

## Notes
- The application will auto-create tables on first run (`spring.jpa.hibernate.ddl-auto=update`).
- For production, review security and database settings.
- Using Docker for the database is convenient for development but requires Docker to be installed.

---
For more details, see the [README.md](README.md). 
