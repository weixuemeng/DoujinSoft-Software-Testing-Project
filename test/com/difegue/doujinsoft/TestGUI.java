package com.difegue.doujinsoft;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
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

    @Test
    public void testAboutPage() {
        driver.get("https://diy.tvc-16.science/about");
    }
}
