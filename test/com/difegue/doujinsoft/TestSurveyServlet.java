package com.difegue.doujinsoft;

import com.difegue.doujinsoft.utils.TemplateBuilderSurvey;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TestSurveyServlet {

    private SurveyServlet servlet;
    private HttpServletRequest  req;
    private HttpServletResponse resp;
    private ServletContext      ctx;
    private StringWriter        body;

    @BeforeEach
    void setUp() throws Exception {
        servlet = spy(new SurveyServlet());
        req  = mock(HttpServletRequest.class);
        resp = mock(HttpServletResponse.class);
//        ctx  = mock(ServletContext.class);
//        when(req.getServletContext()).thenReturn(ctx);
//        when(servlet.getServletConfig()).thenReturn(null);
//        when(servlet.getServletContext()).thenReturn(ctx);
        ServletConfig cfg = mock(ServletConfig.class);
        when(cfg.getServletContext()).thenReturn(ctx);
        doReturn(cfg).when(servlet).getServletConfig();

        body = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(body));
    }

    @Test
    void doGet_ok() throws Exception {
        try (MockedConstruction<TemplateBuilderSurvey> ctor =
                     mockConstruction(TemplateBuilderSurvey.class,
                             (mock, c) -> when(mock.doGetSurveys()).thenReturn("HTML"))) {

            servlet.doGet(req, resp);
            Assertions.assertEquals("HTML", body.toString());
        }
    }

    //  TemplateBuilderSurvey
    @Test
    void doGet_exception() throws Exception {
        try (MockedConstruction<TemplateBuilderSurvey> ctor =
                     mockConstruction(TemplateBuilderSurvey.class,
                             (mock, c) -> when(mock.doGetSurveys()).thenThrow(new RuntimeException("boom")))) {
            servlet.doGet(req, resp);
            Assertions.assertTrue(body.toString().isEmpty());
        }
    }

    //  doPost
    @Test
    void doPost_noParams() throws Exception {
        when(req.getParameterMap()).thenReturn(Collections.emptyMap());

        servlet.doPost(req, resp);
        Assertions.assertEquals("Who are you running from?", body.toString());
    }

    // doPost
    @Test
    void doPost_withParams() throws Exception {
        when(req.getParameterMap()).thenReturn(Map.of("page", new String[]{"2"}));
        try (MockedConstruction<TemplateBuilderSurvey> ctor =
                     mockConstruction(TemplateBuilderSurvey.class,
                             (mock, c) -> when(mock.doPostSurveys()).thenReturn("JSON"))) {
            servlet.doPost(req, resp);
            Assertions.assertEquals("JSON", body.toString());
            TemplateBuilderSurvey built = ctor.constructed().get(0);
            verify(built).doPostSurveys();
        }
    }
}
