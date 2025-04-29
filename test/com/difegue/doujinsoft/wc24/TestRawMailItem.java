package com.difegue.doujinsoft.wc24;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

public class TestRawMailItem {

    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    public void testRenderStringWithLegitWilCode() throws Exception {
        String wilCode = "1234567890123456";
        String raw = "raw";
        RawMailItem rmi = new RawMailItem(wilCode, raw);
        Assertions.assertEquals(raw, rmi.renderString("path"));
    }

    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    public void testRenderStringWithNotLegitWilCode() {
        String wilCode = "123456"; // not legit (length < 16)
        String raw = "raw";
        Assertions.assertThrows(Exception.class, () -> new RawMailItem(wilCode, raw));
    }

    @Test
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    public void testRenderStringWithNoWiiNumber() {
        String wilCode = "1234567890123456";
        String raw = "raw";
        Assertions.assertThrows(Exception.class, () -> new RawMailItem(wilCode, raw));
    }

    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    public void testRenderStringWithNoServerUrl() {
        String wilCode = "1234567890123456";
        String raw = "raw";
        Assertions.assertThrows(Exception.class, () -> new RawMailItem(wilCode, raw));
    }
}
