package com.difegue.doujinsoft.templates;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

public class TestGame {
    @ParameterizedTest
    @MethodSource("differentCombinations")
    public void testGameWithDifferentCombinations(String colorLogo, String description, String name, String id, String previewPic, boolean isNsfw) throws SQLException {
        ResultSet result = mock(ResultSet.class);
        InOrder inOrder = inOrder(result);
        when(result.getString("colorLogo")).thenReturn(colorLogo);
        when(result.getString("description")).thenReturn(description);
        when(result.getString("name")).thenReturn(name);
        when(result.getString("id")).thenReturn(id);
        when(result.getString("previewPic")).thenReturn(previewPic);
        when(result.getBoolean("isNsfw")).thenReturn(isNsfw);
        Game game = new Game(result);
        inOrder.verify(result, times(1)).getString("previewPic");
        inOrder.verify(result, times(1)).getBoolean("isNsfw");
        inOrder.verifyNoMoreInteractions();
        Assertions.assertEquals(previewPic,game.preview);
        Assertions.assertEquals(isNsfw,game.isNsfw);
    }

    // test when isNsfw is set to true and false, and previewPic is set to empty and not empty
    // also change some values each time just in case
    static Stream<Arguments> differentCombinations() {
        return Stream.of(
                Arguments.of("grey darken-4", "This is one description that is longer than 18", "test_name", "theme", "/pic.jpg", true),
                Arguments.of("other color", "shorter than 18", "", "wario", "", false),
                Arguments.of("grey darken-4", "This is one description that is longer than 18", "test_name", "nintendo", "/pic.jpg", false),
                Arguments.of("other color", "shorter than 18", "", "other", "", true)
        );
    }
}
