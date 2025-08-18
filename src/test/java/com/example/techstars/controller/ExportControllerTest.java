package com.example.techstars.controller;

import static com.example.techstars.controller.ExportControllerTest.TestResources.DATASETS_PATH;
import static com.example.techstars.controller.ExportControllerTest.TestResources.EXPORT_API_URL;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.example.techstars.config.AbstractIntegrationTest;
import com.example.techstars.model.Function;
import com.example.techstars.service.impl.SheetExportServiceImpl;
import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.spring.api.DBRider;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DBRider
@DBUnit(caseSensitiveTableNames = true)
class ExportControllerTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @MockBean
    private SheetExportServiceImpl sheetExportService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @SneakyThrows
    @DataSet(value = DATASETS_PATH + "jobs.yml")
    void exportFullDatabase_shouldCreateSqlFileWithAllData() {
        String response = given()
                .contentType(ContentType.JSON)
                .when()
                .post(EXPORT_API_URL + "/sql/database")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .body()
                .asString();

        assertThat(response).contains("Full database exported successfully to file");

        String fileName = response.substring(response.indexOf("techstars_"));
        Path filePath = Path.of(fileName);

        assertThat(Files.exists(filePath)).isTrue();
        String fileContent = Files.readString(filePath);
        assertThat(fileContent).contains("-- Techstars Jobs Full Data Export");
        assertThat(fileContent).contains("Senior Java Developer");
        assertThat(fileContent).contains("DevOps Engineer");
        assertThat(fileContent).contains("Product Manager");

        Files.delete(filePath);
    }

    @Test
    void givenUnsupportedFormat_exportByFunction_shouldReturnBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .post(EXPORT_API_URL + "/csv/" + Function.SOFTWARE_ENGINEERING.getLabel())
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("error", equalTo("400 BAD_REQUEST"))
                .body("message", equalTo("Unsupported export format: csv"));
    }


    static class TestResources {
        static final String EXPORT_API_URL = "/api/export";
        static final String DATASETS_PATH = "datasets/";
    }
}