package com.difegue.doujinsoft.utils;

import com.difegue.doujinsoft.utils.MioUtils.Types;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


//  1. GET – first 50
//  2. POST – page=1  → OFFSET 0
//  3. POST – page=3  → OFFSET 100
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class TestTemplateBuilderSurvey {
    private MockedStatic<DriverManager> dm;
    private Connection        conn;
    private Statement         st;
    private PreparedStatement ps;
    private ResultSet         rs;
    private ServletContext    ctx;
    private HttpServletRequest req;
    private Path              tpl;
    @BeforeEach void setUp() throws Exception {
        conn = mock(Connection.class);
        st   = mock(Statement.class);
        ps   = mock(PreparedStatement.class);
        rs   = mock(ResultSet.class);
        when(conn.createStatement()).thenReturn(st);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(st.executeQuery(anyString())).thenReturn(rs);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        when(rs.getInt(anyInt())).thenReturn(123);
        when(rs.getString(any(String.class))).thenReturn("");
        when(rs.getString(anyInt())).thenReturn("");
        dm = mockStatic(DriverManager.class);
        dm.when(() -> DriverManager.getConnection(anyString())).thenReturn(conn);
        ctx = mock(ServletContext.class);
        when(ctx.getInitParameter("dataDirectory")).thenReturn("/tmp");
        tpl = Files.createTempFile("survey", ".html");
        Files.writeString(tpl, "survey template");
        when(ctx.getRealPath(anyString())).thenReturn(tpl.toString());
        req = mock(HttpServletRequest.class);
        when(req.getParameter(anyString())).thenReturn("");
        when(req.getParameterMap()).thenReturn(java.util.Collections.emptyMap());
    }

    @AfterEach void tearDown() throws Exception {
        dm.close();
        Files.deleteIfExists(tpl);
    }

    // whitebox 1. Get
    @Test void getSurveys_default() throws Exception {
        when(rs.next()).thenReturn(true, false);
        String html = new TemplateBuilderSurvey(ctx, req)
                .doGetSurveys();
        Assertions.assertTrue(html.contains("survey template"));
        verify(st).executeQuery(
                contains("ORDER BY timestamp DESC LIMIT 50"));
        verify(st, times(2)).executeQuery(anyString());
    }

    // whitebox 2 Post
    @Test void post_pageDefault_offset0() throws Exception {
        new TemplateBuilderSurvey(ctx, req).doPostSurveys();
        verify(ps).setInt(eq(1), eq(0));
        verify(conn).prepareStatement(
                contains("ORDER BY timestamp DESC LIMIT 50 OFFSET ?"));
    }

    // whitebox 3 Post
    @Test void post_page3_offset100() throws Exception {
        when(req.getParameterMap()).thenReturn(
                java.util.Map.of("page", new String[]{"3"}));
        when(req.getParameter("page")).thenReturn("3");

        new TemplateBuilderSurvey(ctx, req).doPostSurveys();

        verify(ps).setInt(eq(1), eq(100));
    }


    @Test
    void post_page3_withResults() throws Exception {
        when(req.getParameterMap())
                .thenReturn(Map.of("page", new String[]{"3"}));
        when(req.getParameter("page")).thenReturn("3");
        when(rs.next()).thenReturn(true, false);
        new TemplateBuilderSurvey(ctx, req).doPostSurveys();
        verify(ps).setInt(eq(1), eq(100));
        verify(rs, atLeastOnce()).next();
    }

}
