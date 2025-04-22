package com.difegue.doujinsoft.utils;

import com.difegue.doujinsoft.wc24.MailItemParser;
import com.difegue.doujinsoft.wc24.WiiConnect24Api;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

public class TestServerInit {

    ServerInit serverInit;
    ServletContextEvent event;
    ServletContext application;
    ScheduledExecutorService scheduler;

    @TempDir
    Path tempDir;


    @BeforeEach
    public void init() {
        serverInit = new ServerInit();
        event = Mockito.mock(ServletContextEvent.class);
        application = Mockito.mock(ServletContext.class);
        scheduler = mock(ScheduledExecutorService.class);

        when(event.getServletContext()).thenReturn(application);
        when(application.getInitParameter("dataDirectory"))
                .thenReturn(tempDir.toString());
    }

    @AfterEach
    public void tearDown() {
        serverInit = null;
        event = null;
        application = null;
        scheduler = null;
    }

    @Test
    public void testContextInitializedWithMailFileAndNoMioFolder() throws IOException {
        Files.writeString(tempDir.resolve("mails.wc24"), "random_mail"); // when it exists
        try (
                MockedConstruction<MailItemParser> MailItemParser = mockConstruction(MailItemParser.class,
                        (parsers, ctx) -> doNothing().when(parsers).consumeEmails(anyString()));
                MockedStatic<Executors> executors = mockStatic(Executors.class)
        ) {
            executors.when(Executors::newSingleThreadScheduledExecutor).thenReturn(scheduler);

            serverInit.contextInitialized(event);

            List<MailItemParser> parsers = MailItemParser.constructed();
            Assertions.assertEquals(1, parsers.size());
            MailItemParser parser = parsers.get(0);

            InOrder inOrder = inOrder(event, application, scheduler, parser);
            inOrder.verify(event, times(1)).getServletContext();
            inOrder.verify(application, times(1)).getInitParameter("dataDirectory");
            inOrder.verify(parser, times(1)).consumeEmails(anyString());
            inOrder.verify(scheduler, times(1)).scheduleAtFixedRate(any(Runnable.class), eq((long) 0), eq((long) 1), eq(TimeUnit.HOURS));

            Assertions.assertTrue(Files.exists(tempDir.resolve("mioDatabase.sqlite")));
            Assertions.assertTrue(Files.exists(Paths.get(tempDir.toString(), "mio")));

            // MioStorage.ScanForNewMioFiles(dataDir, SQLog), but no mio file, so no folder should be created
            Assertions.assertFalse(Files.exists(Paths.get(tempDir.toString(), "mio", "game")));
            Assertions.assertFalse(Files.exists(Paths.get(tempDir.toString(), "mio", "manga")));
            Assertions.assertFalse(Files.exists(Paths.get(tempDir.toString(), "mio", "record")));
            Assertions.assertFalse(Files.exists(tempDir.resolve("mails.wc24"))); //deleted
        }
    }

    @Test
    public void testContextInitializedWithNoMailFileAndNoMioFolder() {
        try (
                MockedConstruction<MailItemParser> MailItemParser = mockConstruction(MailItemParser.class,
                        (parsers, ctx) -> doNothing().when(parsers).consumeEmails(anyString()));
                MockedStatic<Executors> executors = mockStatic(Executors.class)
        ) {
            executors.when(Executors::newSingleThreadScheduledExecutor).thenReturn(scheduler);

            serverInit.contextInitialized(event);

            List<MailItemParser> parsers = MailItemParser.constructed();
            Assertions.assertEquals(0, parsers.size()); // no mail file so no parser

            InOrder inOrder = inOrder(event, application, scheduler);
            inOrder.verify(event, times(1)).getServletContext();
            inOrder.verify(application, times(1)).getInitParameter("dataDirectory");
            inOrder.verify(scheduler, times(1)).scheduleAtFixedRate(any(Runnable.class), eq((long) 0), eq((long) 1), eq(TimeUnit.HOURS));

            Assertions.assertTrue(Files.exists(tempDir.resolve("mioDatabase.sqlite")));
            Assertions.assertTrue(Files.exists(Paths.get(tempDir.toString(), "mio")));

            // MioStorage.ScanForNewMioFiles(dataDir, SQLog), but no mio file, so no folder should be created
            Assertions.assertFalse(Files.exists(Paths.get(tempDir.toString(), "mio", "game")));
            Assertions.assertFalse(Files.exists(Paths.get(tempDir.toString(), "mio", "manga")));
            Assertions.assertFalse(Files.exists(Paths.get(tempDir.toString(), "mio", "record")));

            Assertions.assertFalse(Files.exists(tempDir.resolve("mails.wc24"))); //deleted
        }
    }

    @Test
    public void testContextInitializedWithMailFileAndMioFolder() throws IOException {
        Files.writeString(tempDir.resolve("mails.wc24"), "random_mail"); // when it exists
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));

        try (
                MockedConstruction<MailItemParser> MailItemParser = mockConstruction(MailItemParser.class,
                        (parsers, ctx) -> doNothing().when(parsers).consumeEmails(anyString()));
                MockedStatic<Executors> executors = mockStatic(Executors.class)
        ) {
            executors.when(Executors::newSingleThreadScheduledExecutor).thenReturn(scheduler);

            serverInit.contextInitialized(event);

            List<MailItemParser> parsers = MailItemParser.constructed();
            Assertions.assertEquals(1, parsers.size());
            MailItemParser parser = parsers.get(0);

            InOrder inOrder = inOrder(event, application, scheduler, parser);
            inOrder.verify(event, times(1)).getServletContext();
            inOrder.verify(application, times(1)).getInitParameter("dataDirectory");
            inOrder.verify(parser, times(1)).consumeEmails(anyString());
            inOrder.verify(scheduler, times(1)).scheduleAtFixedRate(any(Runnable.class), eq((long) 0), eq((long) 1), eq(TimeUnit.HOURS));

            Assertions.assertTrue(Files.exists(tempDir.resolve("mioDatabase.sqlite")));
            Assertions.assertTrue(Files.exists(Paths.get(tempDir.toString(), "mio"))); //should keep

            // MioStorage.ScanForNewMioFiles(dataDir, SQLog), but no mio file, so no folder should be created
            Assertions.assertFalse(Files.exists(Paths.get(tempDir.toString(), "mio", "game")));
            Assertions.assertFalse(Files.exists(Paths.get(tempDir.toString(), "mio", "manga")));
            Assertions.assertFalse(Files.exists(Paths.get(tempDir.toString(), "mio", "record")));

            Assertions.assertFalse(Files.exists(tempDir.resolve("mails.wc24"))); //deleted
        }
    }

    // note: mockito-inline can't mock native static methods (like Class.forName("org.sqlite.JDBC"))
    // so line 58 coverage is currently skipped
    // may consider trying other mock tools later
    @Test
    public void testContextInitializedWithClassNotFoundException() {

    }


    @Test
    public void testRun() throws IOException {
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        Files.writeString(tempDir.resolve("mails.wc24"), "random_mail"); // when it exists
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));

        try (
                MockedConstruction<MailItemParser> MailItemParser = mockConstruction(MailItemParser.class,
                        (parsers, ctx) -> doNothing().when(parsers).consumeEmails(anyString()));
                MockedStatic<Executors> executors = mockStatic(Executors.class);
                MockedConstruction<WiiConnect24Api> wiiConnect24Api = mockConstruction(WiiConnect24Api.class,
                        (api, ctx) -> {
                            when(api.receiveMails()).thenReturn("random_mail");
                            doNothing().when(api).deleteMails();
                        })
        ) {
            executors.when(Executors::newSingleThreadScheduledExecutor).thenReturn(scheduler);
            when(scheduler.scheduleAtFixedRate(runnableCaptor.capture(), eq(0L), eq(1L), eq(TimeUnit.HOURS)))
                    .thenReturn(null);

            serverInit.contextInitialized(event);
            Runnable runnable = runnableCaptor.getValue();
            runnable.run();
            List<WiiConnect24Api> apis = wiiConnect24Api.constructed();
            Assertions.assertEquals(1, apis.size());
            WiiConnect24Api api = apis.get(0);
            InOrder inOrder = inOrder(api);
            inOrder.verify(api, times(1)).receiveMails();
            inOrder.verify(api, times(1)).deleteMails();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // line 174 coverage
    @Test
    public void testRunWithException() throws IOException {
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        Files.writeString(tempDir.resolve("mails.wc24"), "random_mail"); // when it exists
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));

        try (
                MockedConstruction<MailItemParser> MailItemParser = mockConstruction(MailItemParser.class,
                        (parsers, ctx) -> doNothing().when(parsers).consumeEmails(anyString()));
                MockedStatic<Executors> executors = mockStatic(Executors.class);
                MockedConstruction<WiiConnect24Api> wiiConnect24Api = mockConstruction(WiiConnect24Api.class,
                        (api, ctx) -> when(api.receiveMails()).thenThrow(new RuntimeException("random_error")))
        ) {
            executors.when(Executors::newSingleThreadScheduledExecutor).thenReturn(scheduler);
            when(scheduler.scheduleAtFixedRate(runnableCaptor.capture(), eq(0L), eq(1L), eq(TimeUnit.HOURS)))
                    .thenReturn(null);

            serverInit.contextInitialized(event);
            Runnable runnable = runnableCaptor.getValue();
            Assertions.assertDoesNotThrow(runnable::run);

            List<WiiConnect24Api> apis = wiiConnect24Api.constructed();
            Assertions.assertEquals(1, apis.size());
            WiiConnect24Api api = apis.get(0);
            verify(api, times(1)).receiveMails();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testContextDestroyed() throws IOException {
        when(scheduler.shutdownNow()).thenReturn(null);

        Files.writeString(tempDir.resolve("mails.wc24"), "random_mail");
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));

        try (
                MockedStatic<Executors> executors = mockStatic(Executors.class)
        ) {
            executors.when(Executors::newSingleThreadScheduledExecutor).thenReturn(scheduler);

            serverInit.contextInitialized(event);
            serverInit.contextDestroyed(event);
            verify(scheduler, times(1)).shutdownNow();
        }
    }

    // note: line 142 coverage
    @Test
    public void testConnectionThrow() {
        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class);
             MockedStatic<Executors> executors = mockStatic(Executors.class)
        ) {
            SQLException e = new SQLException("out of memory");
            driverManager.when(() -> DriverManager.getConnection("jdbc:sqlite:" + tempDir + "/mioDatabase.sqlite")).thenThrow(e);
            executors.when(Executors::newSingleThreadScheduledExecutor).thenReturn(scheduler);
            Assertions.assertDoesNotThrow(() -> serverInit.contextInitialized(event));
        }
    }
}
