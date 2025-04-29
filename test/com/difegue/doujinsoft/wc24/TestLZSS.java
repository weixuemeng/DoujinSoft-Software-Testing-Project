package com.difegue.doujinsoft.wc24;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TestLZSS {
    @TempDir
    Path tempDir;
    private LZSS lzss;
    ServletContext mockContext;
    private static Path BIN_DIR = Paths.get("src/test/resources/WEB-INF/lzss");
    private Runtime runtimeMocked;
    private MockedStatic<Runtime> runtimeStatic;
    private static final Path BIN_SRC = Paths.get("src/test/resources/bin");


    File input;
    File output;


    private File createTestFile(String content) throws IOException {
        File file = tempDir.resolve("test.txt").toFile(); // create path
        Files.writeString(file.toPath(), content); // write text to file
        return file;
    }

    private ServletContext stubCtx() {
        ServletContext ctx = mock(ServletContext.class);
        when(ctx.getRealPath("/WEB-INF/lzss"))
                .thenReturn(BIN_DIR.toString());
        return ctx;
    }


    @BeforeEach
    public void setUp() throws  Exception{
        // mocl context
        lzss = new LZSS(stubCtx());

        // mock runtime
        runtimeMocked = mock(Runtime.class);
        runtimeStatic = mockStatic(Runtime.class);
        runtimeStatic.when(Runtime::getRuntime).thenReturn(runtimeMocked);
        when(runtimeMocked.exec(anyString())).thenReturn(mock(Process.class));

        // create file
        input = createTestFile("Test content");
        output = tempDir.resolve("output.bin").toFile();
    }

    @AfterEach
    void tearDown() {
        runtimeStatic.close();
    }

    @Test
    void testEncodeWindowsCallsExe() throws Exception {
        System.setProperty("os.name", "Windows 11");

        String expected =
                BIN_DIR.toString() + "\\gbalzss.exe e "
                        + input.getAbsolutePath() + " "
                        + output.getAbsolutePath();

        lzss.LZS_Encode(input.getAbsolutePath(), output.getAbsolutePath());
// src/test/resources/WEB-INF/lzss
        verify(runtimeMocked)
                .exec(expected);
    }

    @Test
    void testEncodeLinuxCallsChmodAndCmd() throws Exception {
        System.setProperty("os.name", "Linux");

        lzss.LZS_Encode(input.getAbsolutePath(), output.getAbsolutePath());

        String chmodCmd = "chmod +x " + BIN_DIR.toString() + "/gbalzss";
        String compressCmd = BIN_DIR.toString() + "/gbalzss e " +
                input.getAbsolutePath() + " " +
                output.getAbsolutePath();
        verify(runtimeMocked).exec(chmodCmd);
        verify(runtimeMocked).exec(compressCmd);
    }
    // blackbox don't know how to do??
//    @Test
//    void testLZSEncodeWithValidFile() throws Exception {
//
//        lzss.LZS_Encode(input.getAbsolutePath(), output.getAbsolutePath());
//
//        assertTrue(output.exists());
//        assertTrue(output.length() > 0);
//    }

    @Test
    void testLZSEncodeWithNullInput() { // ? not throw?

        // Test with null input filename
        Exception exception = assertThrows(Exception.class,
                () -> lzss.LZS_Encode(null, output.getAbsolutePath()));

        // Verify it fails with some kind of exception (NPE gets wrapped)
        assertTrue(exception.getCause() instanceof NullPointerException ||
                exception instanceof NullPointerException);

        // Test with null output filename
        exception = assertThrows(Exception.class,
                () -> lzss.LZS_Encode(input.getAbsolutePath(), null));

        assertTrue(exception.getCause() instanceof NullPointerException ||
                exception instanceof NullPointerException);


    }
    @Test
    void testLZSEncodeWithNullOutput() throws Exception {

    }
    @Test
    void testLZSEncodeWithBothNull() throws Exception {

    }

    @Test
    void testDecodeWindowsCallsExe() throws Exception {
        System.setProperty("os.name", "Windows 11");

        String expected = BIN_DIR.toString() + "\\gbalzss.exe e "
                        + input.getAbsolutePath() + " "
                        + output.getAbsolutePath();

        lzss.LZS_Decode(input.getAbsolutePath(), output.getAbsolutePath());
// src/test/resources/WEB-INF/lzss
        verify(runtimeMocked).exec(expected);
    }

    @Test
    void testDecodeLinuxCallsChmodAndCmd() throws Exception {
        System.setProperty("os.name", "Linux");

        lzss.LZS_Decode(input.getAbsolutePath(), output.getAbsolutePath());

        String chmodCmd = "chmod +x " + BIN_DIR.toString() + "/gbalzss";
        String compressCmd = BIN_DIR.toString() + "/gbalzss e " +
                input.getAbsolutePath() + " " +
                output.getAbsolutePath();
        verify(runtimeMocked).exec(chmodCmd);
        verify(runtimeMocked).exec(compressCmd);

    }

//    @Test
//    void roundTrip_success() throws Exception {
//        // prepare data "raw.bin"
//        Path src = tempDir.resolve("raw.bin");
//        Files.write(src, "Hello LZSS!".getBytes());
//        System.out.println(System.getProperty("os.name").toLowerCase());
//
//        // /temp/raw.lzs
//        Path encoded = tempDir.resolve("raw.lzs");
//        Path decoded = tempDir.resolve("raw_dec.bin");
//
//        // consturct ServletContext stub 指向真实二进制目录
//        Path binDir = Paths.get("src/test/resources/WEB-INF/lzss");
//        assumeTrue(Files.exists(binDir.resolve(osExeName())), "gbalzss missing");
//
//        LZSS lzss = new LZSS(new StubContext(binDir));
//        lzss.LZS_Encode(src.toString(), encoded.toString());
//        lzss.LZS_Decode(encoded.toString(), decoded.toString());
//
//        assertArrayEquals(rawBytes, Files.readAllBytes(decoded));
//    }

    private String osExeName() {
        return System.getProperty("os.name").toLowerCase().contains("win")
                ? "gbalzss.exe" : "gbalzss";
    }


}
