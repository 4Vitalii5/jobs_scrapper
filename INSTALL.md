# Techstars Job Scraper - Installation Guide

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- Docker and Docker Compose
- PostgreSQL (if running locally)
- Chrome browser (for Selenium)

## Quick Start with Docker (Recommended)

### 1. Clone the repository
```bash
git clone <your-repository-url>
cd jobs_scrapper
```

### 2. Run with Docker Compose
```bash
docker-compose up -d
```

This will:
- Start PostgreSQL database on port 5433
- Build and start the Spring Boot application on port 8080
- Create necessary volumes for data persistence

### 3. Access the application
- API Base URL: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui.html

## Manual Installation

### 1. Database Setup

#### Option A: Using Docker
```bash
docker run --name techstars_postgres \
  -e POSTGRES_DB=techstars_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5433:5432 \
  -d postgres:17-alpine
```

#### Option B: Local PostgreSQL
1. Install PostgreSQL
2. Create database: `createdb techstars_db`
3. Update `application.properties` with your database credentials

### 2. Application Setup

#### Build the project
```bash
mvn clean install
```

#### Run the application
```bash
mvn spring-boot:run
```

## Configuration

### Database Configuration
Update `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5433/techstars_db
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### Google Sheets Integration (Optional)
To enable Google Sheets export:

1. Create a Google Cloud Project
2. Enable Google Sheets API
3. Create service account credentials
4. Download the JSON credentials file
5. Update `application.properties`:

```properties
google.sheets.credentials.path=/path/to/your/credentials.json
google.sheets.spreadsheet.id=your-spreadsheet-id
```

## Usage

### 1. Scrape Jobs
```bash
# Scrape jobs by function
curl -X POST "http://localhost:8080/api/scrape/Software%20Engineering"
```

### 2. View Jobs
```bash
# Get all jobs
curl "http://localhost:8080/api/jobs"

# Get jobs by function
curl "http://localhost:8080/api/jobs/function/Software%20Engineering"

# Get jobs by location
curl "http://localhost:8080/api/jobs/location/New%20York"
```

### 3. Export Data
```bash
# Export to SQL file
curl -X POST "http://localhost:8080/api/export/jobs/Software%20Engineering"

# Export to Google Sheets (if configured)
curl -X POST "http://localhost:8080/api/jobs/export/sheets/Software%20Engineering"
```

## API Endpoints

### Scraping
- `POST /api/scrape/{jobFunction}` - Scrape jobs by function

### Jobs
- `GET /api/jobs` - Get all jobs with pagination
- `GET /api/jobs/{id}` - Get job by ID
- `GET /api/jobs/function/{laborFunction}` - Get jobs by function
- `GET /api/jobs/location/{location}` - Get jobs by location
- `GET /api/jobs/date-range?startDate={start}&endDate={end}` - Get jobs by date range
- `GET /api/jobs/functions` - Get all available labor functions
- `GET /api/jobs/locations` - Get all available locations
- `GET /api/jobs/count/{function}` - Get job count by function

### Export
- `POST /api/export/database` - Export full database to SQL
- `POST /api/export/jobs/{laborFunction}` - Export jobs by function to SQL
- `POST /api/jobs/export/sheets/{laborFunction}` - Export jobs to Google Sheets

## Troubleshooting

### Common Issues

1. **Chrome/ChromeDriver issues**
   - Ensure Chrome is installed
   - Check Chrome version compatibility
   - Update WebDriverManager if needed

2. **Database connection issues**
   - Verify PostgreSQL is running
   - Check database credentials
   - Ensure database exists

3. **Selenium timeout issues**
   - Increase timeout in `application.properties`
   - Check internet connection
   - Verify target website accessibility

### Logs
Check application logs:
```bash
# Docker
docker-compose logs app

# Local
tail -f logs/application.log
```

## Development

### Project Structure
```
src/main/java/com/example/techstars/
├── controller/     # REST controllers
├── dto/           # Data Transfer Objects
├── model/         # JPA entities
├── repository/    # Data access layer
├── service/       # Business logic
└── TechstarsApplication.java
```

### Adding New Features
1. Create/update models in `model/` package
2. Add repository methods in `repository/` package
3. Implement business logic in `service/` package
4. Create REST endpoints in `controller/` package
5. Add DTOs if needed in `dto/` package

## Performance Considerations

- The application uses multithreading for job processing
- Database indexes are created for common queries
- Selenium WebDriver is configured for optimal performance
- Consider using headless mode for production deployments

## Security Notes

- The application includes CORS configuration for development
- Database credentials should be externalized in production
- Google Sheets credentials should be secured
- Consider adding authentication for production use

## Support

For issues and questions:
1. Check the logs for error details
2. Verify configuration settings
3. Test with a simple job function first
4. Ensure all prerequisites are met 
