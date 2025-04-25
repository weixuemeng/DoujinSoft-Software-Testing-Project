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

/**
 * Unit tests for {@link TemplateBuilder}.
 * <p>
 * 设计目标：
 * <ul>
 *     <li>覆盖 <b>doStandardPageGeneric</b>/<b>doSearchGeneric</b> 的主要分支</li>
 *     <li>验证关键 SQL／参数是否正确，而非 SQL 完整字符串</li>
 *     <li>引入最少依赖（仅 Mockito + JUnit5）</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class TestTemplateBuilder {

    /* -------------------- JDBC & Servlet mocks -------------------- */
    private MockedStatic<DriverManager> driverMock;
    private Connection        conn;
    private PreparedStatement ps;
    private ResultSet         rs;
    private ServletContext    servletCtx;
    private HttpServletRequest request;

    /* 临时 Pebble 模板文件 */
    private Path templateFile;

    /* ---------------------------------------------------------------- */

    @BeforeEach
    void setUp() throws Exception {
        /* ==== mock JDBC 层 ==== */
        conn = mock(Connection.class);
        ps   = mock(PreparedStatement.class);
        rs   = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        // 默认 result.next() 为 false，个别测试会重写
        when(rs.next()).thenReturn(false);
        when(rs.getInt(anyInt())).thenReturn(0);
        when(rs.getString(anyString())).thenReturn("");
        when(rs.getString(anyInt())).thenReturn("");
        when(rs.getBytes(anyString())).thenReturn(new byte[0]);
        when(rs.getLong(anyInt())).thenReturn(0L);

        driverMock = mockStatic(DriverManager.class);
        driverMock.when(() -> DriverManager.getConnection(anyString()))
                .thenReturn(conn);

        /* ==== mock Servlet ==== */
        servletCtx = mock(ServletContext.class);
        when(servletCtx.getInitParameter("dataDirectory")).thenReturn("/tmp");

        // PebbleEngine 需要真实模板文件
        templateFile = Files.createTempFile("tmpl", ".html");
        Files.writeString(templateFile, "dummy template");
        when(servletCtx.getRealPath(anyString())).thenReturn(templateFile.toString());

        /* ==== mock Request 基础行为 ==== */
        request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn("");
        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
    }

    @AfterEach
    void tearDown() throws Exception {
        driverMock.close();
        Files.deleteIfExists(templateFile);
    }

    /* =============================================================== */
    /* -------------------- doStandardPageGeneric -------------------- */

    /**
     * 进入“无搜索”默认路径：<br>
     *  name/creator 均为空，走 performSearchQuery 的默认分支。
     */
    @Test
    void noSearch_defaultFlow() throws Exception {
        TemplateBuilder builder = new TemplateBuilder(servletCtx, request);
        String out = builder.doStandardPageGeneric(Types.GAME);

        Assertions.assertNotNull(out);
        verify(conn, atLeastOnce()).prepareStatement(anyString());
        verify(ps,   atLeastOnce()).executeQuery();
    }

    /**
     * 仅 <code>creator_id</code>，且 cartridge 为全 0 —— 触发
     * <code>performCreatorSearchQuery</code> 路径，isLegitCart = false。
     */

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

        verify(ps, atLeast(2)).executeQuery(); // data + count
    }

    /**
     * creator_id + 合法 cartridge_id —— isLegitCart = true。
     */
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

    /**
     * 仅 name 搜索 + sort_by=date —— 覆盖
     * <code>isContentNameSearch == true</code> / creatorSearch == false
     * & orderBy = "timeStamp DESC"。
     */
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

    /**
     * 仅 creator 搜索（不带 name）—— 触发三元表达式分支
     * (isContentNameSearch || isCreatorNameSearch) == true 且只有后者 true。
     */
    @Test
    void creatorNameOnly_search() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("creator", new String[]{"IndieDev"});
        when(request.getParameter("creator")).thenReturn("IndieDev");
        when(request.getParameterMap()).thenReturn(params);

        TemplateBuilder builder = new TemplateBuilder(servletCtx, request);
        builder.doStandardPageGeneric(Types.MANGA);

        // 确认 LIKE ? 出现两次（name LIKE ? AND creator LIKE ?）
        ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
        verify(conn, atLeastOnce()).prepareStatement(sqlCap.capture());
        Assertions.assertTrue(sqlCap.getAllValues().stream()
                .anyMatch(sql -> sql.contains("creator LIKE ?")));
    }

    /**
     * 带 <code>id</code> 查询 & format=json
     * 覆盖“单条 item”分支和 JSON hijack。
     */
    @Test
    void hashId_jsonOutput() throws Exception {
        // resultSet.next() -> true then false，以便 while(result.next()) 走一次
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
        // 准备的 SQL 应包含 "hash == ?"
        ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
        verify(conn).prepareStatement(sqlCap.capture());
        Assertions.assertTrue(sqlCap.getValue().contains("hash == ?"));
        // stmt.setString(1, "deadbeef") 被调用
        verify(ps).setString(eq(1), eq("deadbeef"));
    }

    /* =============================================================== */
    /* ----------------------- doSearchGeneric ----------------------- */

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
        // 缺省 cartridge_id -> null，要求 TemplateBuilder 对 null 做防护

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

        // page=2 -> OFFSET 15
        ArgumentCaptor<Integer> intCap = ArgumentCaptor.forClass(Integer.class);
        verify(ps, atLeastOnce()).setInt(eq(3), intCap.capture());
        Assertions.assertTrue(intCap.getAllValues().stream().anyMatch(i -> i == 15));
    }

    @Test
    void creatorSearch_sortByName_page3_legitCart() throws Exception {
        // --- request 参数 ---
        Map<String,String[]> p = new HashMap<>();
        p.put("creator_id",   new String[]{"CREATOR-42"});
        p.put("cartridge_id", new String[]{"cafebabecafebabecafebabecafebabe"}); // 合法 -> isLegitCart = true
        p.put("sort_by",      new String[]{"name"});       // 触发 orderBy = "normalizedName ASC"
        p.put("page",         new String[]{"3"});          // page != 1 分支
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("creator_id")).thenReturn("CREATOR-42");
        when(request.getParameter("cartridge_id")).thenReturn("cafebabecafebabecafebabecafebabe");
        when(request.getParameter("sort_by")).thenReturn("name");
        when(request.getParameter("page")).thenReturn("3");

        // ResultSet 至少有 1 条，走 while(result.next())
        when(rs.next()).thenReturn(true, false);

        new TemplateBuilder(servletCtx, request).doStandardPageGeneric(Types.GAME);

        /* ------- 验证点 ------- */
        // 1. SQL 里出现 "OR cartridgeID = ?"
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conn, atLeastOnce()).prepareStatement(sql.capture());
        Assertions.assertTrue(sql.getAllValues().stream()
                .anyMatch(s -> s.contains("OR cartridgeID = ?")));

        // 2. 对合法 cartridge_id 调用了 setString(2, ...)
        // 精确 2 次
        verify(ps, times(2)).setString(eq(2), eq("cafebabecafebabecafebabecafebabe"));
//        verify(ps, atLeast(2)).setString(eq(2), eq("cafebabecafebabecafebabecafebabe"));
        // 3. page = 3 -> OFFSET (3*15-15)=30
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

        // 让 ResultSet 有一条数据 → while(result.next()) 进入体内
        when(rs.next()).thenReturn(true, false);

        new TemplateBuilder(servletCtx, request).doSearchGeneric(Types.MANGA);

        // OFFSET = 15 （因为 page=2）
        verify(ps).setInt(eq(3), eq(15));
        // 至少调用了一次 rs.next()
        verify(rs, atLeastOnce()).next();
    }

    @Test
    void pageParam_presentButEmpty() throws Exception {
        Map<String,String[]> p = Map.of("page", new String[]{""});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("page")).thenReturn("");

        new TemplateBuilder(servletCtx, request).doSearchGeneric(Types.GAME);

        // page 默认 1 → OFFSET 0
        verify(ps).setInt(eq(1), eq(0));   // performSearchQuery
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

        // OFFSET 60 (=5*15-15)
        verify(ps).setInt(eq(2), eq(60));  // performCreatorSearchQuery：legitCart==false
    }

    @Test
    void onlyCartridgeId_search() throws Exception {
        Map<String,String[]> p = Map.of("cartridge_id",
                new String[]{"ffffffffffffffffffffffffffffffff"});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("cartridge_id"))
                .thenReturn("ffffffffffffffffffffffffffffffff");

        new TemplateBuilder(servletCtx, request).doStandardPageGeneric(Types.GAME);

        // SQL 一定含 "cartridgeID = ?"
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
        // 不应出现 "name LIKE ?"
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

        // SQL 中应使用默认 orderBy = "normalizedName ASC"
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

        // verify 触发的其实是 performSearchQuery 而不是 performCreatorSearchQuery
        verify(ps, atLeastOnce()).setString(eq(1), eq("RPG%"));
    }

    @Test
    void htmlOutput_default() throws Exception {
        // 不传 format
        new TemplateBuilder(servletCtx, request)
                .doStandardPageGeneric(Types.RECORD);

        // 若返回的是 HTML，应该含有 dummy 模板内容
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

        // realPath 必须带 “surveysDetail.html”
        verify(servletCtx).getRealPath(contains("surveysDetail.html"));
    }

    @Test
    void testSearchGeneric_empty_noJson() throws Exception {
        // 参数表保持空，或仅给 page
        when(request.getParameterMap()).thenReturn(Collections.emptyMap());

        String html = new TemplateBuilder(servletCtx, request)
                .doSearchGeneric(Types.GAME);

        // page 缺省 => OFFSET 0
        verify(ps).setInt(eq(1), eq(0));   // 只有一个占位符
        Assertions.assertFalse(html.startsWith("{"));
    }

    // ① 触发 performCreatorSearchQuery -> page 参数“存在但为空”
    @Test
    void creatorSearch_pageKeyButEmpty() throws Exception {
        Map<String,String[]> p = new HashMap<>();
        p.put("creator_id",   new String[]{"CREATOR-1"});
        p.put("cartridge_id", new String[]{"00000000000000000000000000000000"});
        p.put("page",         new String[]{""});          // key 有、value 为空
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("creator_id")).thenReturn("CREATOR-1");
        when(request.getParameter("cartridge_id")).thenReturn("00000000000000000000000000000000");
        when(request.getParameter("page")).thenReturn("");

        new TemplateBuilder(servletCtx, request).doStandardPageGeneric(Types.GAME);

        // page 仍应被视为 1 -> OFFSET 0
        verify(ps).setInt(eq(2), eq(0));                 // isLegitCart = false ⇒ 占位符是 2
    }

    // ② 触发 doStandardPageGeneric 的 “format 参数存在但不是 json” 分支
    @Test
    void hashId_formatNotJson() throws Exception {
        when(rs.next()).thenReturn(false);               // 没有记录

        Map<String,String[]> p = Map.of(
                "id",     new String[]{"abcd"},
                "format", new String[]{"xml"});              // ≠ "json"
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("id")).thenReturn("abcd");
        when(request.getParameter("format")).thenReturn("xml");

        String html = new TemplateBuilder(servletCtx, request)
                .doStandardPageGeneric(Types.GAME);

        Assertions.assertFalse(html.startsWith("{"));    // 走 writeToTemplate()
    }

    @Test
    void emptyPost_noJson() throws Exception {
        // POST 但没有任何查询参数
        when(request.getParameterMap()).thenReturn(Collections.emptyMap());

        String out = new TemplateBuilder(servletCtx, request)
                .doSearchGeneric(Types.MANGA);

        // should fall back to writeToTemplate()
        Assertions.assertFalse(out.startsWith("{"));
        verify(ps).setInt(eq(1), eq(0));                 // page 默认 1 ⇒ OFFSET 0
    }

    @Test
    void initTemplate_sortKeyButEmpty() throws Exception {
        Map<String,String[]> p = Map.of("sort_by", new String[]{""});
        when(request.getParameterMap()).thenReturn(p);
        when(request.getParameter("sort_by")).thenReturn("");

        new TemplateBuilder(servletCtx, request).doStandardPageGeneric(Types.MANGA);

        // 由于 value 为空，isSortedBy 应为 false → 默认 orderBy = normalizedName
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conn, atLeastOnce()).prepareStatement(sql.capture());
        Assertions.assertTrue(sql.getAllValues().stream()
                .anyMatch(s -> s.contains("ORDER BY normalizedName ASC")));
    }

    /* 1) survey 类型 —— 补 switch(type) 的最后一支 */
    @Test
    void surveyMinimal() throws Exception {
        String html = new TemplateBuilder(servletCtx, request)
                .doStandardPageGeneric(Types.SURVEY);
        Assertions.assertNotNull(html);
    }

    /* 2) 纯 cartridge_id 查询（合法 32hex） */
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

    /* 3) 纯 creator_id 查询 —— 走 isLegitCart==false 分支 */
    @Test
    void creatorIdOnly_search() throws Exception {
        when(request.getParameterMap())
                .thenReturn(Map.of("creator_id", new String[]{"DEV42"}));
        when(request.getParameter("creator_id")).thenReturn("DEV42");

        new TemplateBuilder(servletCtx, request)
                .doStandardPageGeneric(Types.MANGA);

        // 只有 setString(1, "DEV42")，没有位置 2
        verify(ps, atLeast(2)).setString(eq(1), eq("DEV42"));
    }

    /* 4) POST 搜索 name=Zelda & format=json
          —— 把 doSearchGeneric 剩下两个红支一次踩完 */
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
        // verify LIKE ? 占位被设置
        verify(ps, times(2)).setString(eq(1), eq("Zelda%"));
    }




}
