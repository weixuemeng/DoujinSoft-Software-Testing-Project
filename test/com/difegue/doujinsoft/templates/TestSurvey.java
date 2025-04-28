package com.difegue.doujinsoft.templates;

import com.difegue.doujinsoft.utils.MioUtils.Types;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class TestSurvey {

    private static ResultSet stub(int rawType, int commentId,
                                  String rawName, int stars) throws SQLException {
        ResultSet rs = Mockito.mock(ResultSet.class);
        when(rs.getInt("type")).thenReturn(rawType);
        when(rs.getInt("commentId")).thenReturn(commentId);
        when(rs.getString("name")).thenReturn(rawName);
        when(rs.getInt("stars")).thenReturn(stars);
        return rs;
    }

    // whitebox 1, outer x commentedID x 30 branches
    @ParameterizedTest(name = "outerType={0} commentId={1}")
    @MethodSource("commentMatrix")
    @Tag("Branch_COMMENT")
    void everyCommentMappedCorrectly(int outerType, int commentId,
                                     String expColor, String expCat,
                                     String expComment,
                                     int expTypeConstant) throws SQLException {

        ResultSet rs = stub(outerType, commentId, "Foo", 4);
        Survey s = new Survey(rs);
        assertEquals(expColor, s.color);
        assertEquals(expCat,   s.category);
        assertEquals(expTypeConstant, s.type);
        assertEquals(expComment, s.comment);
    }

    // generate 30 test cases data
    private static Stream<Arguments> commentMatrix() {

        Map<Integer, String[]> commentTable = Map.of(
                0, new String[]{
                        "Looked very professional!",
                        "I was so moved!",
                        "Funny!",
                        "Cool graphics!",
                        "Great music!",
                        "Didn't see that coming!",
                        "I want a sequel!",
                        "Very original!",
                        "That was hard!",
                        "You must have worked hard!"
                },
                1, new String[]{
                        "Sounded very professional!",
                        "I was so moved!",
                        "Funny!",
                        "Made me wanna sing!",
                        "Can't wait to hear it again!",
                        "Wasn't expecting that!",
                        "I wanna hear the next one!",
                        "Unique!",
                        "Fun to play!",
                        "You must have worked hard!"
                },
                2, new String[]{
                        "Looked very professional!",
                        "Very...moving!",
                        "Funny!",
                        "Great art style!",
                        "Hilarious punch line!",
                        "That was surprising!",
                        "I wanna read the next one!",
                        "Unique!",
                        "Surreal...",
                        "You must have worked hard!"
                }
        );

        return commentTable.entrySet().stream()
                .flatMap(e -> {
                    int outerType = e.getKey();
                    String[] comments = e.getValue();
                    String expColor = outerType == 0 ? "green"
                            : outerType == 1 ? "pink"
                            :                  "blue";
                    String expCat   = outerType == 0 ? "games"
                            : outerType == 1 ? "records"
                            :                  "comics";
                    int expTypeConst = outerType == 0 ? Types.GAME
                            : outerType == 1 ? Types.RECORD
                            :                  Types.MANGA;

                    return IntStream.range(0, 10).mapToObj(i ->
                            Arguments.of(outerType, i, expColor, expCat,
                                    comments[i], expTypeConst));
                });
    }

    @Test
    @Tag("Branch_DEFAULT")
    void unknownTypeOrCommentFallsBackToBarf() throws SQLException {
        ResultSet rs = stub(99, 42, "X", 1);
        Survey s = new Survey(rs);
        assertEquals("barf", s.comment);
        assertNull(s.color);
        assertNull(s.category);
    }

    @Tag("Survey_GAME_invalidComment")
    @Test
    void gameTypeWithUnknownCommentReturnsBarf() throws SQLException {
        ResultSet rs = stub(0, 42, "Any", 0);
        Survey s = new Survey(rs);

        assertAll(
                () -> assertEquals("green", s.color),
                () -> assertEquals("games", s.category),
                () -> assertEquals("barf", s.comment)
        );
    }

    @Tag("Survey_MANGA_invalidComment")
    @Test
    void mangaTypeWithUnknownCommentReturnsBarf() throws SQLException {
        ResultSet rs = stub(2, -1, "Any", 0);
        Survey s = new Survey(rs);

        assertAll(
                () -> assertEquals("blue", s.color),
                () -> assertEquals("comics", s.category),
                () -> assertEquals("barf", s.comment)
        );
    }

    @Tag("Record_UnknownComment_should_return_barf")
    @Test
    void recordTypeWithUnknownCommentReturnsBarf() throws SQLException {
        ResultSet rs = Mockito.mock(ResultSet.class);
        when(rs.getInt   ("type"     )).thenReturn(1);
        when(rs.getInt   ("commentId")).thenReturn(42);
        when(rs.getString("name"     )).thenReturn("Rec");
        when(rs.getInt   ("stars"    )).thenReturn(3);
        Survey survey = new Survey(rs);

        assertAll(
                () -> assertEquals("barf"  , survey.comment ),
                () -> assertEquals("pink"  , survey.color   ),
                () -> assertEquals("records", survey.category),
                () -> assertEquals(3       , survey.starCount)
        );
    }

    // blackbox: unicode
    @Test
    @Tag("BB_name_sanitise")
    void invalidUnicodeIsRemovedFromName() throws SQLException {
        String raw = "Good\u0001Name";
        ResultSet rs = stub(0, 0, raw, 3);
        Survey s = new Survey(rs);
        assertEquals("GoodName", s.name);
    }

    // blackbox test
    @Test
    @Tag("BB_star_count")
    void starCountIsCopied() throws SQLException {
        ResultSet rs = stub(0, 0, "N", 7);
        Survey s = new Survey(rs);
        assertEquals(7, s.starCount);
    }
}
