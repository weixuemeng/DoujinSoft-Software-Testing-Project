package com.difegue.doujinsoft;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

public class TestFAQServlet {

    @BeforeAll
    static void setUpBeforeClass() {
        RestAssured.baseURI = "https://diy.tvc-16.science/";
    }

    @Test
    public void testSurveysAPI() {
        when().get("surveys").then()
                .statusCode(200)
                .contentType(containsString("text/html"))
                .body(containsString("Surveys")); // just a simple check
    }
}
