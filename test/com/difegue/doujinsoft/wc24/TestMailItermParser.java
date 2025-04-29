package com.difegue.doujinsoft.wc24;

import com.difegue.doujinsoft.utils.MioStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TestMailItermParser {


    static String getMailData(String fromWiiCode, String subject) throws MessagingException {

        MimeMessage msg = new MimeMessage(Session.getInstance(new Properties()));
        msg.setSubject(subject);
        msg.setFrom("w" + fromWiiCode + "@test.com");
        msg.setText("random");
        msg.saveChanges();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            msg.writeTo(bos);
            return "delimiter" + "\nContent-Type: text/plain\n" + bos;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String getMailData(String fromWiiCode, String subject, String textBodyPart) throws MessagingException {

        MimeMessage msg = new MimeMessage(Session.getInstance(new Properties()));
        msg.setSubject(subject);
        msg.setFrom("w" + fromWiiCode + "@test.com");
        msg.setText(textBodyPart);
        msg.saveChanges();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            msg.writeTo(bos);
            return "delimiter" + "\nContent-Type: text/plain\n" + bos;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String getMailData(String fromWiiCode, String subject, String textBodyPart, byte[] binaryBodyPart) throws MessagingException {
        MimeMessage msg = new MimeMessage(Session.getInstance(new Properties()));
        msg.setSubject(subject);
        msg.setFrom("w" + fromWiiCode + "@test.com");

        MimeBodyPart text = new MimeBodyPart();
        text.setText(textBodyPart);

        MimeBodyPart bin = new MimeBodyPart();
        bin.setContent(binaryBodyPart, "application/octet-stream");

        Multipart mp = new MimeMultipart();
        mp.addBodyPart(text);
        mp.addBodyPart(bin);
        msg.setContent(mp);
        msg.saveChanges();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            msg.writeTo(bos);
            return "delimiter" + "\nContent-Type: text/plain\n" + bos;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @TempDir
    Path tempDir;

    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "123456")
    public void testMailItemParserWithFallBackNumber() {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn("path"); // here does not need a real value
        Assertions.assertDoesNotThrow(() -> new MailItemParser(application));
    }

    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
//    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "123456")
    public void testMailItemParserWithNoFallBackNumber() {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn("path");
//        Assertions.assertDoesNotThrow(() -> new MailItemParser(application));
        Assertions.assertThrows(Exception.class, () -> new MailItemParser(application));
    }

    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "123456")
    public void testConsumeEmails() throws Exception {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn(tempDir.toString());
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));
        try (MockedConstruction<WiiConnect24Api> api = Mockito.mockConstruction(WiiConnect24Api.class, (apis, context1) -> {
            when(apis.sendMails(any())).thenReturn("mock_output");
        })) {
            MailItemParser parser = new MailItemParser(application);
            parser.consumeEmails(getMailData("1234567890123456", "WC24 Cmd Message"));
            List<WiiConnect24Api> apis = api.constructed();
            Assertions.assertEquals(1, apis.size());
            verify(apis.get(0), times(1)).sendMails(any());
        }
    }

    // Mail from unregistered Friend Code - Skipping
    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "123456")
    public void testConsumeEmails2() throws Exception {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn(tempDir.toString());
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));
        try (MockedConstruction<WiiConnect24Api> api = Mockito.mockConstruction(WiiConnect24Api.class, (apis, context1) -> {
            when(apis.sendMails(any())).thenReturn("mock_output");
        })) {
            MailItemParser parser = new MailItemParser(application);
            parser.consumeEmails(getMailData("1234567890123456", "random mail subject"));
            List<WiiConnect24Api> apis = api.constructed();
            Assertions.assertEquals(1, apis.size());
            verify(apis.get(0), times(1)).sendMails(any());
        }
    }

    // Invalid Wii Friend Code
    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "123456")
    public void testConsumeEmails3() throws Exception {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn(tempDir.toString());
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));
        try (MockedConstruction<WiiConnect24Api> api = Mockito.mockConstruction(WiiConnect24Api.class, (apis, context1) -> {
            when(apis.sendMails(any())).thenReturn("mock_output");
        })) {
            MailItemParser parser = new MailItemParser(application);
            parser.consumeEmails(getMailData("1234567890", "WC24 Cmd Message"));
            List<WiiConnect24Api> apis = api.constructed();
            Assertions.assertEquals(1, apis.size());
            verify(apis.get(0), times(1)).sendMails(any());
        }
    }

    // cover if (content.contains("This part is ignored"))
    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "123456")
    public void testConsumeEmails4() throws Exception {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn(tempDir.toString());
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));
        try (MockedConstruction<WiiConnect24Api> api = Mockito.mockConstruction(WiiConnect24Api.class, (apis, context1) -> {
            when(apis.sendMails(any())).thenReturn("mock_output");
        })) {
            MailItemParser parser = new MailItemParser(application);
            parser.consumeEmails(getMailData("1234567890123456", "WC24 Cmd Message", "This part is ignored"));
            List<WiiConnect24Api> apis = api.constructed();
            Assertions.assertEquals(1, apis.size());
            verify(apis.get(0), times(1)).sendMails(any());
        }
    }

    // cover SQLException
    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "123456")
    public void testConsumeEmails5() throws Exception {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn(tempDir.toString());
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));
        try (MockedConstruction<WiiConnect24Api> api = Mockito.mockConstruction(WiiConnect24Api.class, (apis, context1) -> {
            when(apis.sendMails(any())).thenReturn("mock_output");
        }); MockedStatic<MioStorage> ms = Mockito.mockStatic(MioStorage.class)) {
            ms.when(() -> MioStorage.ScanForNewMioFiles(any(), any())).thenThrow(new SQLException());
            MailItemParser parser = new MailItemParser(application);
            parser.consumeEmails(getMailData("1234567890123456", "WC24 Cmd Message"));
            List<WiiConnect24Api> apis = api.constructed();
            Assertions.assertEquals(1, apis.size());
            verify(apis.get(0), times(1)).sendMails(any());
        }
    }

    // cover sendMails() exception
    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "123456")
    public void testConsumeEmails6() throws Exception {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn(tempDir.toString());
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));
        try (MockedConstruction<WiiConnect24Api> api = Mockito.mockConstruction(WiiConnect24Api.class, (apis, context1) -> {
            when(apis.sendMails(any())).thenThrow(new RuntimeException("mock exception"));
        })) {
            MailItemParser parser = new MailItemParser(application);
            parser.consumeEmails(getMailData("1234567890123456", "WC24 Cmd Message"));
        }
    }


    // cover wiicode null
    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "123456")
    public void testConsumeEmails7() throws Exception {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn(tempDir.toString());
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));
        try (MockedConstruction<WiiConnect24Api> api = Mockito.mockConstruction(WiiConnect24Api.class, (apis, context1) -> {
            when(apis.sendMails(any())).thenReturn("mock_output");
        })) {
            MailItemParser parser = new MailItemParser(application);
            parser.consumeEmails(getMailData(null, "WC24 Cmd Message"));
            List<WiiConnect24Api> apis = api.constructed();
            Assertions.assertEquals(1, apis.size());
            verify(apis.get(0), times(1)).sendMails(any());
        }
    }

    // cover subject null
    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "123456")
    public void testConsumeEmails8() throws Exception {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn(tempDir.toString());
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));
        try (MockedConstruction<WiiConnect24Api> api = Mockito.mockConstruction(WiiConnect24Api.class, (apis, context1) -> {
            when(apis.sendMails(any())).thenReturn("mock_output");
        })) {
            MailItemParser parser = new MailItemParser(application);
            parser.consumeEmails(getMailData("1234567890123456", null));
            List<WiiConnect24Api> apis = api.constructed();
            Assertions.assertEquals(1, apis.size());
            verify(apis.get(0), times(1)).sendMails(any());
        }
    }

    // cover isFriendCodeSaved TRUE
    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "1234567890123456")
    public void testConsumeEmails9() throws Exception {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn(tempDir.toString());
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement ret = Mockito.mock(PreparedStatement.class);
        ResultSet result = Mockito.mock(ResultSet.class);
        when(connection.prepareStatement(any())).thenReturn(ret);
        when(ret.executeQuery()).thenReturn(result);
        when(result.getInt(1)).thenReturn(1);
//        when(connection.prepareStatement(any())).thenThrow(new SQLException()); // will use this later
        try (MockedConstruction<WiiConnect24Api> api = Mockito.mockConstruction(WiiConnect24Api.class, (apis, context) -> {
            when(apis.sendMails(any())).thenReturn("mock_output");
        }); MockedStatic<DriverManager> driverManager = Mockito.mockStatic(DriverManager.class);) {
            driverManager.when(() -> DriverManager.getConnection("jdbc:sqlite:" + tempDir + "/mioDatabase.sqlite")).thenReturn(connection);
            MailItemParser parser = new MailItemParser(application);
            parser.consumeEmails(getMailData("1234567890123456", "WC24 Cmd Message"));
            List<WiiConnect24Api> apis = api.constructed();
            Assertions.assertEquals(1, apis.size());
            verify(apis.get(0), times(1)).sendMails(any());
        }
    }

    // cover isFriendCodeSaved FALSE
    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "1234567890123456")
    public void testConsumeEmails9_2() throws Exception {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn(tempDir.toString());
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement ret = Mockito.mock(PreparedStatement.class);
        ResultSet result = Mockito.mock(ResultSet.class);
        when(connection.prepareStatement(any())).thenReturn(ret);
        when(ret.executeQuery()).thenReturn(result);
        when(result.getInt(1)).thenReturn(0);
//        when(connection.prepareStatement(any())).thenThrow(new SQLException()); // will use this later
        try (MockedConstruction<WiiConnect24Api> api = Mockito.mockConstruction(WiiConnect24Api.class, (apis, context) -> {
            when(apis.sendMails(any())).thenReturn("mock_output");
        }); MockedStatic<DriverManager> driverManager = Mockito.mockStatic(DriverManager.class);) {
            driverManager.when(() -> DriverManager.getConnection("jdbc:sqlite:" + tempDir + "/mioDatabase.sqlite")).thenReturn(connection);
            MailItemParser parser = new MailItemParser(application);
            parser.consumeEmails(getMailData("1234567890123456", "WC24 Cmd Message"));
            List<WiiConnect24Api> apis = api.constructed();
            Assertions.assertEquals(1, apis.size());
            verify(apis.get(0), times(1)).sendMails(any());
        }
    }

    // survey
    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "12345667890123456")
    public void testConsumeEmails10() throws Exception {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn(tempDir.toString());
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));
        try (MockedConstruction<WiiConnect24Api> api = Mockito.mockConstruction(WiiConnect24Api.class, (apis, context1) -> {
            when(apis.sendMails(any())).thenReturn("mock_output");
        })) {
            MailItemParser parser = new MailItemParser(application);

            byte[] surveyData = new byte[28];
            String surveyName = "test survey title aaaaaaaaaaaaaaaaaa";
            byte[] nameBytes = surveyName.getBytes(UTF_8);
            System.arraycopy(nameBytes, 0, surveyData, 0, Math.min(nameBytes.length, 24));
            byte type = 0; // Game
            byte stars = 4;
            byte comment = 1;
            surveyData[25] = type;
            surveyData[26] = stars;
            surveyData[27] = comment;

            parser.consumeEmails(getMailData("1234567890123456", "QUESTION", "Survey", surveyData));
            List<WiiConnect24Api> apis = api.constructed();
            Assertions.assertEquals(1, apis.size());
            verify(apis.get(0), times(1)).sendMails(any());
        }
    }

    // survey with no title
    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "12345667890123456")
    public void testConsumeEmails11() throws Exception {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn(tempDir.toString());
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));
        try (MockedConstruction<WiiConnect24Api> api = Mockito.mockConstruction(WiiConnect24Api.class, (apis, context1) -> {
            when(apis.sendMails(any())).thenReturn("mock_output");
        })) {
            MailItemParser parser = new MailItemParser(application);

            byte[] surveyData = new byte[28];
            Arrays.fill(surveyData, 0, 24, (byte) ' ');
            byte type = 0; // Game
            byte stars = 4;
            byte comment = 1;
            surveyData[25] = type;
            surveyData[26] = stars;
            surveyData[27] = comment;

            parser.consumeEmails(getMailData("1234567890123456", "QUESTION", "Survey", surveyData));
            List<WiiConnect24Api> apis = api.constructed();
            Assertions.assertEquals(1, apis.size());
            verify(apis.get(0), times(1)).sendMails(any());
        }
    }

    // mio file
    // could be improved further
    @Test
    @SetEnvironmentVariable(key = "WII_NUMBER", value = "1234567890")
    @SetEnvironmentVariable(key = "WC24_SERVER", value = "test.com")
    @SetEnvironmentVariable(key = "WC24_PASSWORD", value = "password")
    @SetEnvironmentVariable(key = "WC24_DEBUG", value = "true")
    @SetEnvironmentVariable(key = "WII_FALLBACK", value = "12345667890123456")
    public void testConsumeEmails12() throws Exception {
        ServletContext application = Mockito.mock(ServletContext.class);
        when(application.getInitParameter("dataDirectory")).thenReturn(tempDir.toString());
        Files.createDirectory(Paths.get(tempDir.toString(), "mio"));
        try (MockedConstruction<WiiConnect24Api> api = Mockito.mockConstruction(WiiConnect24Api.class, (apis, context1) -> {
            when(apis.sendMails(any())).thenReturn("mock_output");
        })) {
            MailItemParser parser = new MailItemParser(application);

            parser.consumeEmails(getMailData("1234567890123456", "G", "Game"));
            List<WiiConnect24Api> apis = api.constructed();
            Assertions.assertEquals(1, apis.size());
            verify(apis.get(0), times(1)).sendMails(any());
        }
    }

}
