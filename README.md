# Techstars Job Scraper

A Java Spring Boot application for scraping job postings from [jobs.techstars.com](https://jobs.techstars.com) based on specific job functions. The application provides a comprehensive solution for collecting, storing, and exporting job data with advanced filtering and search capabilities.

## 🚀 Features

- **Intelligent Web Scraping**: Uses Selenium WebDriver to scrape job listings from Techstars jobs platform
- **Multi-threaded Processing**: Implements concurrent job processing for improved performance
- **Comprehensive Data Collection**: Captures all required job information including:
  - Position name and description
  - Organization details and logo
  - Job function and location
  - Posted date (Unix timestamp)
  - Tags and categories
  - Full HTML description
- **Advanced Filtering**: Filter jobs by function, location, date range, and tags
- **Data Export Options**:
  - SQL database dumps with complete schema
  - Google Sheets integration via API
  - Structured data formats
- **RESTful API**: Complete REST API for job management and scraping operations
- **Docker Support**: Full containerization with Docker Compose
- **Database Persistence**: PostgreSQL database with JPA/Hibernate ORM

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

- **Backend**: Spring Boot 3.2.0, Java 21
- **Database**: PostgreSQL 17 with JPA/Hibernate
- **Web Scraping**: Selenium WebDriver 4.21.0
- **HTTP Client**: OkHttp 4.12.0
- **HTML Parsing**: Jsoup 1.17.2
- **Build Tool**: Maven 3.6+
- **Containerization**: Docker & Docker Compose
- **API Integration**: Google Sheets API v4

## 📊 Data Model

### Job Entity
- Position name, URL, and description
- Organization details (title, URL, logo)
- Labor function and location information
- Posted date (Unix timestamp)
- Tags and categories
- Address and location details

### Organization Entity
- Company title and URL
- Logo image URL

### Tag Entity
- Tag names for job categorization

## 🔌 API Endpoints

### Scraping Operations
- `POST /api/scrape/{jobFunction}` - Scrape jobs by specific function

### Job Management
- `GET /api/jobs` - Get all jobs with pagination and sorting
- `GET /api/jobs/{id}` - Get specific job by ID
- `GET /api/jobs/function/{function}` - Get jobs by labor function
- `GET /api/jobs/location/{location}` - Get jobs by location
- `GET /api/jobs/date-range` - Get jobs within date range
- `GET /api/jobs/functions` - Get all available labor functions
- `GET /api/jobs/locations` - Get all available locations
- `GET /api/jobs/count/{function}` - Get job count by function

### Data Export
- `POST /api/export/database` - Export full database to SQL
- `POST /api/export/jobs/{function}` - Export jobs by function to SQL
- `POST /api/jobs/export/sheets/{function}` - Export to Google Sheets

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.6+
- Docker & Docker Compose
- Chrome browser (for Selenium)

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
# Scrape Software Engineering jobs
curl -X POST "http://localhost:8080/api/scrape/Software%20Engineering"
```

## 📋 Usage Examples

### Scrape Jobs by Function
```bash
# Scrape different job functions
curl -X POST "http://localhost:8080/api/scrape/Software%20Engineering"
curl -X POST "http://localhost:8080/api/scrape/Product%20Management"
curl -X POST "http://localhost:8080/api/scrape/Marketing"
```

### View and Filter Jobs
```bash
# Get all jobs with pagination
curl "http://localhost:8080/api/jobs?page=0&size=20&sortBy=postedDate&sortDir=desc"

# Get jobs by function
curl "http://localhost:8080/api/jobs/function/Software%20Engineering"

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
spring.datasource.url=jdbc:postgresql://localhost:5433/techstars_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### Google Sheets Integration
```properties
google.sheets.credentials.path=/path/to/credentials.json
google.sheets.spreadsheet.id=your-spreadsheet-id
```

### Selenium Configuration
```properties
selenium.headless=false
selenium.timeout=30
```

## 🐳 Docker Deployment

### Development
```bash
docker-compose up -d
```

### Production
```bash
# Build image
docker build -t techstars-scraper .

# Run container
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  --name techstars-app \
  techstars-scraper
```

## 📁 Project Structure

```
jobs_scrapper/
├── src/main/java/com/example/techstars/
│   ├── controller/          # REST controllers
│   │   ├── JobController.java
│   │   ├── JobScraperController.java
│   │   └── DatabaseExportController.java
│   ├── dto/                # Data Transfer Objects
│   │   ├── JobDTO.java
│   │   ├── OrganizationDTO.java
│   │   └── TagDTO.java
│   ├── model/              # JPA entities
│   │   ├── Job.java
│   │   ├── Organization.java
│   │   └── Tag.java
│   ├── repository/         # Data access layer
│   │   ├── JobRepository.java
│   │   ├── OrganizationRepository.java
│   │   └── TagRepository.java
│   ├── service/            # Business logic
│   │   ├── JobScraperService.java
│   │   ├── DatabaseExportService.java
│   │   └── GoogleSheetsService.java
│   └── TechstarsApplication.java
├── src/main/resources/
│   └── application.properties
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── INSTALL.md
└── README.md
```

## 🔍 Scraping Details

### Supported Job Functions
The application can scrape jobs from various categories including:
- Software Engineering
- Product Management
- Marketing
- Sales
- Design
- Operations
- And more...

### Data Collection Process
1. **Navigation**: Automatically navigates to jobs.techstars.com
2. **Function Selection**: Selects specified job function from dropdown
3. **Job Discovery**: Identifies all job cards on the page
4. **Data Extraction**: Extracts detailed information from each job listing
5. **Data Storage**: Saves structured data to PostgreSQL database
6. **Duplicate Prevention**: Avoids re-scraping existing jobs

### Performance Features
- Multi-threaded job processing
- Efficient database operations
- Optimized Selenium WebDriver configuration
- Connection pooling and caching

## 📊 Data Export Features

### SQL Export
- Complete database schema creation
- Data insertion statements
- Proper indexing and constraints
- Sequence reset commands

### Google Sheets Export
- Structured data formatting
- Header row with column names
- Automatic sheet creation/updating
- Error handling and logging

## 🚨 Important Notes

### Rate Limiting
- Respects website terms of service
- Implements reasonable delays between requests
- Uses proper user agent headers

### Data Accuracy
- Scraped data reflects current website content
- Regular updates recommended for fresh data
- Validation and error handling implemented

### Legal Compliance
- Ensure compliance with website terms of service
- Respect robots.txt and website policies
- Use responsibly and ethically

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:
1. Check the [INSTALL.md](INSTALL.md) for setup instructions
2. Review the API documentation
3. Check application logs for error details
4. Open an issue in the repository

## 🔮 Future Enhancements

- [ ] Real-time job notifications
- [ ] Advanced analytics and reporting
- [ ] Machine learning job matching
- [ ] Mobile application
- [ ] Additional job board support
- [ ] Enhanced filtering and search
- [ ] Job application tracking
- [ ] Resume parsing and matching

---

**Built with ❤️ using Spring Boot and modern Java technologies** 
