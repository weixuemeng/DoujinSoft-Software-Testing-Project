package test.java;

import com.difegue.doujinsoft.templates.BaseMio;
import com.difegue.doujinsoft.templates.Cart;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.CsvSource;
import com.xperia64.diyedit.metadata.Metadata;
import com.difegue.doujinsoft.utils.MioUtils;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TestCart {
    HttpServletRequest request;
    Part savePart;
    @BeforeEach
    public void setUp(){
        request = mock(HttpServletRequest.class);
        savePart        = mock(Part.class);

    }
    @Test
    public void testSavePartNull() throws IOException, ServletException {
        // jsonParse: reads the input string and builds a tree of JsonElement objects
        when(request.getPart("save")).thenReturn(null);
        when(request.getParameter("recipient")).thenReturn("rec1");

        when(request.getParameter("games")).thenReturn("[]");
        when(request.getParameter("manga")).thenReturn("[]");
        when(request.getParameter("records")).thenReturn("[]");

        Cart cart = new Cart(request);
        assertNull(cart.getSaveFile()); // no drop the file
        assertEquals("rec1", cart.getRecipientCode());

        assertTrue(cart.getGames().isEmpty());
        assertTrue(cart.getManga().isEmpty());
        assertTrue(cart.getRecords().isEmpty());

    }

    @Test
    public void testSavePartNonNull() throws IOException, ServletException {
        // jsonParse: reads the input string and builds a tree of JsonElement objects
        when(request.getPart("save")).thenReturn(savePart);

        byte[] fakeSaveBytes = "fake save bytes".getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(fakeSaveBytes);
        when(savePart.getSubmittedFileName()).thenReturn("filename.bin");
        when(savePart.getInputStream()).thenReturn(inputStream);

        // test game, manga and records are json array
        String gamesJson   = "[\"game1\",\"game2\"]";
        String mangaJson   = "[\"manga1\"]";
        String recordsJson = "[\"record1\",\"record2\",\"record3\"]";
        when(request.getParameter("games")).thenReturn(gamesJson);
        when(request.getParameter("manga")).thenReturn(mangaJson);
        when(request.getParameter("records")).thenReturn(recordsJson);

        Cart cart = new Cart(request);
        assertNotNull(cart.getSaveFile()); // saveFile = File.createTempFile(fileName + System.currentTimeMillis(), "bin");
        assertEquals( JsonParser.parseString(request.getParameter("games")),cart.getGames());
        assertEquals( JsonParser.parseString(request.getParameter("manga")),cart.getManga());
        assertEquals( JsonParser.parseString(request.getParameter("records")),cart.getRecords());
        assertEquals(2, cart.getGames().size());
        assertEquals(1, cart.getManga().size());
        assertEquals(3, cart.getRecords().size());

    }

    public void testNotJsonArray() throws IOException, ServletException{
        when(request.getPart("save")).thenReturn(savePart);

        byte[] fakeSaveBytes = "fake save bytes".getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(fakeSaveBytes);
        when(savePart.getSubmittedFileName()).thenReturn("filename.bin");
        when(savePart.getInputStream()).thenReturn(inputStream);

        // test game, manga and records not json array
        String gamesJson = "\"gamejson\"";
        String mangaJson   = "\"mangajson\"";
        String recordsJson = "\"recordsjson\"";

        when(request.getParameter("games")).thenReturn(gamesJson);
        when(request.getParameter("manga")).thenReturn(mangaJson);
        when(request.getParameter("records")).thenReturn(recordsJson);


        Cart cart = new Cart(request);
        verify(request, times(2)).getPart("save");

        // JsonElement a = JsonParser.parseString(request.getParameter("games"));
        JsonElement a = JsonParser.parseString(request.getParameter("games"));
        assertNull(cart.getGames());
        assertNull(cart.getManga());
        assertNull(cart.getRecords());
    }


}
