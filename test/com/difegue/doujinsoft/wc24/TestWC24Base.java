package com.difegue.doujinsoft.wc24;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;
import javax.servlet.ServletContext;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TestWC24Base {
//    helpers
    private final Map<String, String> realEnv = new HashMap<>(System.getenv());
    @SuppressWarnings("unchecked")
    private static void setEnv(Map<String, String> newEnv) throws Exception {
        Class<?> pe = Class.forName("java.lang.ProcessEnvironment");
        for (String fieldName : List.of("theEnvironment", "theCaseInsensitiveEnvironment")) {
            try {
                Field f = pe.getDeclaredField(fieldName);
                f.setAccessible(true);
                Map<String, String> mod = (Map<String, String>) f.get(null);
                mod.clear();
                mod.putAll(newEnv);
            } catch (NoSuchFieldException ignore) {
                Field f = System.getenv().getClass().getDeclaredField("m");
                f.setAccessible(true);
                Map<String, String> mod = (Map<String, String>) f.get(System.getenv());
                mod.clear();
                mod.putAll(newEnv);
            }
        }
    }

    private ServletContext ctx;
    @BeforeEach void setup() {
        ctx = Mockito.mock(ServletContext.class);
    }
    @AfterEach void restoreEnv() throws Exception {
        setEnv(realEnv);
    }

    @Test
    @Tag("all_vars_present_debug_flag_absent_false")
    void ctor_allVars_noDebug() throws Exception {
        Map<String,String> env = Map.of(
                "WII_NUMBER",    "1234567890123456",
                "WC24_SERVER",   "https://wc24.example",
                "WC24_PASSWORD", "pwd"
        );
        setEnv(env);
        WC24Base base = new WC24Base(ctx);
        assertEquals("1234567890123456", base.sender);
        assertFalse(base.debugLogging);
    }

    @Test
    @Tag("all_vars_WC24_DEBUG_true")
    void ctor_allVars_debugTrue() throws Exception {
        Map<String,String> env = Map.of(
                "WII_NUMBER",    "1111222233334444",
                "WC24_SERVER",   "http://localhost",
                "WC24_PASSWORD", "pw",
                "WC24_DEBUG",    "true"
        );
        setEnv(env);
        WC24Base base = new WC24Base(ctx);
        assertTrue(base.debugLogging);
        assertEquals("http://localhost", base.wc24Server);
    }


//    failure paths
    @Test @Tag("missing_WII_NUMBER")
    void ctor_missingWiiNumber() throws Exception {
        Map<String,String> env = Map.of(
                "WC24_SERVER",   "x",
                "WC24_PASSWORD", "y"
        );
        setEnv(env);
        Exception ex = assertThrows(Exception.class, () -> new WC24Base(ctx));
        assertTrue(ex.getMessage().contains("Wii sender friend number"));
    }

    @Test @Tag("missing_WC24_SERVER")
    void ctor_missingServer() throws Exception {
        Map<String,String> env = Map.of(
                "WII_NUMBER",    "123",
                "WC24_PASSWORD", "pwd"
        );
        setEnv(env);
        Exception ex = assertThrows(Exception.class, () -> new WC24Base(ctx));
        assertTrue(ex.getMessage().contains("WiiConnect24 server url"));
    }

    @Test @Tag("missing_WC24_PASSWORD")
    void ctor_missingPassword() throws Exception {
        Map<String,String> env = Map.of(
                "WII_NUMBER",  "123",
                "WC24_SERVER", "s"
        );
        setEnv(env);
        Exception ex = assertThrows(Exception.class, () -> new WC24Base(ctx));
        assertTrue(ex.getMessage().contains("WiiConnect24 account password"));
    }
}
