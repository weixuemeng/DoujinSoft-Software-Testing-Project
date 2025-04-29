package com.difegue.doujinsoft;

import com.difegue.doujinsoft.wc24.MailItem;
import com.difegue.doujinsoft.wc24.WiiConnect24Api;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import javax.servlet.ServletContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.difegue.doujinsoft.wc24.TestWiiConnect24Api.putEnv;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestWC24FriendServlet {

    private WC24FriendServlet servlet;
    private ServletContext    ctx;
    private HttpServletRequest  req;
    private HttpServletResponse resp;
    private ByteArrayOutputStream bodyOut;

    @BeforeEach
    void setUp() throws Exception {
        // servlet config
        ctx = mock(ServletContext.class);
        ServletConfig cfg = mock(ServletConfig.class);
        when(cfg.getServletContext()).thenReturn(ctx);
        servlet = spy(new WC24FriendServlet());
        doReturn(cfg).when(servlet).getServletConfig();
        req  = mock(HttpServletRequest.class);
        resp = mock(HttpServletResponse.class);
        bodyOut = new ByteArrayOutputStream();
        when(resp.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override public void write(int b) { bodyOut.write(b); }
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(javax.servlet.WriteListener l) { }
        });

    }
    @BeforeAll
    static void initEnv() throws Exception {
        putEnv("WII_NUMBER", "1111222233334444");
        putEnv("WC24_SERVER", "example.com");
        putEnv("WC24_PASSWORD", "hunter2");
        putEnv("WC24_DEBUG", "true");
    }

    @Test
    void noCode_returnsHelp() throws Exception {
        when(req.getParameterMap()).thenReturn(Collections.emptyMap());
        servlet.doGet(req, resp);
        Assertions.assertEquals("Please add a friend code.",
                bodyOut.toString(StandardCharsets.UTF_8));
    }

    @Test
    void happyPath_singleMail() throws Exception {
        when(req.getParameterMap())
                .thenReturn(Map.of("code", new String[]{"1111222233334444"}));
        when(req.getParameter("code")).thenReturn("1111222233334444");
        try (MockedConstruction<WiiConnect24Api> ctor =
                     mockConstruction(WiiConnect24Api.class,
                             (mock, c) -> when(mock.sendMails(anyList())).thenReturn("OK"))) {
            servlet.doGet(req, resp);
            WiiConnect24Api mockApi = ctor.constructed().get(0);
            ArgumentCaptor<List> cap = ArgumentCaptor.forClass(List.class);
            verify(mockApi, times(1)).sendMails(cap.capture());
            MailItem first = ((List<MailItem>) cap.getValue()).get(0);
            Field f = MailItem.class.getDeclaredField("recipient");
            f.setAccessible(true);
            Assertions.assertEquals("1111222233334444", f.get(first));
            Assertions.assertEquals("OK", bodyOut.toString(StandardCharsets.UTF_8));
        }
    }

    @Test
    void wc24Throws_messageBubbled() throws Exception {
        when(req.getParameterMap())
                .thenReturn(Map.of("code", new String[]{"9999888877776666"}));
        when(req.getParameter("code")).thenReturn("9999888877776666");
        try (MockedConstruction<WiiConnect24Api> ctor =
                     mockConstruction(WiiConnect24Api.class,
                             (mock, c) -> when(mock.sendMails(anyList()))
                                     .thenThrow(new RuntimeException("boom")))) {
            servlet.doGet(req, resp);
            Assertions.assertEquals("boom", bodyOut.toString(StandardCharsets.UTF_8));
        }
    }
}
