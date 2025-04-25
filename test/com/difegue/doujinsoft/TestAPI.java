package com.difegue.doujinsoft;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

public class TestAPI {

    @BeforeAll
    static void setUpBeforeClass() {
        RestAssured.baseURI = "https://diy.tvc-16.science/";
    }

    // content type: application/octet-stream
    // which means unknown binary
    @ParameterizedTest
    @CsvSource({
            "game, 0ae052669ed2310fc1e27686bcdbfa17, 200",
            "record, cb498eeac61daed6daa9d07f50992e50, 200",
            "manga, 3bb6e5d8214deabbcae884b9ae250632, 200",
    })
    public void testDownloadAPI(String type, String id, int status) {
        given()
                .queryParam("type", type)
                .queryParam("id", id)
//                .get("download?type=" + type + "&id=" + id)
                .when()
                .get("download")
                .then().statusCode(status)
                .contentType(containsString("application/octet-stream"));
    }

    @ParameterizedTest
    @CsvSource({
            "game, 3bb6e5d8214deabbcae884b9ae250630, 400",
            "record, 3bb6e5d8214deabbcae884b9ae250630, 400",
            "manga, 3bb6e5d8214deabbcae884b9ae250630, 400",
            "random, 0ae052669ed2310fc1e27686bcdbfa17, 200" // undefined
    })
    public void testDownloadAPI2(String type, String id, int status) {
        given()
                .queryParam("type", type)
                .queryParam("id", id)
                .when()
//                .get("download?type=" + type + "&id=" + id)
                .then().statusCode(greaterThanOrEqualTo(status));
    }

    // only download preview picture
    // /download?type=game&id=some_hash&preview=true
    // content-type: image/png(for game/manga) or image/jpg(for record/undefined type)
    // when the type is invalid, it should return a default image meta.jpg
    @ParameterizedTest
    @CsvSource({
            "game, 0ae052669ed2310fc1e27686bcdbfa17, 200, image/png",
            "record, cb498eeac61daed6daa9d07f50992e50, 200, image/jpg",
            "manga, 3bb6e5d8214deabbcae884b9ae250632, 200, image/png",
            "random, 0ae052669ed2310fc1e27686bcdbfa17, 200, image/jpg" // undefined
    })
    public void testDownloadAPI3(String type, String id, int status, String contentType) {
        given().
                queryParam("type", type)
                .queryParam("id", id)
                .queryParam("preview", "true")
                .when()
                .get("download")
                .then().statusCode(status)
                .contentType(containsString(contentType));
    }

    // only download preview picture
    // but with not valid id, should return default image meta.jpg
    // why failure: when return the default image, the path is written as "/img/meta.jpg"
    // which should be "/meta.jpg"
    @ParameterizedTest
    @CsvSource({
            "game, 3bb6e5d8214deabbcae884b9ae250630, 200",
            "record, 3bb6e5d8214deabbcae884b9ae250630, 200",
            "manga, 3bb6e5d8214deabbcae884b9ae250630, 200",
            "random, 0ae052669ed2310fc1e27686bcdbfa17, 200" // undefined
    })
    public void testDownloadAPI4(String type, String id, int status) {
        given()
                .queryParam("type", type)
                .queryParam("id", id)
                .queryParam("preview", "true")
                .when()
                .get("download")
                .then()
                .statusCode(status)
                .contentType(containsString("image/jpg"));
    }

    @ParameterizedTest
    @CsvSource({
            "games, Ikachan, Pixel",
            "records, Happy, Ochiru",
            "comics, 2012, Andy"
    })
    public void testJsonAPI(String type, String name, String creator) {
        given()
                .queryParam("format", "json")
                .queryParam("name", name)
                .queryParam("creator", creator)
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .body("totalitems", greaterThan(0));
    }

    @ParameterizedTest
    @CsvSource({
            "games, Ikachandsfs, Pixeleqweq",
            "records, Happyfsdsfe, Ochirueqeqw",
            "comics, 2012eqewq, Andyeqwwqqe",
    })
    public void testJsonAPI2(String type, String name, String creator) {
        given()
                .queryParam("format", "json")
                .queryParam("name", name)
                .queryParam("creator", creator)
                .when()
                .get(type)
                .then()
                .statusCode(200)
                .body("totalitems", equalTo(0));
    }

    @Test
    public void testSurveysAPI() {
        when().get("surveys").then()
                .statusCode(200)
                .body(containsString("Surveys")); // just a simple check
    }

    // actually it is a faq page
    @Test
    public void testAboutAPI() {
        when().get("about").then()
                .statusCode(200)
                .body(containsString("Questions")); // just a simple check
    }

    // actually a welcome page
    @Test
    public void testHomeAPI() {
        when().get("home").then()
                .statusCode(200)
                .body(containsString("DoujinSoft Store")); // just a simple check
    }




}
