package com.difegue.doujinsoft.wc24;

import com.difegue.doujinsoft.utils.MioUtils;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.xperia64.diyedit.metadata.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;

import static com.github.stefanbirkner.systemlambda.SystemLambda.*;


import javax.servlet.ServletContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TestMailItem {
    @TempDir
    Path tempDir;

    private ServletContext mockContext;
    private Metadata testMetadata;
    MailItem mailItemMocked;
    private String templateDir;

    private LZSS mockLzss;


    @BeforeEach
    void setUp() throws Exception{
        // mock context
        mockContext = mock(ServletContext.class);
        when(mockContext.getRealPath("/WEB-INF/lzss"))
                .thenReturn(Paths.get("src/test/resources/WEB-INF/lzss").toAbsolutePath().toString());

        when(mockContext.getInitParameter("dataDirectory"))
                .thenReturn(tempDir.toString());

        // Create test DIY metatdata
        testMetadata = new Metadata("Test DIY content".getBytes());
        // mock mailitem
        mailItemMocked = mock(MailItem.class);

        // Mock LZSS
        mockLzss = mock(LZSS.class);
        doNothing().when(mockLzss).LZS_Encode(anyString(), anyString());

        // set up for render string
        templateDir = tempDir.toString();
        createTestTemplate(templateDir + "/friend_request.eml");
        createTestTemplate(templateDir + "/recap_mail.eml");
        createTestTemplate(templateDir + "/recap_mail_blue.eml");
        createTestTemplate(templateDir + "/doujinsoft_mail.eml");
        createTestTemplate(templateDir + "/game_mail.eml");
        createTestTemplate(templateDir + "/manga_mail.eml");
        createTestTemplate(templateDir + "/record_mail.eml");
    }

    private void createTestTemplate(String path) throws IOException {
        Files.writeString(Path.of(path), "Test template for {{ mail.subject }}");
    }

    @Test
    public  void testConstructor() throws  Exception{
        withEnvironmentVariable("WII_NUMBER", "1234567890")
                .and("WC24_SERVER", "https://test.wii.com")
                .execute(() -> {

                    String wiiCode = "1234123412341234"; // 16

                    // mio.lz10
                    Metadata metadata = mock(Metadata.class);
                    metadata.file = "HelloWiiData".getBytes(StandardCharsets.UTF_8);
                    // Create dummy file paths
                    Path dummyInputFile = tempDir.resolve("mio.lz10");
                    Files.write(dummyInputFile, metadata.file);

                    // /var/folders/1l/3gddlk2s50g65pmtw1d8c6lc0000gn/T/junit13729178062003612716/mio.lz10d
                    Path dummyOutputFile = tempDir.resolve("mio.lz10d");
                    byte[] compressedBytes = "COMPRESSEDDATA".getBytes(StandardCharsets.UTF_8);
                    Files.write(dummyOutputFile, compressedBytes);

                    LZSS mockLzss = mock(LZSS.class);
                    doAnswer(invocation -> {
                        String outputPath = invocation.getArgument(1);
                        System.out.println(outputPath);
                        Files.write(Paths.get(outputPath), compressedBytes);
                        return null;
                    }).when(mockLzss).LZS_Encode(anyString(), anyString());

                    try (MockedConstruction<LZSS> mockedLzss = mockConstruction(LZSS.class,
                            (lzssMock, context) -> {
                                doAnswer(invocation -> {
                                    Files.write(dummyOutputFile, compressedBytes);
                                    return null;
                                }).when(lzssMock).LZS_Encode(anyString(), anyString());
                            })) {

                        MailItem mailItem = new MailItem(wiiCode, metadata, MioUtils.Types.GAME, mockContext);

                        assertNotNull(mailItem.base64EncodedAttachment);
                        assertTrue(mailItem.base64EncodedAttachment.contains("COMPRESSEDDATA"));
                        assertEquals(1, mailItem.attachmentType);

                        // Ensure newline insertion every 76 characters (if long enough)
                        if (mailItem.base64EncodedAttachment.length() > 76) {
                            assertTrue(mailItem.base64EncodedAttachment.contains("\n"));
                        }
                    }

                });
    }


    @Test
    @Tag("initializeFromEnvironment")
    public void testDIYConstructorWithValidInput() throws Exception {

        withEnvironmentVariable("WII_NUMBER", "1234567890")
                .and("WC24_SERVER", "https://test.wii.com")
                .execute(() -> {
                    // Create a proper mock of LZSS
                    LZSS mockLzss = mock(LZSS.class);

                    // Test the constructor
                    MailItem mail = new MailItem(
                            "1234567890123456",
                            testMetadata,
                            MioUtils.Types.GAME,
                            mockContext
                    );

                    // Verify results
                    assertNotNull(mail.base64EncodedAttachment);
                    assertEquals("1234567890", mail.sender);
                    assertEquals("1234567890123456", mail.recipient);
                    assertEquals(MioUtils.Types.GAME, mail.attachmentType);

                });
    }

    @Test
    @Tag("initializeFromEnvironment")
    public void testConstructorMissingWiiNumber() throws Exception{
        withEnvironmentVariable("WC24_SERVER", "https://test.wii.com")
                .execute(() -> {
                    LZSS mockLzss = mock(LZSS.class);

                    // Stub the LZS_Encode method to simulate compression
//                    doAnswer(invocation -> {
//                        String input = invocation.getArgument(0);
//                        String output = invocation.getArgument(1);
//                        // Simulate compression by copying the file
//                        Files.copy(Path.of(input), Path.of(output));
//                        return null;
//                    }).when(mockLzss).LZS_Encode(anyString(), anyString());

                    Exception exception = assertThrows(Exception.class, () -> {
                        new MailItem(
                                "1234567890123456",
                                testMetadata,
                                MioUtils.Types.GAME,
                                mockContext
                        );
                    });
                    assertEquals("Wii sender friend number not specified. Please set the WII_NUMBER environment variable.",
                            exception.getMessage()
                    );
                });
    }


    @ParameterizedTest
    @ValueSource(strings = {"123", "abcdefghijklmnop", "123456789012345 ", "12345678901234567"
    })
    public void testConstructorRecipientCodeFalse(String code) throws Exception{
        withEnvironmentVariable("WII_NUMBER", "1234567890")
                .and("WC24_SERVER", "https://test.wii.com")
                .execute(() -> {
                    LZSS mockLzss = mock(LZSS.class);

                    Exception exception =assertThrows(Exception.class, () -> {
                        new MailItem(
                                code,
                                testMetadata,
                                MioUtils.Types.GAME,
                                mockContext
                        );
                    });
                    assertEquals("Invalid Wii Friend Code.", exception.getMessage());

                });
    }

    @Test
    public void testConstructorMissingWC24SERVERNumber() throws Exception{
        withEnvironmentVariable("WII_NUMBER", "1234567890")
                .execute(() -> {
                    LZSS mockLzss = mock(LZSS.class);

                    doAnswer(invocation -> {
                        String input = invocation.getArgument(0);
                        String output = invocation.getArgument(1);
                        // Simulate compression by copying the file
                        Files.copy(Path.of(input), Path.of(output));
                        return null;
                    }).when(mockLzss).LZS_Encode(anyString(), anyString());

                    Exception exception = assertThrows(Exception.class, () -> {
                        new MailItem(
                                "1234567890123456",
                                testMetadata,
                                MioUtils.Types.GAME,
                                mockContext
                        );
                    });
                    assertEquals("WiiConnect24 server url not specified. Please set the WC24_SERVER environment variable.",
                            exception.getMessage()
                    );
                });
    }


    @Test // pass
    public void testMailCreation_NullMetadata() {
        assertThrows(Exception.class, () ->
                new MailItem("123456789", null, 1, mockContext));
    }

    @Test // pass
    void testMailCreation_EmptyWiiCode() {
        assertThrows(Exception.class, () ->
                new MailItem("", testMetadata, 1, mockContext));
    }


    static Stream<Arguments> provideContentFor3ParameterConstructor() {
        return Stream.of(
                Arguments.of("1234567890123456", List.of("Game1"), true),
                Arguments.of("1234567890123456", List.of("Manga1", "Manga2"), false),
                Arguments.of("1234567890123456", Collections.emptyList(), true),
                Arguments.of("1234567890123456", List.of("A".repeat(100)), false)
        );
    }

    @ParameterizedTest
    @MethodSource("provideContentFor3ParameterConstructor")
    public void testConstructor3Parameter(String code, List<String> contentNames, boolean incoming) throws  Exception{
        withEnvironmentVariable("WII_NUMBER", "1234567890")
                .and("WC24_SERVER", "https://test.wii.com")
                .execute(() -> {
                    MailItem mail = new MailItem(code, contentNames, incoming);
                    if(incoming){
                        assertTrue(mail.attachmentType ==2);
                    }else{
                        assertTrue(mail.attachmentType ==1);
                    }
                    assertNotNull(mail.base64EncodedAttachment);
                    assertNotNull(mail.wiiFace);
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"This is a long message", "Multi\nline\nmessage", "Special chars: こんにちは §±!@#$%^&*()", ""})
    void testTwoParameterConstructorWithVariousMessages(String message) throws Exception {
        withEnvironmentVariable("WII_NUMBER", "1234567890")
                .and("WC24_SERVER", "https://test.wii.com")
                .execute(() -> {
                    MailItem mail = new MailItem("1234567890123456", message); // alreayd test initializeFromEnvironment, so fix code

                    assertEquals(3, mail.attachmentType);
                    assertNotNull(mail.base64EncodedAttachment);

                    // Verify can be decoded back to original message(msg->encode UTF_16BE (byte[])->base64 getEncoder -> replace)
                    // reverse the order
                    String recoverNewline = mail.base64EncodedAttachment.replace("\n", "");
                    byte[] decoded = Base64.getDecoder().decode(recoverNewline);
                    String decodedMessage = new String(decoded, StandardCharsets.UTF_16BE);
                    assertEquals(message, decodedMessage.trim());

                });
    }

    @Test
    void testOneParameterConstructorWithValidWii() throws Exception {
        withEnvironmentVariable("WII_NUMBER", "1234567890")
                .and("WC24_SERVER", "https://test.wii.com")
                .execute(() -> {
                    MailItem mail = new MailItem( "1234123412341234");
                    assertEquals(0, mail.attachmentType);

                });
    }
    @Test
    void testOneParameterConstructorWithNonvalidWii() throws Exception {
        withEnvironmentVariable("WII_NUMBER", "1234567890")
                .and("WC24_SERVER", "https://test.wii.com")
                .execute(() -> {
                    assertThrows(Exception.class, () ->
                            new MailItem(null));
                });
    }

    // --------------------renderString---------------------
    static Stream<Arguments> templateTestCases() {
        return Stream.of(
                Arguments.of(0, "friend_request.eml"),
                Arguments.of(1, "recap_mail.eml"),
                Arguments.of(2, "recap_mail_blue.eml"),
                Arguments.of(3, "doujinsoft_mail.eml"),
                Arguments.of(MioUtils.Types.GAME, "game_mail.eml"),
                Arguments.of(MioUtils.Types.MANGA, "manga_mail.eml"),
                Arguments.of(MioUtils.Types.RECORD, "record_mail.eml")
        );
    }

    @Test
    public void testRenderStringNullString() throws IOException{
        String result = mailItemMocked.renderString(null);
        assertNull(result);
    }

    @ParameterizedTest
    @MethodSource("templateTestCases")
    public void testRenderString(int type, String suffix) throws Exception {
        withEnvironmentVariable("WII_NUMBER", "1234567890")
                .and("WC24_SERVER", "https://test.wii.com")
                .execute(() -> {
                    LZSS mockLzss = mock(LZSS.class);
                    String templatePath ="template";
// remove: not working when  new PebbleEngine.Builder().build();
//                    PebbleEngine mockEngine = mock(PebbleEngine.class);
//                    PebbleTemplate mockTemplate = mock(PebbleTemplate.class);
//                    when(mockEngine.getTemplate(templatePath + "/" + suffix)).thenReturn(mockTemplate);
                    try (MockedConstruction<PebbleEngine.Builder> mockedBuilder = mockConstruction(PebbleEngine.Builder.class,
                            (builderMock, context) -> {
                                PebbleEngine mockEngine = mock(PebbleEngine.class);
                                PebbleTemplate mockTemplate = mock(PebbleTemplate.class);
                                when(mockEngine.getTemplate(templatePath + "/" + suffix)).thenReturn(mockTemplate);

                                // Important: cast the builderMock properly and stub build()
                                when(builderMock.build()).thenReturn(mockEngine);
                            })){

                        MailItem validMailItem = new MailItem(
                                "1234123412341234",
                                testMetadata,
                                type,
                                mockContext
                        );

                        String result = validMailItem.renderString(templatePath);
                        PebbleEngine.Builder actualBuilder = (PebbleEngine.Builder) mockedBuilder.constructed().get(0);
                        verify(actualBuilder).build();
//                        verify(pebbleCons.constructed().get(0)).getTemplate("template/" + suffix);
                    }

                });

    }

}
