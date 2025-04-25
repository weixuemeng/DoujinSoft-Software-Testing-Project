package com.difegue.doujinsoft.templates;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 白盒为主：覆盖 BaseMio 中与 Manga 相关的全部分支；
 * 顺带最後一条黑盒，保证 pages 行为正确。
 */
class TestManga {

    /* ──────────── 共用的 ResultSet Mock 工具 ──────────── */

    /** 带「安全默认值」的基础 mock，防止 NPE */
    private static ResultSet baseRs() throws SQLException {
        ResultSet rs = Mockito.mock(ResultSet.class);

        // 未 stub 的列默认返回空串
        when(rs.getString(anyString())).thenReturn("");

        // BaseMio 其他必需字段
        when(rs.getInt("timeStamp")).thenReturn(0);
        when(rs.getString("hash")).thenReturn("H");
        when(rs.getString("brand")).thenReturn("B");
        when(rs.getString("creator")).thenReturn("C");
        when(rs.getString("creatorID")).thenReturn("CID");
        when(rs.getString("cartridgeID")).thenReturn("CAR");
        when(rs.getString("color")).thenReturn("blue");
        when(rs.getInt("logo")).thenReturn(1);

        // 4 帧默认值
        when(rs.getString("frame0")).thenReturn("p0");
        when(rs.getString("frame1")).thenReturn("p1");
        when(rs.getString("frame2")).thenReturn("p2");
        when(rs.getString("frame3")).thenReturn("p3");

        return rs;
    }

    /* ──────────── 白盒 ①：颜色替换分支 ──────────── */

    @ParameterizedTest
    @MethodSource("colorCases")
    @Tag("Branch_COLOR")              // 让 reader 一眼看出测试的是颜色分支
    void colorLogo_replaced_or_kept(String inputColor, String expectedColor) throws SQLException {
        ResultSet rs = baseRs();
        when(rs.getString("colorLogo")).thenReturn(inputColor);
        when(rs.getString("description")).thenReturn("short desc");      // 走 “≤18” 分支
        when(rs.getString("name")).thenReturn("title");
        when(rs.getString("id")).thenReturn("dummy");

        Manga m = new Manga(rs);
        assertEquals(expectedColor, m.colorLogo);
    }
    private static Stream<Arguments> colorCases() {
        return Stream.of(
                Arguments.of("grey darken-4", "grey"),   // 会被替换
                Arguments.of("red"          , "red")     // 保持不变
        );
    }

    /* ──────────── 白盒 ②：描述长短边界 ──────────── */

    @Test
    @Tag("Branch_DESC_long_≥19")
    void longDescription_split_into_two_fields() throws SQLException {
        final String DESC = "0123456789ABCDEFGH*"; // 20 字符
        ResultSet rs = baseRs();
        when(rs.getString("colorLogo")).thenReturn("c");
        when(rs.getString("description")).thenReturn(DESC);
        when(rs.getString("name")).thenReturn("name");
        when(rs.getString("id")).thenReturn("id");

        Manga m = new Manga(rs);
        assertEquals(DESC.substring(0,18), m.mioDesc1);
        assertEquals(DESC.substring(18)  , m.mioDesc2);
    }

    @Test
    @Tag("Branch_DESC_short_≤18")
    void shortDescription_kept_single_field() throws SQLException {
        final String DESC = "shorter than 18";
        ResultSet rs = baseRs();
        when(rs.getString("colorLogo")).thenReturn("c");
        when(rs.getString("description")).thenReturn(DESC);
        when(rs.getString("name")).thenReturn("name");
        when(rs.getString("id")).thenReturn("id");

        Manga m = new Manga(rs);
        assertEquals(DESC, m.mioDesc1);
        assertEquals("",  m.mioDesc2);
    }

    /* ──────────── 白盒 ③：空标题触发默认值 ──────────── */

    @Test
    @Tag("Branch_EMPTY_TITLE")
    void blankTitle_converted_to_NoTitle() throws SQLException {
        ResultSet rs = baseRs();
        when(rs.getString("colorLogo")).thenReturn("c");
        when(rs.getString("description")).thenReturn("d");
        when(rs.getString("name")).thenReturn("   ");    // 空白
        when(rs.getString("id")).thenReturn("id");

        Manga m = new Manga(rs);
        assertEquals("No Title", m.name);
    }

    /* ──────────── 黑盒验收：pages 列表 ──────────── */

    @Test
    @Tag("BB_pages_size")
    void pages_contains_four_frames() throws SQLException {
        ResultSet rs = baseRs();   // 用默认四帧
        when(rs.getString("colorLogo")).thenReturn("x");
        when(rs.getString("description")).thenReturn("d");
        when(rs.getString("name")).thenReturn("n");
        when(rs.getString("id")).thenReturn("id");

        Manga m = new Manga(rs);
        List<String> p = m.pages;

        assertEquals(4, p.size());
        assertIterableEquals(List.of("p0","p1","p2","p3"), p);
    }
}
