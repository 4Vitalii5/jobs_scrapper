package com.example.techstars.controller;

import static com.example.techstars.controller.JobScraperControllerTest.TestResources.API_SCRAPE_URL;
import static com.example.techstars.controller.JobScraperControllerTest.TestResources.INVALID_JOB_FUNCTION;
import static com.example.techstars.controller.JobScraperControllerTest.TestResources.JOB_FUNCTION;
import static com.example.techstars.controller.JobScraperControllerTest.TestResources.SERVICE_EXCEPTION_MESSAGE;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.example.techstars.config.AbstractIntegrationTest;
import com.example.techstars.model.Function;
import com.example.techstars.service.ScraperService;
import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.spring.api.DBRider;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DBRider
@DBUnit(caseSensitiveTableNames = true)
class JobScraperControllerTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @SpyBean
    private ScraperService scraperService;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void givenValidJobFunction_scrapeJobs_shouldTriggerScrapingAndReturnOk() {
        // Given
        doNothing().when(scraperService).scrapeJobsByFunction(anyString());

        // When
        given()
                .post(API_SCRAPE_URL, JOB_FUNCTION.name())
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(equalTo("Scraping process for job function '" + JOB_FUNCTION
                        + "' started in the background."));

        // Then
        verify(scraperService, timeout(1000).times(1)).scrapeJobsByFunction(JOB_FUNCTION.getLabel());
    }

    @Test
    void givenInvalidJobFunction_scrapeJobs_shouldReturnBadRequest() {
        // When
        given()
                .post(API_SCRAPE_URL, INVALID_JOB_FUNCTION)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("timestamp", notNullValue())
                .body("status", equalTo(HttpStatus.BAD_REQUEST.value()))
                .body("error", equalTo("Bad Request"))
                .body("message", equalTo("The parameter 'jobFunction' of value 'INVALID_FUNCTION' could not be converted to type 'Function'"))
                .body("path", equalTo("/api/scrape/" + INVALID_JOB_FUNCTION));
    }

    @Test
    void givenScraperServiceThrowsException_scrapeJobs_shouldReturnInternalServerError() {
        // Given
        doThrow(new RuntimeException(SERVICE_EXCEPTION_MESSAGE)).when(scraperService).scrapeJobsByFunction(anyString());

        // When
        given()
                .post(API_SCRAPE_URL, JOB_FUNCTION.name())
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("timestamp", notNullValue())
                .body("status", equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .body("error", equalTo("Internal Server Error"))
                .body("message", equalTo(
                        "An unexpected error occurred: " + SERVICE_EXCEPTION_MESSAGE))
                .body("path", equalTo("/api/scrape/" + JOB_FUNCTION.name()));
    }

    static class TestResources {
        static final Function JOB_FUNCTION = Function.SOFTWARE_ENGINEERING;
        static final String INVALID_JOB_FUNCTION = "INVALID_FUNCTION";
        static final String SERVICE_EXCEPTION_MESSAGE = "Scraping failed";
        static final String API_SCRAPE_URL = "/api/scrape/{jobFunction}";
    }
}