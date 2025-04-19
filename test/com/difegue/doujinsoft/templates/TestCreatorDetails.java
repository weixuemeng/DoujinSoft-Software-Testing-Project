package com.difegue.doujinsoft.templates;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.mockito.Mockito.*;

public class TestCreatorDetails {
    /*
    note: CreatorDetails is never used
    since GetCreatorInfo() in TemplateBuilder.java is commented out
    this is tested for completeness purpose
     */

    public static String getCountLegitQuery(String tableName, String creatorID, String cartridgeId) {
        return "SELECT COUNT(id) FROM " + tableName + " WHERE "
                + "creatorID = '" + creatorID + "'"
                + " OR cartridgeID = '" + cartridgeId + "'";
    }

    public static String getCountNotLegitQuery(String tableName, String creatorID) {
        return "SELECT COUNT(id) FROM " + tableName + " WHERE "
                + "creatorID = '" + creatorID + "'";
    }

    Connection connection;
    Statement statement;
    ResultSet resultSet1;
    ResultSet resultSet2;
    ResultSet resultSet3;

    String setLegitCreatorAndBrandNamesQuery;
    String setNotLegitCreatorAndBrandNamesQuery;


    @BeforeEach
    public void setUp() throws SQLException {
        connection = mock(Connection.class);
        statement = mock(Statement.class);
        resultSet1 = mock(ResultSet.class);
        resultSet2 = mock(ResultSet.class);
        resultSet3 = mock(ResultSet.class);

        // Statement statement = connection.createStatement();
        when(connection.createStatement()).thenReturn(statement);

        // mocked return values for contentCount()
        when(resultSet1.getInt(1)).thenReturn(1);
        when(statement.executeQuery(getCountLegitQuery("Games", "122400003ED2DDB9", "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")))
                .thenReturn(resultSet1);
        when(statement.executeQuery(getCountLegitQuery("Manga", "122400003ED2DDB9", "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")))
                .thenReturn(resultSet1);
        when(statement.executeQuery(getCountLegitQuery("Records", "122400003ED2DDB9", "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")))
                .thenReturn(resultSet1);
        when(statement.executeQuery(getCountNotLegitQuery("Games", "122400003ED2DDB9")))
                .thenReturn(resultSet1);
        when(statement.executeQuery(getCountNotLegitQuery("Manga", "122400003ED2DDB9")))
                .thenReturn(resultSet1);
        when(statement.executeQuery(getCountNotLegitQuery("Records", "122400003ED2DDB9")))
                .thenReturn(resultSet1);

        // mocked for setCreatorAndBrandNames()
        when(resultSet3.next()).thenReturn(true, true, true, true, true, false);
        when(resultSet3.getString(1))
                .thenReturn("PYORO")
                .thenReturn("WADY")
                .thenReturn("CREATOR3")
                .thenReturn("CREATOR4")
                .thenReturn("CREATOR5");
        when(resultSet3.getString(2))
                .thenReturn("NinSoft")
                .thenReturn("Classic")
                .thenReturn("Brand3")
                .thenReturn("Brand4")
                .thenReturn("Brand5");

        setLegitCreatorAndBrandNamesQuery =
                "SELECT creator, brand FROM "
                        + "Games "
                        + "WHERE creatorID = '" + "122400003ED2DDB9" + "' " + "OR cartridgeID = '" + "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "' "

                        + "UNION "

                        + "SELECT creator, brand FROM "
                        + "Records "
                        + "WHERE creatorID = '" + "122400003ED2DDB9" + "' " + "OR cartridgeID = '" + "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "' "

                        + "UNION "

                        + "SELECT creator, brand FROM "
                        + "Manga "
                        + "WHERE creatorID = '" + "122400003ED2DDB9" + "' " + "OR cartridgeID = '" + "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "' ";

        setNotLegitCreatorAndBrandNamesQuery =
                "SELECT creator, brand FROM "
                        + "Games "
                        + "WHERE creatorID = '" + "122400003ED2DDB9" + "' "

                        + "UNION "

                        + "SELECT creator, brand FROM "
                        + "Records "
                        + "WHERE creatorID = '" + "122400003ED2DDB9" + "' "

                        + "UNION "

                        + "SELECT creator, brand FROM "
                        + "Manga "
                        + "WHERE creatorID = '" + "122400003ED2DDB9" + "' ";

        when(statement.executeQuery(setLegitCreatorAndBrandNamesQuery)).thenReturn(resultSet3);
        when(statement.executeQuery(setNotLegitCreatorAndBrandNamesQuery)).thenReturn(resultSet3);

    }

    @AfterEach
    public void tearDown() {
        connection = null;
        statement = null;
        resultSet1 = null;
        resultSet2 = null;
        resultSet3 = null;
    }

    @Test
    public void testCreatorDetailsWithLegitCart() throws SQLException {
        // mocked return values for resetCount()
        when(resultSet2.getInt(1)).thenReturn(2);
        String resetCountQuery = "SELECT COUNT(*) FROM( SELECT creatorID FROM Games WHERE cartridgeID = '" + "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" +
                "' UNION SELECT creatorID FROM Records WHERE cartridgeID = '" + "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" +
                "' UNION SELECT creatorID FROM Manga WHERE cartridgeID = '" + "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "')";
        when(statement.executeQuery(resetCountQuery)).thenReturn(resultSet2);

        CreatorDetails legitCd = new CreatorDetails(connection, "122400003ED2DDB9", "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        verify(connection, times(1)).createStatement();
        verifyNoMoreInteractions(connection);
        InOrder inOrder = inOrder(statement);
        inOrder.verify(statement, times(1)).executeQuery(getCountLegitQuery("Games", "122400003ED2DDB9", "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"));
        inOrder.verify(statement, times(1)).executeQuery(getCountLegitQuery("Manga", "122400003ED2DDB9", "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"));
        inOrder.verify(statement, times(1)).executeQuery(getCountLegitQuery("Records", "122400003ED2DDB9", "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"));
        inOrder.verify(statement, times(1)).executeQuery(resetCountQuery);
        inOrder.verify(statement, times(1)).executeQuery(setLegitCreatorAndBrandNamesQuery);
        inOrder.verify(statement, times(1)).close();
        inOrder.verifyNoMoreInteractions();

        Assertions.assertEquals("122400003ED2DDB9", legitCd.creatorId);
        Assertions.assertEquals("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", legitCd.cartridgeId);
        Assertions.assertTrue(legitCd.legitCart);
        Assertions.assertEquals(1, legitCd.totalGames);
        Assertions.assertEquals(1, legitCd.totalManga);
        Assertions.assertEquals(1, legitCd.totalRecords);
        Assertions.assertEquals(1, legitCd.timesReset);
        // two failures: 1. hashset does not guarantee order 2. toString hashset directly will insert brackets
        Assertions.assertEquals("PYORO, WADY, CREATOR3, CREATOR4, CREATOR5", legitCd.creatorNames);
        Assertions.assertEquals("NinSoft, Classic, Brand3, Brand4, Brand5", legitCd.brandNames);
    }

    @Test
    public void testCreatorDetailsWithNotLegitCart() throws SQLException {
        CreatorDetails notLegitCd = new CreatorDetails(connection, "122400003ED2DDB9", "00000000000000000000000000000000");
        verify(connection, times(1)).createStatement();
        verifyNoMoreInteractions(connection);
        InOrder inOrder = inOrder(statement);
        inOrder.verify(statement, times(1)).executeQuery(getCountNotLegitQuery("Games", "122400003ED2DDB9"));
        inOrder.verify(statement, times(1)).executeQuery(getCountNotLegitQuery("Manga", "122400003ED2DDB9"));
        inOrder.verify(statement, times(1)).executeQuery(getCountNotLegitQuery("Records", "122400003ED2DDB9"));
        inOrder.verify(statement, times(1)).executeQuery(setNotLegitCreatorAndBrandNamesQuery);
        inOrder.verify(statement, times(1)).close();
        inOrder.verifyNoMoreInteractions();

        Assertions.assertEquals("122400003ED2DDB9", notLegitCd.creatorId);
        Assertions.assertEquals("00000000000000000000000000000000", notLegitCd.cartridgeId);
        Assertions.assertFalse(notLegitCd.legitCart);
        Assertions.assertEquals(1, notLegitCd.totalGames);
        Assertions.assertEquals(1, notLegitCd.totalManga);
        Assertions.assertEquals(1, notLegitCd.totalRecords);
        Assertions.assertEquals(0, notLegitCd.timesReset);
        // two failures: 1. hashset does not guarantee order 2. toString hashset directly will insert brackets
        Assertions.assertEquals("PYORO, WADY, CREATOR3, CREATOR4, CREATOR5", notLegitCd.creatorNames);
        Assertions.assertEquals("NinSoft, Classic, Brand3, Brand4, Brand5", notLegitCd.brandNames);
    }

}
