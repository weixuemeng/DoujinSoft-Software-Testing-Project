package com.difegue.doujinsoft.templates;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 白盒：专门触发 BaseMio 的 specialBrand 三分支
 * 黑盒：最后验收 pages 不存在且字段继承正常
 */
class TestRecord {

    /* ─────────── 基础 mock，避免零散 NPE ─────────── */

    private static ResultSet baseRs() throws SQLException {
        ResultSet rs = Mockito.mock(ResultSet.class);

        when(rs.getString(anyString())).thenReturn(""); // 未 stub 列默认空

        when(rs.getInt("timeStamp")).thenReturn(0);
        when(rs.getString("hash")).thenReturn("H");
        when(rs.getString("brand")).thenReturn("BR");
        when(rs.getString("creator")).thenReturn("CR");
        when(rs.getString("creatorID")).thenReturn("CID");
        when(rs.getString("cartridgeID")).thenReturn("CAR");
        when(rs.getString("color")).thenReturn("blue");
        when(rs.getInt("logo")).thenReturn(1);
        when(rs.getString("colorLogo")).thenReturn("c");   // 不触发 grey 分支
        when(rs.getString("description")).thenReturn("   "); // 触发空描述

        // Record 没有 frame/xtra 字段

        return rs;
    }

    /* ─────────── ① specialBrand 三分支 ─────────── */

    @ParameterizedTest(name="#{index} ⇒ id={0} → {1}")
    @MethodSource("brandCases")
    @Tag("Branch_SPECIAL_BRAND")
    void specialBrand_set_correctly(String id, String expected) throws SQLException {
        ResultSet rs = baseRs();
        when(rs.getString("name")).thenReturn("title");
        when(rs.getString("id")).thenReturn(id);

        Record rec = new Record(rs);

        assertEquals(expected, rec.specialBrand);
        // 还顺带验证“空描述 ➜ No Description.”
        assertEquals("No Description.", rec.mioDesc1);
        assertEquals("",                rec.mioDesc2);
    }
    private static Stream<Arguments> brandCases() {
        return Stream.of(
                Arguments.of("themId",   "theme"),
                Arguments.of("瓦里奥_wari", "wario"),
                Arguments.of("123nintendo456", "nintendo")
        );
    }

    /* ─────────── ② 黑盒验收：pages 不存在 & 字段继承 ─────────── */

    @Test
    @Tag("BB_basic_fields")
    void record_has_no_extra_pages_and_inherits_fields() throws SQLException {
        ResultSet rs = baseRs();
        when(rs.getString("name")).thenReturn("myRecord");
        when(rs.getString("id")).thenReturn("plainId");

        Record rec = new Record(rs);

        assertNull(rec.specialBrand);          // 普通 id 不触发
        assertEquals("myRecord", rec.name);    // 继承字段正常
    }
}
