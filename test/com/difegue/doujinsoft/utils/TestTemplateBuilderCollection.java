package com.difegue.doujinsoft.utils;

import com.difegue.doujinsoft.templates.Collection;
import com.difegue.doujinsoft.utils.MioUtils.Types;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class TestTemplateBuilderCollection {
    // general mock
    private MockedStatic<DriverManager> dm;
    private Connection   conn;
    private Statement    st;
    private PreparedStatement ps;
    private ResultSet    rs;
    private ServletContext ctx;
    private HttpServletRequest req;
    private Path template;

    private static class FakeCollection extends Collection {
        private final String mioSql;
        private final int    type;
        FakeCollection(String sql, int t, String[] hashes) {
            this.mioSql = sql;
            this.type   = t;
            this.mios   = hashes;
            this.collection_type = switch (t) {
                case Types.GAME   -> "game";
                case Types.MANGA  -> "manga";
                case Types.RECORD -> "record";
                default           -> "game";
            };
            this.collection_name = "Demo";
        }
        @Override public String getMioSQL() { return mioSql; }
        @Override public int    getType()   { return type;   }
    }

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
        dm = mockStatic(DriverManager.class);
        dm.when(() -> DriverManager.getConnection(anyString())).thenReturn(conn);
        ctx = mock(ServletContext.class);
        when(ctx.getInitParameter("dataDirectory")).thenReturn("/tmp");
        template = Files.createTempFile("colTpl", ".html");
        Files.writeString(template, "collection template");
        when(ctx.getRealPath(anyString())).thenReturn(template.toString());
        req = mock(HttpServletRequest.class);
        when(req.getParameter(anyString())).thenReturn("");
        when(req.getParameterMap()).thenReturn(Collections.emptyMap());
    }

    @AfterEach void tearDown() throws Exception {
        dm.close();
        Files.deleteIfExists(template);
    }

    // whitebox 1 empty
    @Test void emptyCollection() throws Exception {
        Collection c = new FakeCollection(")", Types.GAME, new String[0]);

        String html = new TemplateBuilderCollection(ctx, req)
                .doStandardPageCollection(c);

        Assertions.assertTrue(html.contains("collection template"));
        verify(st, never()).executeQuery(anyString());
    }

   // get + 15 branch
    @Test void getPage_normal() throws Exception {
        when(rs.next()).thenReturn(false);
        String sql = "(\"a\",\"b\",\"c\")";
        Collection c = new FakeCollection(sql, Types.MANGA,
                new String[]{"a","b","c"});
        new TemplateBuilderCollection(ctx, req)
                .doStandardPageCollection(c);
        verify(st).executeQuery(
                argThat(s -> s.contains(sql) && s.contains("timeStamp DESC")));
    }

//    whitebox3
    @Test void getPage_json() throws Exception {
        when(req.getParameterMap())
                .thenReturn(Map.of("format", new String[]{"json"}));
        when(req.getParameter("format")).thenReturn("json");

        Collection c = new FakeCollection("(\"x\")", Types.GAME,
                new String[]{"x"});

        String out = new TemplateBuilderCollection(ctx, req)
                .doStandardPageCollection(c);

        Assertions.assertTrue(out.startsWith("{"));
    }

    // whitebox 4 Post + name / creator
    @Test void post_nameCreator() throws Exception {
        when(req.getParameterMap()).thenReturn(Map.of(
                "name",    new String[]{"Mario"},
                "creator", new String[]{"Nin"}));
        when(req.getParameter("name")).thenReturn("Mario");
        when(req.getParameter("creator")).thenReturn("Nin");
        Collection c = new FakeCollection("(\"y\")", Types.MANGA,
                new String[]{"y"});
        new TemplateBuilderCollection(ctx, req).doSearchCollection(c);
        verify(ps, times(2)).setString(eq(1), eq("Mario%"));
        verify(ps, times(2)).setString(eq(2), eq("Nin%"));
        verify(ctx).getRealPath(contains("mangaDetail.html"));
    }

    // test 5 creator id in search
    @Test void post_creatorId() throws Exception {
        when(req.getParameterMap()).thenReturn(Map.of(
                "creator_id",   new String[]{"CID"},
                "cartridge_id", new String[]{"00000000000000000000000000000000"}));
        when(req.getParameter("creator_id")).thenReturn("CID");
        when(req.getParameter("cartridge_id"))
                .thenReturn("00000000000000000000000000000000");
        Collection c = new FakeCollection("(\"z\")", Types.GAME,
                new String[]{"z"});
        new TemplateBuilderCollection(ctx, req)
                .doSearchCollection(c);
        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(conn, atLeastOnce()).prepareStatement(cap.capture());
        Assertions.assertTrue(cap.getAllValues().stream()
                .anyMatch(s -> s.contains("creatorID = ?")));
        verify(ps, times(2)).setString(eq(1), eq("CID"));
    }

    @Test void getPage_withResultLoop() throws Exception {
        when(rs.next()).thenReturn(true, false);
        when(rs.getString(anyString())).thenReturn("");
        Collection c = new FakeCollection("(\"h1\")", Types.GAME,
                new String[]{"h1"});
        new TemplateBuilderCollection(ctx, req)
                .doStandardPageCollection(c);
        verify(rs, atLeastOnce()).next();
    }

    @Test void post_creatorAndCart_only() throws Exception {
        when(req.getParameterMap()).thenReturn(Map.of(
                "creator_id",   new String[]{"DEV1"},
                "cartridge_id", new String[]{"00000000000000000000000000000000"}));
        when(req.getParameter("creator_id")).thenReturn("DEV1");
        when(req.getParameter("cartridge_id"))
                .thenReturn("00000000000000000000000000000000");
        Collection c = new FakeCollection("(\"x\")", Types.GAME, new String[]{"x"});
        new TemplateBuilderCollection(ctx, req).doSearchCollection(c);
        verify(ps, times(2)).setString(eq(1), eq("DEV1"));
    }

    @Test void post_jsonHijack() throws Exception {
        when(req.getParameterMap()).thenReturn(Map.of(
                "format", new String[]{"json"}));
        when(req.getParameter("format")).thenReturn("json");
        Collection c = new FakeCollection("(\"z\")", Types.MANGA, new String[]{"z"});
        String json = new TemplateBuilderCollection(ctx, req)
                .doSearchCollection(c);
        Assertions.assertTrue(json.startsWith("{"));
    }
}
