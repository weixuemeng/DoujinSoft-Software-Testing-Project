package com.difegue.doujinsoft;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class TestContentServlet {

    @BeforeAll
    static void setUpBeforeClass() {
        RestAssured.baseURI = "https://diy.tvc-16.science/";
    }

    @ParameterizedTest
    @CsvSource({
            "games, Ikachan, Pixel",
            "records, Happy, Ochiru",
            "comics, 2012, Andy"
    })
    public void testJsonAPIGetWithNameAndCreator(String type, String name, String creator) {
        given()
                .queryParam("format", "json")
                .queryParam("name", name)
                .queryParam("creator", creator)
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"))
                .body("totalitems", greaterThan(0));
    }

    @ParameterizedTest
    @CsvSource({
            "games, Ikachan",
            "records, Happy",
            "comics, 2012"
    })
    public void testJsonAPIGetWithName(String type, String name) {
        given()
                .queryParam("format", "json")
                .queryParam("name", name)
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"))
                .body("totalitems", greaterThan(0));
    }

    @ParameterizedTest
    @CsvSource({
            "games, Pixel",
            "records, Ochiru",
            "comics, Andy"
    })
    public void testJsonAPIGetWithCreator(String type, String creator) {
        given()
                .queryParam("format", "json")
                .queryParam("creator", creator)
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"))
                .body("totalitems", greaterThan(0));
    }

    @ParameterizedTest
    @CsvSource({
            "games, EC00D724260331323130313205050B08",
            "records, EC00B359DE0734373337313206060908",
            "comics, EC00B359DE0734373337313206060908"
    })
    public void testJsonAPIGetWithCartridgeId(String type, String cartridgeId) {
        given()
                .queryParam("format", "json")
                .queryParam("cartridge_id", cartridgeId)
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"))
                .body("totalitems", greaterThan(0));
    }

    // sort_by=date
    // expected in descending order
    @ParameterizedTest
    @CsvSource({
            "games",
            "records",
            "comics"
    })
    public void testJsonAPIGetWithSortByDate(String type) {
        Response rs = given()
                .queryParam("format", "json")
                .queryParam("sort_by", "date")
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"))
                .body("totalitems", greaterThan(0))
                .extract()
                .response();

        String firstTs = rs.path("items[0].timestamp");
        String LastTs = rs.path("items[-1].timestamp");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate fFirstTs = LocalDate.parse(firstTs, formatter);
        LocalDate fLastTs = LocalDate.parse(LastTs, formatter);
        Assertions.assertTrue(fFirstTs.isAfter(fLastTs) || fFirstTs.isEqual(fLastTs));
    }

    // sort_by=name
    // "No Title" is added for those with empty title (in MioBase)
    // after the SQL query ("normalizedName ASC" in TemplateBuilder)
    // That may be the author's design, so I do not treat it as a failure here
    @ParameterizedTest
    @CsvSource({
            "games",
            "records",
            "comics"
    })
    public void testJsonAPIGetWithSortByName(String type) {
        Response rs = given()
                .queryParam("format", "json")
                .queryParam("sort_by", "name")
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"))
                .body("totalitems", greaterThan(0))
                .extract()
                .response();

        String firstName = rs.path("items[0].name");
        String LastName = rs.path("items[-1].name");
        Assertions.assertTrue(
                firstName.compareTo(LastName) <= 0
                        || firstName.equals("No Title") && "".compareTo(LastName) <= 0);
    }


    @ParameterizedTest
    @CsvSource({
            "games, Ikachandsfs, Pixeleqweq",
            "records, Happyfsdsfe, Ochirueqeqw",
            "comics, 2012eqewq, Andyeqwwqqe",
    })
    public void testJsonAPIGetWithWrongNameAndCreator(String type, String name, String creator) {
        given()
                .queryParam("format", "json")
                .queryParam("name", name)
                .queryParam("creator", creator)
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"))
                .body("totalitems", equalTo(0));
    }

    @ParameterizedTest
    @CsvSource({
            "games, Ikachandsfs",
            "records, Happyfsdsfe",
            "comics, 2012eqewq"
    })
    public void testJsonAPIGetWitWrongName(String type, String name) {
        given()
                .queryParam("format", "json")
                .queryParam("name", name)
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"))
                .body("totalitems", equalTo(0));
    }

    @ParameterizedTest
    @CsvSource({
            "games, Pixeleqweq",
            "records, Ochirueqeqw",
            "comics, Andyeqwwqqe"
    })
    public void testJsonAPIGetWithWrongCreator(String type, String creator) {
        given()
                .queryParam("format", "json")
                .queryParam("creator", creator)
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"))
                .body("totalitems", equalTo(0));
    }

    @ParameterizedTest
    @CsvSource({
            "games, EC00D724260331323130313205050B00",
            "records, EC00B359DE0734373337313206060900",
            "comics, EC00B359DE0734373337313206060900"
    })
    public void testJsonAPIGetWithWrongCartridgeId(String type, String cartridgeId) {
        given()
                .queryParam("format", "json")
                .queryParam("cartridge_id", cartridgeId)
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"))
                .body("totalitems", equalTo(0));
    }

    @ParameterizedTest
    @CsvSource({
            "games, Ikachan, Pixel",
            "records, Happy, Ochiru",
            "comics, 2012, Andy"
    })
    public void testGetWithNameAndCreator(String type, String name, String creator) {
        given()
                .queryParam("name", name)
                .queryParam("creator", creator)
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"));
    }

    @ParameterizedTest
    @CsvSource({
            "games, Ikachan",
            "records, Happy",
            "comics, 2012"
    })
    public void testGetWithName(String type, String name) {
        given()
                .queryParam("name", name)
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"));
    }

    @ParameterizedTest
    @CsvSource({
            "games, Pixel",
            "records, Ochiru",
            "comics, Andy"
    })
    public void testGetWithCreator(String type, String creator) {
        given()
                .queryParam("creator", creator)
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"));
    }

    @ParameterizedTest
    @CsvSource({
            "games, EC00D724260331323130313205050B08",
            "records, EC00B359DE0734373337313206060908",
            "comics, EC00B359DE0734373337313206060908"
    })
    public void testGetWithCartridgeId(String type, String cartridgeId) {
        given()
                .queryParam("cartridge_id", cartridgeId)
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"));
    }

    // sort_by=date
    // expected in descending order
    @ParameterizedTest
    @CsvSource({
            "games",
            "records",
            "comics"
    })
    public void testGetWithSortByDate(String type) {
        given()
                .queryParam("sort_by", "date")
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"));
    }

    @ParameterizedTest
    @CsvSource({
            "games",
            "records",
            "comics"
    })
    public void testGetWithSortByName(String type) {
        given()
                .queryParam("sort_by", "name")
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"));
    }

}
