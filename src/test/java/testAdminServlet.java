package test.java;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class testAdminServlet {
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "VO£w790LL3[D(ijxM|GN";
    final String BASE_URL   = "https://"+ ADMIN_USERNAME +":"+ ADMIN_PASSWORD +"@doujinsoft.yihang.one/manage";

    WebDriver webDriver;

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
        void postAdminShowsAdmin() {
        webDriver.get(BASE_URL);
        WebElement header = webDriver.findElement(By.cssSelector("h1.header.center.amber-text"));
        // assert its text is exactly "Admin"
        assertEquals("Admin", header.getText().trim());
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
    @Test
    void testPostValidCollection(){
        webDriver.get(BASE_URL);
        WebElement collectionType = webDriver.findElement(By.id("collection_type"));
        WebElement collectionId = webDriver.findElement(By.id("collection_id"));
        WebElement collectionName = webDriver.findElement(By.id("collection_name"));
        WebElement collectionColor = webDriver.findElement(By.id("collection_color"));
        WebElement collectionIcon = webDriver.findElement(By.id("collection_icon"));
        WebElement backgroundPic = webDriver.findElement(By.id("background_pic"));
        WebElement backgroundDesc = webDriver.findElement(By.id("collection_desc"));
        WebElement backgroundDesc2 = webDriver.findElement(By.id("collection_desc2"));
        String id = "1";

        collectionType.sendKeys("game1");
        collectionId.sendKeys(id);
        collectionName.sendKeys("collection1");
        collectionColor.sendKeys("#FFF");
        collectionIcon.sendKeys("");
        backgroundPic.sendKeys("");
        backgroundDesc.sendKeys("");
        backgroundDesc2.sendKeys("");

        WebElement submitButton = webDriver.findElement(By.xpath("/html/body/main/div/div/div[4]/form[1]/input"));
        submitButton.click();

        String body = webDriver.findElement(By.tagName("body")).getText();
        System.out.println(body);
        assertTrue(body.equals( String.format("Collection created at /home/doujinsoft/collections/%s.json", id)));

    }

    @Test
    void testPostInValidCollection(){
        webDriver.get(BASE_URL);
        WebElement collectionType = webDriver.findElement(By.id("collection_type"));
        WebElement collectionId = webDriver.findElement(By.id("collection_id"));
        WebElement collectionName = webDriver.findElement(By.id("collection_name"));
        WebElement collectionColor = webDriver.findElement(By.id("collection_color"));
        WebElement collectionIcon = webDriver.findElement(By.id("collection_icon"));
        WebElement backgroundPic = webDriver.findElement(By.id("background_pic"));
        WebElement backgroundDesc = webDriver.findElement(By.id("collection_desc"));
        WebElement backgroundDesc2 = webDriver.findElement(By.id("collection_desc2"));
        String id = "";

        collectionType.sendKeys("");
        collectionId.sendKeys(id);
        collectionName.sendKeys("");
        collectionColor.sendKeys("");
        collectionIcon.sendKeys("");
        backgroundPic.sendKeys("");
        backgroundDesc.sendKeys("");
        backgroundDesc2.sendKeys("");

        WebElement submitButton = webDriver.findElement(By.xpath("/html/body/main/div/div/div[4]/form[1]/input"));
        submitButton.click();
        new WebDriverWait(webDriver, Duration.ofSeconds(5))
                .until((ExpectedCondition<Boolean>) wd ->
                        !wd.findElement(By.tagName("body")).getText().isEmpty()
                );

        String body = webDriver.findElement(By.tagName("body")).getText().toLowerCase();
        assertTrue(body.contains("error"), "Expected an alert for valid input, but page body was:\n" + body);

    }


}
