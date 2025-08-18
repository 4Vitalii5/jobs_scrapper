CREATE TABLE organization (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL UNIQUE,
    logo_url VARCHAR(500) NOT NULL
);

CREATE INDEX idx_organization_url ON organization(url);