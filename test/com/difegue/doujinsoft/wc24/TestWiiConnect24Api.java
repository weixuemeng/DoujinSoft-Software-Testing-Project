package com.difegue.doujinsoft.wc24;

import com.mitchellbosecke.pebble.error.PebbleException;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import javax.servlet.ServletContext;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestWiiConnect24Api {
    public ServletContext          ctx;
    public CloseableHttpClient     http;
    public WiiConnect24Api         api;
    public MockedStatic<HttpClients> httpStub;
    @SuppressWarnings("unchecked")
    public static void putEnv(String k, String v) throws Exception {
        Class<?> pe = Class.forName("java.lang.ProcessEnvironment");
        try {
            Field f = pe.getDeclaredField("theCaseInsensitiveEnvironment");
            f.setAccessible(true);
            ((Map<String,String>) f.get(null)).put(k, v);
        } catch (NoSuchFieldException e) {
            Field unMod = pe.getDeclaredField("theUnmodifiableEnvironment");
            unMod.setAccessible(true);
            Map<String,String> m = (Map<String,String>) unMod.get(null);
            Field mod = m.getClass().getDeclaredField("m");
            mod.setAccessible(true);
            ((Map<String,String>) mod.get(m)).put(k, v);
            Field env = pe.getDeclaredField("theEnvironment");
            env.setAccessible(true);
            ((Map<String,String>) env.get(null)).put(k, v);
        }
    }

    @BeforeAll
    void init() throws Exception {
        putEnv("WII_NUMBER",    "1111222233334444");
        putEnv("WC24_SERVER",   "example.com");
        putEnv("WC24_PASSWORD", "hunter2");
        putEnv("WC24_DEBUG",    "true");
        ctx = mock(ServletContext.class);
        when(ctx.getRealPath(anyString()))
                .thenReturn(System.getProperty("java.io.tmpdir"));
        http = mock(CloseableHttpClient.class);
        CloseableHttpResponse resp = mock(CloseableHttpResponse.class);
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenAnswer(i ->
                new ByteArrayInputStream("OK".getBytes(StandardCharsets.UTF_8)));
        when(resp.getEntity()).thenReturn(entity);
        when(http.execute(any(HttpPost.class))).thenReturn(resp);
        httpStub = mockStatic(HttpClients.class);
        httpStub.when(HttpClients::createDefault).thenReturn(http);
        api = new WiiConnect24Api(ctx);
    }

    @AfterAll
    void cleanup() {
        if (httpStub != null) httpStub.close();
    }

    @Test
    void sendMails_happyPath_noChunk() throws Exception {
        MailItem mail = mock(MailItem.class);
        when(mail.renderString(anyString())).thenReturn("MAIL");
        List<MailItem> list = Collections.nCopies(5, mail);
        String rst = api.sendMails(list);
        Assertions.assertTrue(rst.contains("OK"));
        verify(http, atLeastOnce()).execute(any(HttpPost.class));
    }

    @Test
    void sendMails_renderError_gracefullyIgnored() throws Exception {
        MailItem bad = mock(MailItem.class);
        when(bad.renderString(anyString()))
                .thenThrow(new PebbleException(null, "boom"));
        String rst = api.sendMails(List.of(bad));
        verify(http, atLeastOnce()).execute(any(HttpPost.class));
        Assertions.assertTrue(rst.contains("OK"));
    }

    @Test
    void receiveMails_fullCycle() throws Exception {
        try (MockedConstruction<MailItemParser> parser =
                     mockConstruction(MailItemParser.class,
                             (mock, ctx) -> doNothing().when(mock).consumeEmails(anyString()))) {
            String html = api.receiveMails();
            Assertions.assertTrue(html.startsWith("<pre>"));
        }
        verify(http).execute(argThat(p ->
                ((HttpPost) p).getURI().toString().endsWith("receive.cgi")));
    }

    @Test
    void deleteMails_ok() throws Exception {
        api.deleteMails();
        verify(http).execute(argThat(p ->
                ((HttpPost) p).getURI().toString().endsWith("delete.cgi")));
    }
}
