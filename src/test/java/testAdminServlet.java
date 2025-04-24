package test.java;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static io.restassured.RestAssured.*;
import static io.restassured.config.RedirectConfig.redirectConfig;
import static org.hamcrest.Matchers.*;

public class testAdminServlet {
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "VO£w790LL3[D(ijxM|GN";
    final String BASE_URL   = "https://"+ ADMIN_USERNAME +":"+ ADMIN_PASSWORD +"@doujinsoft.yihang.one/manage";

    WebDriver webDriver;
    @BeforeAll
    static void beforeAll() {
        // THIS MUST RUN FIRST to download & configure the right chromedriver
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        webDriver = new ChromeDriver(options);
    }

    @AfterEach
    void tearDown() {
        webDriver.quit();
        webDriver = null;
    }

    @Test
        void postNewCollectionOnServer() {
            // Change these to values your remote test instance will accept
        webDriver.get(BASE_URL);
//            given()
//                    .auth().preemptive().basic(ADMIN_USERNAME, ADMIN_PASSWORD)
//                    .formParam("collection_type", "game")
//                    .formParam("collection_id",   "test1")
//                    .formParam("collection_name", "game-name")
//                    .formParam("collection_color","#fff")
//                    .formParam("collection_icon", "icon.png")
//                    .formParam("background_pic",  "background.png")
//                    .formParam("collection_desc", "des")
//                    .formParam("collection_desc2","des2")
//                    .when()
//                    .post()
//                    .then()
//                    .statusCode(200)
//                    .body(containsString("Collection created at"));
        }


}
