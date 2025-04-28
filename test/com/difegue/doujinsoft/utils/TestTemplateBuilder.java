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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class TestTemplateBuilder {
    private MockedStatic<DriverManager> driverMock;
    private Connection        conn;
    private PreparedStatement ps;
    private ResultSet         rs;
    private ServletContext    servletCtx;
    private HttpServletRequest request;
    private Path templateFile;


    @BeforeEach
    void setUp() throws Exception {
        //mock JDBC
        conn = mock(Connection.class);
        ps   = mock(PreparedStatement.class);
        rs   = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        when(rs.getInt(anyInt())).thenReturn(0);
        when(rs.getString(anyString())).thenReturn("");
        when(rs.getString(anyInt())).thenReturn("");
        when(rs.getBytes(anyString())).thenReturn(new byte[0]);
        when(rs.getLong(anyInt())).thenReturn(0L);

        driverMock = mockStatic(DriverManager.class);
        driverMock.when(() -> DriverManager.getConnection(anyString()))
                .thenReturn(conn);

       //mock Servlet
        servletCtx = mock(ServletContext.class);
        when(servletCtx.getInitParameter("dataDirectory")).thenReturn("/tmp");

        // PebbleEngine
        templateFile = Files.createTempFile("tmpl", ".html");
        Files.writeString(templateFile, "dummy template");
        when(servletCtx.getRealPath(anyString())).thenReturn(templateFile.toString());

       //mock Request
        request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn("");
        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
    }

    @AfterEach
    void tearDown() throws Exception {
        driverMock.close();
        Files.deleteIfExists(templateFile);
    }


    //  doStandardPageGeneric
    @Test
    void noSearch_defaultFlow() throws Exception {
        TemplateBuilder builder = new TemplateBuilder(servletCtx, request);
        String out = builder.doStandardPageGeneric(Types.GAME);

        Assertions.assertNotNull(out);
        verify(conn, atLeastOnce()).prepareStatement(anyString());
        verify(ps,   atLeastOnce()).executeQuery();
    }


    @Test
    void creatorIdOnly_illegalCart() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("creator_id",   new String[]{"C123"});
        params.put("cartridge_id", new String[]{"00000000000000000000000000000000"});
        when(request.getParameter("cartridge_id")).thenReturn("00000000000000000000000000000000");
        when(request.getParameter("creator_id")).thenReturn("C123");
        when(request.getParameterMap()).thenReturn(params);

        TemplateBuilder builder = new TemplateBuilder(servletCtx, request);
        builder.doStandardPageGeneric(Types.GAME);

        verify(ps, atLeast(2)).executeQuery();
    }

     // creator_id +  cartridge_id —— isLegitCart = true。
    @Test
    void creatorIdAndCart_validCart() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("creator_id",   new String[]{"abcde"});
        params.put("cartridge_id", new String[]{"ff00ff00ff00ff00ff00ff00ff00ff00"});
        when(request.getParameter("cartridge_id")).thenReturn("00000000000000000000000000000000");
        when(request.getParameter("creator_id")).thenReturn("abcde");
        when(request.getParameterMap()).thenReturn(params);

        TemplateBuilder builder = new TemplateBuilder(servletCtx, request);
        builder.doStandardPageGeneric(Types.GAME);

        verify(ps, atLeast(2)).executeQuery();
    }

    @Test
    void nameSearch_sortByDate() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("name",    new String[]{"Zelda"});
        params.put("sort_by", new String[]{"date"});
        when(request.getParameter("name")).thenReturn("Zelda");
        when(request.getParameter("sort_by")).thenReturn("date");
        when(request.getParameterMap()).thenReturn(params);

        TemplateBuilder builder = new TemplateBuilder(servletCtx, request);
        builder.doStandardPageGeneric(Types.MANGA);

        ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
        verify(conn, atLeastOnce()).prepareStatement(sqlCap.capture());
        Assertions.assertTrue(sqlCap.getAllValues().stream()
                .anyMatch(sql -> sql.contains("timeStamp DESC")));
    }

    @Test
    void creatorNameOnly_search() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("creator", new String[]{"IndieDev"});
        when(request.getParameter("creator")).thenReturn("IndieDev");
        when(request.getParameterMap()).thenReturn(params);

        TemplateBuilder builder = new TemplateBuilder(servletCtx, request);
        builder.doStandardPageGeneric(Types.MANGA);
        ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
        verify(conn, atLeastOnce()).prepareStatement(sqlCap.capture());
        Assertions.assertTrue(sqlCap.getAllValues().stream()
                .anyMatch(sql -> sql.contains("creator LIKE ?")));
    }


    @Test
    void hashId_jsonOutput() throws Exception {
        when(rs.next()).thenReturn(true, false);

        Map<String, String[]> params = new HashMap<>();
        params.put("id",     new String[]{"deadbeef"});
        params.put("format", new String[]{"json"});
        when(request.getParameter("id")).thenReturn("deadbeef");
        when(request.getParameter("format")).thenReturn("json");
        when(request.getParameterMap()).thenReturn(params);
        TemplateBuilder builder = new TemplateBuilder(servletCtx, request);
        String json = builder.doStandardPageGeneric(Types.GAME);
        Assertions.assertTrue(json.startsWith("{"));
        ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
        verify(conn).prepareStatement(sqlCap.capture());
        Assertions.assertTrue(sqlCap.getValue().contains("hash == ?"));
        verify(ps).setString(eq(1), eq("deadbeef"));
    }

    @Test
    void jsonFormat_creatorSearch() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("creator_id", new String[]{"XYZ"});
        params.put("format",    new String[]{"json"});
        params.put("cartridge_id", new String[]{"00000000000000000000000000000000"});
        when(request.getParameter("cartridge_id")).thenReturn("00000000000000000000000000000000");
        when(request.getParameter("creator_id")).thenReturn("XYZ");
        when(request.getParameter("format")).thenReturn("json");
        when(request.getParameterMap()).thenReturn(params);
        TemplateBuilder builder = new TemplateBuilder(servletCtx, request);
        String jsonOut = builder.doSearchGeneric(Types.RECORD);
        Assertions.assertTrue(jsonOut.startsWith("{"));
        verify(ps, atLeast(2)).executeQuery();
    }

    @Test
    void nameAndCreatorSearch_sortByName_page2() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("name",    new String[]{"Mario"});
        params.put("creator", new String[]{"Nintendo"});
        params.put("sort_by", new String[]{"name"});
        params.put("page",    new String[]{"2"});
        params.put("format",  new String[]{"json"});
        when(request.getParameter("name")).thenReturn("Mario");
        when(request.getParameter("creator")).thenReturn("Nintendo");
        when(request.getParameter("sort_by")).thenReturn("name");
        when(request.getParameter("page")).thenReturn("2");
        when(request.getParameter("format")).thenReturn("json");
        when(request.getParameterMap()).thenReturn(params);

        TemplateBuilder builder = new TemplateBuilder(servletCtx, request);
        builder.doSearchGeneric(Types.GAME);
        ArgumentCaptor<Integer> intCap = ArgumentCaptor.forClass(Integer.class);
        verify(ps, atLeastOnce()).setInt(eq(3), intCap.capture());
        Assertions.assertTrue(intCap.getAllValues().stream().anyMatch(i -> i == 15));
    }

    @Test
    void creatorSearch_sortByName_page3_legitCart() throws Exception {
        Map<String,String[]> p = new HashMap<>();
        p.put("creator_id",   new String[]{"CREATOR-42"});
        p.put("cartridge_id", new String[]{"cafebabecafebabecafebabecafebabe"});
        p.put("sort_by",      new String[]{"name"});
        p.put("page",         new String[]{"3"});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("creator_id")).thenReturn("CREATOR-42");
        when(request.getParameter("cartridge_id")).thenReturn("cafebabecafebabecafebabecafebabe");
        when(request.getParameter("sort_by")).thenReturn("name");
        when(request.getParameter("page")).thenReturn("3");
        when(rs.next()).thenReturn(true, false);

        new TemplateBuilder(servletCtx, request).doStandardPageGeneric(Types.GAME);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conn, atLeastOnce()).prepareStatement(sql.capture());
        Assertions.assertTrue(sql.getAllValues().stream()
                .anyMatch(s -> s.contains("OR cartridgeID = ?")));
        verify(ps, times(2)).setString(eq(2), eq("cafebabecafebabecafebabecafebabe"));
        verify(ps).setInt(eq(3), eq(30));
    }

    @Test
    void creatorSearch_sortByDate() throws Exception {
        Map<String,String[]> p = Map.of(
                "creator_id", new String[]{"CREATOR-99"},
                "cartridge_id", new String[]{"00000000000000000000000000000000"},
                "sort_by", new String[]{"date"});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("creator_id")).thenReturn("CREATOR-99");
        when(request.getParameter("cartridge_id")).thenReturn("00000000000000000000000000000000");
        when(request.getParameter("sort_by")).thenReturn("date");

        new TemplateBuilder(servletCtx, request).doStandardPageGeneric(Types.RECORD);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conn, atLeastOnce()).prepareStatement(sql.capture());
        Assertions.assertTrue(
                sql.getAllValues().stream().anyMatch(s -> s.contains("timeStamp DESC")));
    }

    @Test
    void nameAndCreator_page2_resultsLoop() throws Exception {
        Map<String,String[]> p = Map.of(
                "name",    new String[]{"Puzzle"},
                "creator", new String[]{"Indie"},
                "page",    new String[]{"2"});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("name")).thenReturn("Puzzle");
        when(request.getParameter("creator")).thenReturn("Indie");
        when(request.getParameter("page")).thenReturn("2");
        when(rs.next()).thenReturn(true, false);
        new TemplateBuilder(servletCtx, request).doSearchGeneric(Types.MANGA);
        verify(ps).setInt(eq(3), eq(15));
        verify(rs, atLeastOnce()).next();
    }

    @Test
    void pageParam_presentButEmpty() throws Exception {
        Map<String,String[]> p = Map.of("page", new String[]{""});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("page")).thenReturn("");
        new TemplateBuilder(servletCtx, request).doSearchGeneric(Types.GAME);
        verify(ps).setInt(eq(1), eq(0));
    }

    @Test
    void page5_creatorSearch() throws Exception {
        Map<String,String[]> p = Map.of(
                "creator_id", new String[]{"CID"},
                "cartridge_id", new String[]{"00000000000000000000000000000000"},
                "page", new String[]{"5"});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("creator_id")).thenReturn("CID");
        when(request.getParameter("cartridge_id")).thenReturn("00000000000000000000000000000000");
        when(request.getParameter("page")).thenReturn("5");
        new TemplateBuilder(servletCtx, request).doStandardPageGeneric(Types.RECORD);
        verify(ps).setInt(eq(2), eq(60));
    }

    @Test
    void onlyCartridgeId_search() throws Exception {
        Map<String,String[]> p = Map.of("cartridge_id",
                new String[]{"ffffffffffffffffffffffffffffffff"});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("cartridge_id"))
                .thenReturn("ffffffffffffffffffffffffffffffff");

        new TemplateBuilder(servletCtx, request).doStandardPageGeneric(Types.GAME);
        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(conn, atLeastOnce()).prepareStatement(cap.capture());
        Assertions.assertTrue(cap.getAllValues().stream()
                .anyMatch(s -> s.contains("cartridgeID = ?")));
    }

    @Test
    void jsonOutput_normalSearch() throws Exception {
        Map<String,String[]> p = Map.of("format", new String[]{"json"});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("format")).thenReturn("json");
        String json = new TemplateBuilder(servletCtx, request)
                .doStandardPageGeneric(Types.MANGA);
        Assertions.assertTrue(json.startsWith("{"));
    }

    @Test
    void creatorIdSearch_noNameNoCreator() throws Exception {
        Map<String,String[]> p = Map.of("creator_id", new String[]{"X"},
                "cartridge_id",
                new String[]{"00000000000000000000000000000000"});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("creator_id")).thenReturn("X");
        when(request.getParameter("cartridge_id"))
                .thenReturn("00000000000000000000000000000000");
        new TemplateBuilder(servletCtx, request).doSearchGeneric(Types.GAME);
        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(conn, atLeastOnce()).prepareStatement(cap.capture());
        Assertions.assertFalse(cap.getAllValues().stream()
                .anyMatch(s -> s.contains("name LIKE ?")));
    }

    @Test
    void creatorSearch_defaultSort() throws Exception {
        Map<String,String[]> p = Map.of(
                "creator_id",   new String[]{"ALPHA"},
                "cartridge_id", new String[]{"00000000000000000000000000000000"});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("creator_id")).thenReturn("ALPHA");
        when(request.getParameter("cartridge_id"))
                .thenReturn("00000000000000000000000000000000");

        new TemplateBuilder(servletCtx, request).doStandardPageGeneric(Types.GAME);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conn, atLeastOnce()).prepareStatement(sql.capture());
        Assertions.assertTrue(sql.getAllValues().stream()
                .anyMatch(s -> s.contains("normalizedName ASC")));
    }

    @Test
    void mixedNameAndCreatorId() throws Exception {
        Map<String,String[]> p = Map.of(
                "name",        new String[]{"RPG"},
                "creator_id",  new String[]{"XYZ"},
                "cartridge_id",new String[]{"00000000000000000000000000000000"});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("name")).thenReturn("RPG");
        when(request.getParameter("creator_id")).thenReturn("XYZ");
        when(request.getParameter("cartridge_id"))
                .thenReturn("00000000000000000000000000000000");
        new TemplateBuilder(servletCtx, request).doStandardPageGeneric(Types.MANGA);
        verify(ps, atLeastOnce()).setString(eq(1), eq("RPG%"));
    }

    @Test
    void htmlOutput_default() throws Exception {
        new TemplateBuilder(servletCtx, request)
                .doStandardPageGeneric(Types.RECORD);
        verify(conn, atLeastOnce()).prepareStatement(anyString());
    }

    @Test
    void search_nameOnly_html() throws Exception {
        Map<String,String[]> p = Map.of("name", new String[]{"Puzzle"});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("name")).thenReturn("Puzzle");
        String html = new TemplateBuilder(servletCtx, request)
                .doSearchGeneric(Types.GAME);
        Assertions.assertTrue(html.contains("dummy template"));
    }

    @Test
    void testSearchGeneric_detail_survey() throws Exception {
        Map<String,String[]> p = Map.of(
                "name",     new String[]{"Any"},
                "creator",  new String[]{"Who"},
                "sort_by",  new String[]{"date"});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("name")).thenReturn("Any");
        when(request.getParameter("creator")).thenReturn("Who");
        when(request.getParameter("sort_by")).thenReturn("date");
        new TemplateBuilder(servletCtx, request).doSearchGeneric(Types.SURVEY);
        verify(servletCtx).getRealPath(contains("surveysDetail.html"));
    }

    @Test
    void testSearchGeneric_empty_noJson() throws Exception {
        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
        String html = new TemplateBuilder(servletCtx, request)
                .doSearchGeneric(Types.GAME);
        verify(ps).setInt(eq(1), eq(0));   // only one
        Assertions.assertFalse(html.startsWith("{"));
    }

    //performCreatorSearchQuery -> page
    @Test
    void creatorSearch_pageKeyButEmpty() throws Exception {
        Map<String,String[]> p = new HashMap<>();
        p.put("creator_id",   new String[]{"CREATOR-1"});
        p.put("cartridge_id", new String[]{"00000000000000000000000000000000"});
        p.put("page",         new String[]{""});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("creator_id")).thenReturn("CREATOR-1");
        when(request.getParameter("cartridge_id")).thenReturn("00000000000000000000000000000000");
        when(request.getParameter("page")).thenReturn("");
        new TemplateBuilder(servletCtx, request).doStandardPageGeneric(Types.GAME);
        verify(ps).setInt(eq(2), eq(0));
    }

    @Test
    void hashId_formatNotJson() throws Exception {
        when(rs.next()).thenReturn(false);
        Map<String,String[]> p = Map.of(
                "id",     new String[]{"abcd"},
                "format", new String[]{"xml"});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("id")).thenReturn("abcd");
        when(request.getParameter("format")).thenReturn("xml");
        String html = new TemplateBuilder(servletCtx, request)
                .doStandardPageGeneric(Types.GAME);
        Assertions.assertFalse(html.startsWith("{"));
    }

    @Test
    void emptyPost_noJson() throws Exception {
        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
        String out = new TemplateBuilder(servletCtx, request)
                .doSearchGeneric(Types.MANGA);
        Assertions.assertFalse(out.startsWith("{"));
        verify(ps).setInt(eq(1), eq(0));
    }

    @Test
    void initTemplate_sortKeyButEmpty() throws Exception {
        Map<String,String[]> p = Map.of("sort_by", new String[]{""});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("sort_by")).thenReturn("");
        new TemplateBuilder(servletCtx, request).doStandardPageGeneric(Types.MANGA);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conn, atLeastOnce()).prepareStatement(sql.capture());
        Assertions.assertTrue(sql.getAllValues().stream()
                .anyMatch(s -> s.contains("ORDER BY normalizedName ASC")));
    }

    @Test
    void surveyMinimal() throws Exception {
        String html = new TemplateBuilder(servletCtx, request)
                .doStandardPageGeneric(Types.SURVEY);
        Assertions.assertNotNull(html);
    }

    @Test
    void cartridgeOnly_search() throws Exception {
        when(request.getParameterMap())
                .thenReturn(Map.of("cartridge_id",
                        new String[]{"ffeeddccbbaa00112233445566778899"}));
        when(request.getParameter("cartridge_id"))
                .thenReturn("ffeeddccbbaa00112233445566778899");
        new TemplateBuilder(servletCtx, request)
                .doStandardPageGeneric(Types.GAME);
        verify(ps, times(2)).setString(eq(2), eq("ffeeddccbbaa00112233445566778899"));
    }

    @Test
    void creatorIdOnly_search() throws Exception {
        when(request.getParameterMap())
                .thenReturn(Map.of("creator_id", new String[]{"DEV42"}));
        when(request.getParameter("creator_id")).thenReturn("DEV42");

        new TemplateBuilder(servletCtx, request)
                .doStandardPageGeneric(Types.MANGA);
        verify(ps, atLeast(2)).setString(eq(1), eq("DEV42"));
    }

    @Test
    void post_nameSearch_json() throws Exception {
        Map<String,String[]> body = Map.of(
                "name",   new String[]{"Zelda"},
                "format", new String[]{"json"});
        when(request.getParameterMap()).thenReturn(body);
        when(request.getParameter("name")).thenReturn("Zelda");
        when(request.getParameter("format")).thenReturn("json");

        String json = new TemplateBuilder(servletCtx, request)
                .doSearchGeneric(Types.RECORD);

        Assertions.assertTrue(json.startsWith("{"));
        verify(ps, times(2)).setString(eq(1), eq("Zelda%"));
    }


}
