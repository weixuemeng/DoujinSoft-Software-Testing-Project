package com.difegue.doujinsoft;

import com.difegue.doujinsoft.utils.MioCompress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class TestDownloadServletMock {

    DownloadServlet ds;
    HttpServletRequest request;
    HttpServletResponse response;
    ServletConfig config;
    ServletContext application;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws ServletException {
        ds = new DownloadServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        config = mock(ServletConfig.class);
        application = mock(ServletContext.class);
        ds.init(config);
        when(config.getServletContext()).thenReturn(application);
        when(application.getInitParameter("dataDirectory")).thenReturn(tempDir.toString());
    }

    @AfterEach
    public void tearDown() {
        ds.destroy();
        ds = null;
    }

//    @Test
    @ParameterizedTest
    @ValueSource(strings = {"game", "manga"})
    public void testDoGetWithGameOrMangaTypeAndImageOnly(String type) throws ServletException, IOException {
        Map<String, String[]> map = new HashMap<>();
        map.put("type", new String[]{type});
        map.put("id", new String[]{"1234567890"});
        map.put("preview", new String[]{"abcd"});
        when(request.getParameterMap()).thenReturn(map);
        when(request.getParameter("type")).thenReturn(type);
        when(request.getParameter("id")).thenReturn("1234567890");
        InputStream is = mock(InputStream.class);
        ServletOutputStream os = mock(ServletOutputStream.class);
        when(application.getResourceAsStream("/img/meta.jpg")).thenReturn(is);
        when(response.getOutputStream()).thenReturn(os);
        ds.doGet(request, response);

        verify(application).getResourceAsStream("/img/meta.jpg"); // should return default image as the database/data does not exist
        verify(response).setContentType("image/jpg");
        verify(response).setCharacterEncoding("UTF-8");
        verify(response).getOutputStream();
        verify(is).transferTo(os);
    }

    @ParameterizedTest
    @ValueSource(strings = {"game", "record", "manga"})
    public void testDoGetWithDifferentTypeAndNotImageOnly(String type) throws ServletException, IOException {
        Map<String, String[]> map = new HashMap<>();
        map.put("type", new String[]{type});
        map.put("id", new String[]{"1234567890"});
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));
        Files.createDirectory(Paths.get(tempDir.toString(), "mio", type));
        Files.writeString(Paths.get(tempDir.toString(), "mio", type, "1234567890.miozip"), "random_data"); //place_holder
        try(MockedStatic<MioCompress> mc = mockStatic(MioCompress.class)) {
            File downloadFile = new File(Paths.get(tempDir.toString(), "mio", type, "1234567890.miozip").toString()); //just skip "uncompress"
            mc.when(() -> MioCompress.uncompressMio(any(File.class))).thenReturn(downloadFile);
            when(request.getParameterMap()).thenReturn(map);
            when(request.getParameter("type")).thenReturn(type);
            when(request.getParameter("id")).thenReturn("1234567890");
            ServletOutputStream os = mock(ServletOutputStream.class);
            doNothing().when(os).write(any(byte[].class), eq(0), anyInt());
            when(response.getOutputStream()).thenReturn(os);
            ds.doGet(request, response);
            verify(response).setContentType("application/octet-stream");
            verify(response).setCharacterEncoding("UTF-8");
            verify(response).setContentLength((int)downloadFile.length());
            verify(response).setHeader("Content-Disposition", String.format("attachment; filename*=UTF-8''%s",
                    URLEncoder.encode(downloadFile.getName(), "UTF-8")));
            verify(response).getOutputStream();
            verify(os, atLeast(1)).write(any(byte[].class), eq(0), anyInt());
        }
    }

    // I just duplicate the last test to test the undefined case
    // there is some redundancy, but it works
    @ParameterizedTest
    @ValueSource(strings = {"other"})
    public void testDoGetWithNotDefinedTypeAndNotImageOnly(String type) throws ServletException, IOException {
        Map<String, String[]> map = new HashMap<>();
        map.put("type", new String[]{type});
        map.put("id", new String[]{"1234567890"});
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));
        Files.createDirectory(Paths.get(tempDir.toString(), "mio", type));
        Files.writeString(Paths.get(tempDir.toString(), "mio", type, "1234567890.miozip"), "random_data"); //place_holder
        try(MockedStatic<MioCompress> mc = mockStatic(MioCompress.class)) {
            File downloadFile = new File(Paths.get(tempDir.toString(), "mio", type, "1234567890.miozip").toString()); //just skip "uncompress"
            mc.when(() -> MioCompress.uncompressMio(any(File.class))).thenReturn(downloadFile);
            when(request.getParameterMap()).thenReturn(map);
            when(request.getParameter("type")).thenReturn(type);
            when(request.getParameter("id")).thenReturn("1234567890");
            ServletOutputStream os = mock(ServletOutputStream.class);
            doNothing().when(os).write(any(byte[].class), eq(0), anyInt());
            when(response.getOutputStream()).thenReturn(os);
            ds.doGet(request, response);
            verify(response, never()).setContentType("application/octet-stream");
            verify(response, never()).setCharacterEncoding("UTF-8");
            verify(response, never()).setContentLength((int)downloadFile.length());
            verify(response, never()).setHeader("Content-Disposition", String.format("attachment; filename*=UTF-8''%s",
                    URLEncoder.encode(downloadFile.getName(), "UTF-8")));
            verify(response, never()).getOutputStream();
            verify(os, never()).write(any(byte[].class), eq(0), anyInt());
        }
    }

//    @Test
    @ParameterizedTest
    @ValueSource(strings = {"record", "other"})
    public void testDoGetWithRecordTypeOrUndefinedTypeAndImageOnly(String type) throws ServletException, IOException {
        Map<String, String[]> map = new HashMap<>();
//        map.put("type", new String[]{"game"});
        map.put("type", new String[]{type});
        map.put("id", new String[]{"1234567890"});
        map.put("preview", new String[]{"abcd"});
        when(request.getParameterMap()).thenReturn(map);
//        when(request.getParameter("type")).thenReturn("game");
        when(request.getParameter("type")).thenReturn(type);
        when(request.getParameter("id")).thenReturn("1234567890");
        when(request.getParameter("preview")).thenReturn("abcd");
        InputStream is = mock(InputStream.class);
        ServletOutputStream os = mock(ServletOutputStream.class);
        when(application.getResourceAsStream("/meta.jpg")).thenReturn(is);
        when(response.getOutputStream()).thenReturn(os);
        ds.doGet(request, response);

        verify(application).getResourceAsStream("/meta.jpg");
        verify(response).setContentType("image/jpg");
        verify(response).setCharacterEncoding("UTF-8");
        verify(response).getOutputStream();
        verify(is).transferTo(os);
    }

    @Test
    public void testDoGetWithNoTypeKey() {
        Map<String, String[]> map = new HashMap<>();
        map.put("id", new String[]{"1234567890"});
        map.put("preview", new String[]{"abcd"});
        when(request.getParameterMap()).thenReturn(map);
        when(request.getParameter("id")).thenReturn("1234567890");
        when(request.getParameter("preview")).thenReturn("abcd");
        Assertions.assertDoesNotThrow(() -> ds.doGet(request, response));

    }



}
