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

class TestManga {

    private static ResultSet baseRs() throws SQLException {
        ResultSet rs = Mockito.mock(ResultSet.class);
        // return empty string for no stub
        when(rs.getString(anyString())).thenReturn("");
        // BaseMio
        when(rs.getInt("timeStamp")).thenReturn(0);
        when(rs.getString("hash")).thenReturn("H");
        when(rs.getString("brand")).thenReturn("B");
        when(rs.getString("creator")).thenReturn("C");
        when(rs.getString("creatorID")).thenReturn("CID");
        when(rs.getString("cartridgeID")).thenReturn("CAR");
        when(rs.getString("color")).thenReturn("blue");
        when(rs.getInt("logo")).thenReturn(1);
        when(rs.getString("frame0")).thenReturn("p0");
        when(rs.getString("frame1")).thenReturn("p1");
        when(rs.getString("frame2")).thenReturn("p2");
        when(rs.getString("frame3")).thenReturn("p3");
        return rs;
    }

    // whitebox 1 colder cases
    @ParameterizedTest
    @MethodSource("colorCases")
    @Tag("Branch_COLOR")
    void colorLogo_replaced_or_kept(String inputColor, String expectedColor) throws SQLException {
        ResultSet rs = baseRs();
        when(rs.getString("colorLogo")).thenReturn(inputColor);
        when(rs.getString("description")).thenReturn("short desc");
        when(rs.getString("name")).thenReturn("title");
        when(rs.getString("id")).thenReturn("dummy");
        Manga m = new Manga(rs);
        assertEquals(expectedColor, m.colorLogo);
    }
    private static Stream<Arguments> colorCases() {
        return Stream.of(
                Arguments.of("grey darken-4", "grey"),
                Arguments.of("red"          , "red")
        );
    }

    // whitebox 2 long/short
    @Test
    @Tag("Branch_DESC_long_≥19")
    void longDescription_split_into_two_fields() throws SQLException {
        final String DESC = "0123456789ABCDEFGH*";
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

    // whitebox 3 empty title to trigger the default value.
    @Test
    @Tag("Branch_EMPTY_TITLE")
    void blankTitle_converted_to_NoTitle() throws SQLException {
        ResultSet rs = baseRs();
        when(rs.getString("colorLogo")).thenReturn("c");
        when(rs.getString("description")).thenReturn("d");
        when(rs.getString("name")).thenReturn("   ");
        when(rs.getString("id")).thenReturn("id");
        Manga m = new Manga(rs);
        assertEquals("No Title", m.name);
    }

    // blackbox on pages
    @Test
    @Tag("BB_pages_size")
    void pages_contains_four_frames() throws SQLException {
        ResultSet rs = baseRs();
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
