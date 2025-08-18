CREATE TABLE job (
    id BIGSERIAL PRIMARY KEY,
    position_name VARCHAR(500) NOT NULL,
    job_page_url VARCHAR(500) NOT NULL UNIQUE,
    labor_function VARCHAR(255) NOT NULL,
    posted_date BIGINT NOT NULL,
    description TEXT,
    organization_id BIGINT REFERENCES organization(id)
);

CREATE TABLE job_tag (
    job_id BIGINT REFERENCES job(id),
    tag_id BIGINT REFERENCES tag(id),
    PRIMARY KEY (job_id, tag_id)
);

CREATE TABLE job_location (
    job_id BIGINT REFERENCES job(id),
    location_id BIGINT REFERENCES location(id),
    PRIMARY KEY (job_id, location_id)
);

CREATE INDEX idx_job_page_url ON job(job_page_url);
CREATE INDEX idx_job_labor_function ON job(labor_function);
CREATE INDEX idx_job_posted_date ON job(posted_date);