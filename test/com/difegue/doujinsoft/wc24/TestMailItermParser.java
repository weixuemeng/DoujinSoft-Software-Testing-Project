package com.difegue.doujinsoft.wc24;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.servlet.ServletContext;

import java.nio.file.Path;

import static org.mockito.Mockito.when;

public class TestMailItermParser {

//    public static String getMailData(String delimiter) {
//        String mailData = delimiter +"\n"
//                + "Content-Type: text/plain\n"
//                + ""
//    }


    @TempDir
    Path tempDir;

    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "https://test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "123456")
    public void testMailItemParserWithFallBackNumber() {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn("path"); // here does not need a real value
        Assertions.assertDoesNotThrow(() -> new MailItemParser(application));
    }

    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "https://test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
//    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "123456")
    public void testMailItemParserWithNoFallBackNumber() {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn("path");
//        Assertions.assertDoesNotThrow(() -> new MailItemParser(application));
        Assertions.assertThrows(Exception.class, () -> new MailItemParser(application));
    }

    // not finished
    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "https://test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "123456")
    public void testConsumeEmails() throws Exception {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn(tempDir.toString());
        MailItemParser mailItemParser = new MailItemParser(application);
//        Assertions.assertDoesNotThrow(() -> mailItemParser.consumeEmails());
    }

}
