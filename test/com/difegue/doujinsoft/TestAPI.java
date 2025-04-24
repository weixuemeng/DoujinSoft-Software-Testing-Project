package com.difegue.doujinsoft;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

public class TestAPI {

    @BeforeAll
    static void setUpBeforeClass() {
        RestAssured.baseURI = "https://diy.tvc-16.science/";
    }

    @ParameterizedTest
    @CsvSource({
            "game, 0ae052669ed2310fc1e27686bcdbfa17, 200",
            "record, cb498eeac61daed6daa9d07f50992e50, 200",
            "manga, 3bb6e5d8214deabbcae884b9ae250632, 200",
    })
    public void testDownloadAPI(String type, String id, int status) {
        when().get("download?type="+type+"&id="+id).then().statusCode(status);
    }

    @ParameterizedTest
    @CsvSource({
            "game, 3bb6e5d8214deabbcae884b9ae250630, 400",
            "record, 3bb6e5d8214deabbcae884b9ae250630, 400",
            "manga, 3bb6e5d8214deabbcae884b9ae250630, 400",
            "random, 0ae052669ed2310fc1e27686bcdbfa17, 400" // undefined
    })
    public void testDownloadAPI2(String type, String id, int status) {
        when().get("download?type="+type+"&id="+id).then().statusCode(greaterThanOrEqualTo(status));
    }

    @ParameterizedTest
    @CsvSource({
            "games, Ikachan, Pixel",
            "records, Happy, Ochiru",
            "comics, 2012, Andy"
    })
    public void testSearchAPI(String type, String name, String creator) {
        when().get(type+"?format=json&name="+name+"&creator="+creator)
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
    public void testSearchAPI2(String type, String name, String creator) {
        when().get(type+"?format=json&name="+name+"&creator="+creator)
                .then()
                .statusCode(200)
                .body("totalitems", equalTo(0));
    }



}
