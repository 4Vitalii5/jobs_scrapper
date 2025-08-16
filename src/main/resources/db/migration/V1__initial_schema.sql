-- Створення таблиці для організацій
CREATE TABLE organization (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL UNIQUE,
    logo_url VARCHAR(500) NOT NULL
);

-- Створення таблиці для тегів
CREATE TABLE tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Створення таблиці для локацій (НОВА ТАБЛИЦЯ)
CREATE TABLE location (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Створення таблиці для вакансій (ОНОВЛЕНА СТРУКТУРА)
CREATE TABLE job (
    id BIGSERIAL PRIMARY KEY,
    position_name VARCHAR(500) NOT NULL,
    job_page_url VARCHAR(500) NOT NULL UNIQUE,
    labor_function VARCHAR(255) NOT NULL,
    posted_date BIGINT NOT NULL,
    description TEXT,
    organization_id BIGINT REFERENCES organization(id)
);

-- Створення проміжної таблиці для зв'язку "багато-до-багатьох" між вакансіями та тегами
CREATE TABLE job_tag (
    job_id BIGINT REFERENCES job(id),
    tag_id BIGINT REFERENCES tag(id),
    PRIMARY KEY (job_id, tag_id)
);

-- Створення проміжної таблиці для зв'язку "багато-до-багатьох" між вакансіями та локаціями
CREATE TABLE job_location (
    job_id BIGINT REFERENCES job(id),
    location_id BIGINT REFERENCES location(id),
    PRIMARY KEY (job_id, location_id)
);

-- Створення індексів для покращення продуктивності запитів
CREATE INDEX idx_job_labor_function ON job(labor_function);
CREATE INDEX idx_job_posted_date ON job(posted_date);
CREATE INDEX idx_organization_url ON organization(url);
CREATE INDEX idx_tag_name ON tag(name);
CREATE INDEX idx_location_name ON location(name);