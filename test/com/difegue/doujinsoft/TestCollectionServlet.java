package com.difegue.doujinsoft;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class TestCollectionServlet {

    @BeforeAll
    static void setUpBeforeClass() {
        RestAssured.baseURI = "https://diy.tvc-16.science/";
    }

    // the author does not assign a 404 for this
    // and it is against the common practice
    // that's why I regard it as a failure
    @ParameterizedTest
    @CsvSource({
            "bninsoftaaa",
            "newmio_maaa",
            "d_newmio_raaa"
    })
    public void testCollectionAPIWithNotExistId(String id) {
        given()
                .queryParam("id", id)
                .when()
                .get("collection")
                .then()
                .statusCode(greaterThanOrEqualTo(400))
                .contentType(containsString("text/html"))
                .body(containsString("Collection doesn't exist!"));
    }

    @ParameterizedTest
    @CsvSource({
            "bninsoftaaaa",
            "newmio_maaa",
            "d_newmio_raaaa"
    })
    public void testJsonCollectionAPIWithNotExistId(String id) {
        given()
                .queryParam("id", id)
                .queryParam("format", "json")
                .when()
                .get("collection")
                .then()
                .statusCode(greaterThanOrEqualTo(400))// same
                .contentType(containsString("application/json"))
                .body(containsString("Collection doesn't exist!"));
    }

    @ParameterizedTest
    @CsvSource({
            "bninsoft",
            "newmio_m",
            "d_newmio_r"
    })
    public void testCollectionAPI(String id) {
        given()
                .queryParam("id", id)
                .when()
                .get("collection")
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"));
    }

    @ParameterizedTest
    @CsvSource({
            "bninsoft",
            "newmio_m",
            "d_newmio_r"
    })
    public void testJsonCollectionAPI(String id) {
        given()
                .queryParam("id", id)
                .queryParam("format", "json")
                .when()
                .get("collection")
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"));
    }
}
