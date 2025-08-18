# Techstars Job Scraper

A Java Spring Boot application for scraping job postings from [jobs.techstars.com](https://jobs.techstars.com) based on specific job functions. The application provides a comprehensive solution for collecting, storing, and exporting job data with advanced filtering and search capabilities.

## 🚀 Features

- **Efficient Data Scraping**: Utilizes the Getro API to fetch job lists and Jsoup to parse detailed descriptions.
- **Asynchronous Processing**: Data is saved to the database asynchronously to improve performance.
- **Comprehensive Data Collection**: Gathers all necessary job information, including:
  - Position name and URL
  - Organization details and logo
  - Job function and locations
  - Posted date (in Unix Timestamp format)
  - Tags
  - Full HTML description
- **Advanced Filtering**: Filter jobs by function, location, and date range.
- **Data Export Options**:
  - Export to an SQL file with the complete database schema.
  - Integration with Google Sheets via API.
- **RESTful API**: A complete REST API for managing jobs and initiating the scraping process.
- **Docker Support**: Fully containerized with Docker Compose.
- **Data Persistence**: PostgreSQL database with JPA/Hibernate.

## 🏗️ Architecture

The application follows a clean, layered architecture:

```
┌─────────────────┐
│   Controllers   │  ← REST API endpoints
├─────────────────┤
│     Services    │  ← Business logic
├─────────────────┤
│   Repositories  │  ← Data access layer
├─────────────────┤
│     Models      │  ← JPA entities
├─────────────────┤
│   PostgreSQL    │  ← Database
└─────────────────┘
```

## 🛠️ Technology Stack

- **Backend**: Spring Boot 3.3.4, Java 21
- **Database**: PostgreSQL 17 with JPA/Hibernate
- **HTTP Client**: OpenFeign
- **HTML Parsing**: Jsoup 1.21.1
- **Build Tool**: Maven
- **Containerization**: Docker & Docker Compose
- **API Integration**: Google Sheets API v4

## 🔌 API Endpoints

### Scraping Operations
- `POST /api/scrape/{jobFunction}` - Start scraping jobs for a specific function.

### Job Management
- `GET /api/jobs` - Get all jobs with pagination and sorting.
- `GET /api/jobs/{id}` - Get a job by its ID.
- `GET /api/jobs/function/{function}` - Get jobs by function.
- `GET /api/jobs/location/{location}` - Get jobs by location.
- `GET /api/jobs/date-range` - Get jobs within a date range.
- `GET /api/jobs/functions` - Get a list of all available job functions.
- `GET /api/jobs/locations` - Get a list of all available locations.
- `GET /api/jobs/count/{function}` - Get the number of jobs for a function.

### Data Export
- `POST /api/export/sql/database` - Export the entire database to SQL.
- `POST /api/export/{format}/{function}` - Export jobs by function in the specified format (`sql` or `sheets`).

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.6+
- Docker & Docker Compose

### 1. Clone Repository
```bash
  git clone https://github.com/4Vitalii5/jobs_scrapper.git
  cd jobs_scrapper
```

### 2. Run with Docker
```bash
  docker-compose up -d
```

### 3. Access Application
- API: http://localhost:8080/api
- Database: localhost:5433

### 4. Start Scraping
```bash
  # Scrape jobs for Software Engineering
  curl -X POST "http://localhost:8080/api/scrape/SOFTWARE_ENGINEERING"
```

## 📋 Usage Examples

### View and Filter Jobs
```bash
  # Get all jobs with pagination
  curl "http://localhost:8080/api/jobs?page=0&size=10&sort=postedDate,desc"
  
  # Get jobs by function
  curl "http://localhost:8080/api/jobs/function/SOFTWARE_ENGINEERING"
  
  # Get jobs by location
  curl "http://localhost:8080/api/jobs/location/Remote"
```

### Export Data
```bash
# Export to SQL file
  curl -X POST "http://localhost:8080/api/export/jobs/Software%20Engineering"
  
  # Export to Google Sheets (if configured)
  curl -X POST "http://localhost:8080/api/jobs/export/sheets/Software%20Engineering"
```

## 🔧 Configuration

### Database Settings
```properties
spring.datasource.url=${POSTGRES_URL}
spring.datasource.username=${POSTGRES_USERNAME}
spring.datasource.password=${POSTGRES_PASSWORD}
```

### Google Sheets Integration
```properties
google.sheets.spreadsheetId=${GOOGLE_SPREADSHEET_ID}
google.sheets.credentials.base64=${GOOGLE_CREDENTIALS_JSON}
```

## 🐳 Docker Deployment

### Development
```bash
  docker-compose up -d
```

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:
1. Check the [INSTALL.md](INSTALL.md) for setup instructions
2. Review the API documentation
3. Check application logs for error details
4. Open an issue in the repository

---

**Built with ❤️ using Spring Boot and modern Java technologies** 
