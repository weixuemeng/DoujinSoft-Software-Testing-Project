package com.difegue.doujinsoft;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class TestGUI {

    @TempDir
    Path tempDir;

    WebDriver driver;

    @BeforeEach
    void setUp() {
        HashMap<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", tempDir.toString());
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);
        driver = new ChromeDriver(options);
    }

    @AfterEach
    void tearDown() {
        driver.quit();
        driver = null;
    }

    @ParameterizedTest
    @ValueSource(strings = {"games", "records", "comics"})
    public void testDownload(String type) {

        driver.get("https://diy.tvc-16.science/" + type);
        List<WebElement> downloadButtons = driver.findElements(By.xpath("//a[@data-tooltip='Download']"));
        Assertions.assertFalse(downloadButtons.isEmpty());
        downloadButtons.get(0).click();
        File[] files = new File(tempDir.toString()).listFiles();

        Assertions.assertTrue(files != null && files.length > 0);
    }

    // This is a faq page
    @Test
    public void testAboutPage() {
        driver.get("https://diy.tvc-16.science/about");
        Assertions.assertTrue(Objects.requireNonNull(driver.getPageSource()).contains("Questions"));
    }

    @Test
    public void testIndexPage() {
        driver.get("https://diy.tvc-16.science/");
        Assertions.assertTrue(Objects.requireNonNull(driver.getPageSource()).contains("DoujinSoft Store"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"games", "records", "comics"})
    public void testCart(String type) {
        driver.get("https://diy.tvc-16.science/" + type);
        WebElement cartNumElement = driver.findElement(By.xpath("/html/body/div[1]/nav/div/ul[1]/li[4]/a/span/b"));
        String cartNum1 = cartNumElement.getText();
        Assertions.assertEquals("0", cartNum1);
        List<WebElement> addToCartButton = driver.findElements(By.xpath("//a[@data-tooltip='Add to cart']"));
        Assertions.assertFalse(addToCartButton.isEmpty());
        addToCartButton.get(0).click();
        WebElement cartNumElement2 = driver.findElement(By.xpath("/html/body/div[1]/nav/div/ul[1]/li[4]/a/span/b"));
        String cartNum2 = cartNumElement2.getText();
        Assertions.assertEquals("1", cartNum2);
    }

    //Get sharing link
    @ParameterizedTest
    @ValueSource(strings = {"games", "records", "comics"})
    public void testSharing(String type) throws InterruptedException {
        driver.get("https://diy.tvc-16.science/" + type);
        List<WebElement> sharingButtons = driver.findElements(By.xpath("//a[@data-tooltip='Get sharing link']"));
        Assertions.assertFalse(sharingButtons.isEmpty());
        sharingButtons.get(0).click();
        Thread.sleep(1000); // for demo, I could use js to get clipboard content but that needs manual permission confirmation
    }

    @ParameterizedTest
    @CsvSource({
            "games, JON+",
            "records, Ben",
            "comics, Spencer",
    })
    public void testSearchingCreatorName(String type, String creatorName) throws InterruptedException {
        driver.get("https://diy.tvc-16.science/" + type);
        WebElement creatorInput = driver.findElement(By.xpath("//*[@id=\"maker_name\"]"));
        creatorInput.sendKeys(creatorName);
        WebElement searchButton = driver.findElement(By.xpath("//*[@id=\"game-search\"]/a[1]"));
        searchButton.click();
        WebElement contentList = driver.findElement(By.xpath("//*[@id=\"content\"]"));
        Thread.sleep(1000); // wait for the page to load
        String notFoundText = "No more "+ type +".\n" + "Sad.";
        Assertions.assertFalse(contentList.getText().contains(notFoundText));
    }

    @ParameterizedTest
    @CsvSource({
            "games, Tennis Pro",
            "records, 0apple!",
            "comics, 2 Cats",
    })
    public void testSearchingWorkName(String type, String creatorName) throws InterruptedException {
        driver.get("https://diy.tvc-16.science/" + type);
        WebElement workInput = driver.findElement(By.xpath("//*[@id=\"item_name\"]"));
        workInput.sendKeys(creatorName);
        WebElement searchButton = driver.findElement(By.xpath("//*[@id=\"game-search\"]/a[1]"));
        searchButton.click();
        WebElement contentList = driver.findElement(By.xpath("//*[@id=\"content\"]"));
        Thread.sleep(1000); // wait for the page to load
        String notFoundText = "No more "+ type +".\n" + "Sad.";
        Assertions.assertFalse(contentList.getText().contains(notFoundText));
    }

    @ParameterizedTest
    @CsvSource({
            "games, JON+xxxxxxxxxxx",
            "records, Benxxxxxxxxxxxxx",
            "comics, Spencexxxxxxxxxxxxxxxxxxr",
    })
    public void testSearchingCreatorNameWithNotValidName(String type, String creatorName) throws InterruptedException {
        driver.get("https://diy.tvc-16.science/" + type);
        WebElement creatorInput = driver.findElement(By.xpath("//*[@id=\"maker_name\"]"));
        creatorInput.sendKeys(creatorName);
        WebElement searchButton = driver.findElement(By.xpath("//*[@id=\"game-search\"]/a[1]"));
        searchButton.click();
        WebElement contentList = driver.findElement(By.xpath("//*[@id=\"content\"]"));
        Thread.sleep(1000); // wait for the page to load
        String notFoundText = "No more "+ type +".\n" + "Sad.";
        Assertions.assertTrue(contentList.getText().contains(notFoundText));
    }

    @ParameterizedTest
    @CsvSource({
            "games, Tennis Proaaaaaaaaaaaaaaaa",
            "records, 0apple!aaaaaaaaaaaaaaaaaaa",
            "comics, 2 Catsaaaaaaaaaaaaaaaaaa",
    })
    public void testSearchingWorkNameWithNotValidName(String type, String creatorName) throws InterruptedException {
        driver.get("https://diy.tvc-16.science/" + type);
        WebElement workInput = driver.findElement(By.xpath("//*[@id=\"item_name\"]"));
        workInput.sendKeys(creatorName);
        WebElement searchButton = driver.findElement(By.xpath("//*[@id=\"game-search\"]/a[1]"));
        searchButton.click();
        WebElement contentList = driver.findElement(By.xpath("//*[@id=\"content\"]"));
        Thread.sleep(1000); // wait for the page to load
        String notFoundText = "No more "+ type +".\n" + "Sad.";
        Assertions.assertTrue(contentList.getText().contains(notFoundText));
    }

    @ParameterizedTest
    @CsvSource({
            "games, JON+, Tennis Pro",
            "records, Ben, 8Bit Melody",
            "comics, Spencer, 2 Cats",
    })
    public void testSearchingCreatorNameAndWorkName(String type, String creatorName, String workName) throws InterruptedException {
        driver.get("https://diy.tvc-16.science/" + type);
        WebElement creatorInput = driver.findElement(By.xpath("//*[@id=\"maker_name\"]"));
        creatorInput.sendKeys(creatorName);
        WebElement workInput = driver.findElement(By.xpath("//*[@id=\"item_name\"]"));
        workInput.sendKeys(workName);
        WebElement searchButton = driver.findElement(By.xpath("//*[@id=\"game-search\"]/a[1]"));
        searchButton.click();
        WebElement contentList = driver.findElement(By.xpath("//*[@id=\"content\"]"));
        Thread.sleep(1000); // wait for the page to load
        String notFoundText = "No more "+ type +".\n" + "Sad.";
        Assertions.assertFalse(contentList.getText().contains(notFoundText));
    }

    @ParameterizedTest
    @CsvSource({
            "games, JON+aaa, Tennis Pro",
            "records, Benaaa, 8Bit Melody",
            "comics, Spenceraaa, 2 Cats",
    })
    public void testSearchingCreatorNameAndWorkNameWithNotValidName(String type, String creatorName, String workName) throws InterruptedException {
        driver.get("https://diy.tvc-16.science/" + type);
        WebElement creatorInput = driver.findElement(By.xpath("//*[@id=\"maker_name\"]"));
        creatorInput.sendKeys(creatorName);
        WebElement workInput = driver.findElement(By.xpath("//*[@id=\"item_name\"]"));
        workInput.sendKeys(workName);
        WebElement searchButton = driver.findElement(By.xpath("//*[@id=\"game-search\"]/a[1]"));
        searchButton.click();
        WebElement contentList = driver.findElement(By.xpath("//*[@id=\"content\"]"));
        Thread.sleep(1000); // wait for the page to load
        String notFoundText = "No more "+ type +".\n" + "Sad.";
        Assertions.assertTrue(contentList.getText().contains(notFoundText));
    }

    @ParameterizedTest
    @CsvSource({
            "games, JON+, Tennis Proaaa",
            "records, Ben, 8Bit Melodaaa",
            "comics, Spencer, 2 Catsaaa",
    })
    public void testSearchingCreatorNameAndWorkNameWithNotValidName2(String type, String creatorName, String workName) throws InterruptedException {
        driver.get("https://diy.tvc-16.science/" + type);
        WebElement creatorInput = driver.findElement(By.xpath("//*[@id=\"maker_name\"]"));
        creatorInput.sendKeys(creatorName);
        WebElement workInput = driver.findElement(By.xpath("//*[@id=\"item_name\"]"));
        workInput.sendKeys(workName);
        WebElement searchButton = driver.findElement(By.xpath("//*[@id=\"game-search\"]/a[1]"));
        searchButton.click();
        WebElement contentList = driver.findElement(By.xpath("//*[@id=\"content\"]"));
        Thread.sleep(1000); // wait for the page to load
        String notFoundText = "No more "+ type +".\n" + "Sad.";
        Assertions.assertTrue(contentList.getText().contains(notFoundText));
    }

    @ParameterizedTest
    @CsvSource({
            "games, JON+aaa, Tennis Proaaa",
            "records, Benaaa, 8Bit Melodaaa",
            "comics, Spenceraaa, 2 Catsaaa",
    })
    public void testSearchingCreatorNameAndWorkNameWithNotValidName3(String type, String creatorName, String workName) throws InterruptedException {
        driver.get("https://diy.tvc-16.science/" + type);
        WebElement creatorInput = driver.findElement(By.xpath("//*[@id=\"maker_name\"]"));
        creatorInput.sendKeys(creatorName);
        WebElement workInput = driver.findElement(By.xpath("//*[@id=\"item_name\"]"));
        workInput.sendKeys(workName);
        WebElement searchButton = driver.findElement(By.xpath("//*[@id=\"game-search\"]/a[1]"));
        searchButton.click();
        WebElement contentList = driver.findElement(By.xpath("//*[@id=\"content\"]"));
        Thread.sleep(1000); // wait for the page to load
        String notFoundText = "No more "+ type +".\n" + "Sad.";
        Assertions.assertTrue(contentList.getText().contains(notFoundText));
    }
}
