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

/**
 * 覆盖 TemplateBuilderSurvey：
 * 1. GET – 默认取前 50 条
 * 2. POST – page=1 (缺省) → OFFSET 0
 * 3. POST – page=3        → OFFSET 100
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class TestTemplateBuilderSurvey {

    /* ---------------- 通用 mock ---------------- */
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

        /* Statement / PreparedStatement 基本行为 */
        when(conn.createStatement()).thenReturn(st);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(st.executeQuery(anyString())).thenReturn(rs);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);            // 个别用例再覆盖
        when(rs.getInt(anyInt())).thenReturn(123);    // totalitems 用
        when(rs.getString(any(String.class))).thenReturn("");  // <-- 新增
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

    /* ========== 1. GET 逻辑：取前 50 条 ========== */
    @Test void getSurveys_default() throws Exception {
        // 让 while(result.next()) 进入一次
        when(rs.next()).thenReturn(true, false);

        String html = new TemplateBuilderSurvey(ctx, req)
                .doGetSurveys();

        Assertions.assertTrue(html.contains("survey template"));
        verify(st).executeQuery(
                contains("ORDER BY timestamp DESC LIMIT 50"));
        // 再次查询 COUNT(*)：
        verify(st, times(2)).executeQuery(anyString());
    }

    /* ========== 2. POST – page 缺省 (=1) ⇒ OFFSET 0 ========== */
    @Test void post_pageDefault_offset0() throws Exception {
        new TemplateBuilderSurvey(ctx, req).doPostSurveys();

        verify(ps).setInt(eq(1), eq(0));          // 50*(1)-50
        verify(conn).prepareStatement(
                contains("ORDER BY timestamp DESC LIMIT 50 OFFSET ?"));
    }

    /* ========== 3. POST – page=3 ⇒ OFFSET 100 ========== */
    @Test void post_page3_offset100() throws Exception {
        when(req.getParameterMap()).thenReturn(
                java.util.Map.of("page", new String[]{"3"}));
        when(req.getParameter("page")).thenReturn("3");

        new TemplateBuilderSurvey(ctx, req).doPostSurveys();

        verify(ps).setInt(eq(1), eq(100));        // 50*(3)-50
    }

    /** page=3 且有数据 —— 覆盖
     *   ① if ( … containsKey("page") && !isEmpty() )
     *   ② while ( result.next() )
     */
    @Test
    void post_page3_withResults() throws Exception {

        // ----------- 模拟 request body -----------
        when(req.getParameterMap())
                .thenReturn(Map.of("page", new String[]{"3"}));
        when(req.getParameter("page")).thenReturn("3");

        // ----------- 让查询真的跑出 1 条记录 -----------
        when(rs.next()).thenReturn(true, false);   // → while() 进入一次

        // —— 调用 —— //
        new TemplateBuilderSurvey(ctx, req).doPostSurveys();

        // ----------- 断言 -----------
        // page = 3  ⇒ OFFSET  (3 * 50 - 50) = 100
        verify(ps).setInt(eq(1), eq(100));

        // 至少循环过一次
        verify(rs, atLeastOnce()).next();
    }

}
