package com.example.techstars.controller;

import static com.example.techstars.controller.JobControllerTest.TestResources.BASE_RESOURCE_PATH;
import static com.example.techstars.controller.JobControllerTest.TestResources.DATASETS_PATH;
import static com.example.techstars.controller.JobControllerTest.TestResources.JOBS_API_URL;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.example.techstars.config.AbstractIntegrationTest;
import com.example.techstars.util.TestUtils;
import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.spring.api.DBRider;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DBRider
@DBUnit(caseSensitiveTableNames = true)
class JobControllerTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @SneakyThrows
    @DataSet(value = DATASETS_PATH + "jobs.yml")
    void givenJobsExist_getAllJobs_shouldReturnPaginatedJobs() {
        String response = given()
                .queryParam("page", 0)
                .queryParam("size", 2)
                .queryParam("sort", "postedDate,desc")
                .contentType(ContentType.JSON)
                .when()
                .get(JOBS_API_URL)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .body()
                .asString();

        JSONAssert.assertEquals(
                TestUtils.readResource(BASE_RESOURCE_PATH + "get_all_jobs_paginated.json"),
                response,
                JSONCompareMode.LENIENT
        );
    }

    @Test
    @SneakyThrows
    @DataSet(value = DATASETS_PATH + "jobs.yml")
    void givenJobIdExists_getJobById_shouldReturnJob() {
        String response = given()
                .contentType(ContentType.JSON)
                .when()
                .get(JOBS_API_URL + "/1")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .body()
                .asString();

        JSONAssert.assertEquals(
                TestUtils.readResource(BASE_RESOURCE_PATH + "get_job_by_id.json"),
                response,
                JSONCompareMode.LENIENT
        );
    }

    @Test
    @DataSet(value = DATASETS_PATH + "jobs.yml")
    void givenJobIdNotExists_getJobById_shouldReturnNotFound() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(JOBS_API_URL + "/999")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("message", equalTo("Job with ID 999 not found."));
    }

    @Test
    @SneakyThrows
    @DataSet(value = DATASETS_PATH + "jobs.yml")
    void givenFunctionExists_getJobsByFunction_shouldReturnJobs() {
        String response = given()
                .contentType(ContentType.JSON)
                .when()
                .get(JOBS_API_URL + "/function/SOFTWARE_ENGINEERING")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .body()
                .asString();

        JSONAssert.assertEquals(
                TestUtils.readResource(BASE_RESOURCE_PATH + "get_jobs_by_function.json"),
                response,
                JSONCompareMode.LENIENT
        );
    }

    @Test
    @DataSet(value = DATASETS_PATH + "jobs.yml")
    void givenLocationExists_getJobsByLocation_shouldReturnJobs() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(JOBS_API_URL + "/location/New York")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(2));
    }

    @Test
    @DataSet(value = DATASETS_PATH + "jobs.yml")
    void givenDateRange_getJobsByDateRange_shouldReturnJobs() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("startDate", 1672531200)
                .queryParam("endDate", 1675209600)
                .when()
                .get(JOBS_API_URL + "/date-range")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(2));
    }

    @Test
    @DataSet(value = DATASETS_PATH + "jobs.yml")
    void givenJobsExist_getAllLaborFunctions_shouldReturnDistinctFunctions() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(JOBS_API_URL + "/functions")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(2))
                .body("$", contains("Product", "Software Engineering"));
    }

    @Test
    @DataSet(value = DATASETS_PATH + "jobs.yml")
    void givenJobsExist_getAllLocations_shouldReturnDistinctLocations() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(JOBS_API_URL + "/locations")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(3))
                .body("$", contains("London", "New York, NY", "Remote"));
    }

    static class TestResources {
        static final String JOBS_API_URL = "/api/jobs";
        static final String DATASETS_PATH = "datasets/";
        static final String BASE_RESOURCE_PATH = "com/example/techstars/controller/";
    }
}