package test.java;
import com.difegue.doujinsoft.templates.Collection;
import com.difegue.doujinsoft.utils.CollectionUtils;
import com.difegue.doujinsoft.utils.MioCompress;
import com.difegue.doujinsoft.utils.MioStorage;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class TestMioCompress {
    //compressMio(File orig, File dest, String desiredName)
    @TempDir
    Path tempDir;
    File validOrig;
    File validDest;
    String validDesiredName;
    MioCompress mioCompress;
    @BeforeEach
    public void setUp() throws IOException{
        mioCompress = new MioCompress();
        validOrig  = tempDir.resolve("orig.txt").toFile(); // create valid orig file
        String content =  "Content in orig.txt";
//        System.out.println(content.getBytes().length);  // 19 bytes
        Files.write(validOrig.toPath(), content.getBytes()); // write content to orig
        validDest = tempDir.resolve("zipedOrig.zip").toFile();
        validDesiredName = "content.txt";

    }

//    public File createTempFile(String content) throws IOException {
//        File validOrig  = tempDir.resolve("orig.txt").toFile();
//        if (content != null) Files.write(validOrig.toPath(), content.getBytes());
//        return validOrig;
//    }

    //create valid orig

//    private Stream<Arguments> createCompressMioParams() throws IOException, NullPointerException{
//        return Stream.of(
//                Arguments.of(null, null, null, NullPointerException.class),
//                Arguments.of("fake.txt", null, null),
//                Arguments.of(null, null, null),
//                Arguments.of(null, null, null),
//                Arguments.of(null, null, null),
//                Arguments.of(null, null, null),
//                Arguments.of(null, validDest, validDesiredName, InvalidPathException.class),
//                Arguments.of(null, null, null),
//                Arguments.of(val, null, null),
//
//        );
//
//    }
    @Test
    @Tag("orig-null")
    @Tag("orig-EP")
    public void testCompressMioOrigNull()  {
        assertThrows(NullPointerException.class,
                () -> MioCompress.compressMio (null, validDest, validDesiredName));
    }
    @Test
    @Tag("orig-notexist")
    @Tag("orig-EP")
    // FileNotFoundException ( subclass of IOException)
    public void testCompressMioOrigInvalid ()  {
        File nonExistent =tempDir.resolve("fakeOrig.txt").toFile();
        assertThrows(FileNotFoundException.class,
                () -> MioCompress.compressMio (nonExistent, validDest, validDesiredName));
    }

    @Test
    @Tag("orig-Dir")
    @Tag("orig-EP")
    void testOrigIsDirectory() {
        File orig = tempDir.toFile();
        assertThrows(FileNotFoundException.class,
                () -> MioCompress.compressMio(orig, validDest, validDesiredName));
    }

    @Test
    @Tag("orig-unreadable")
    @Tag("orig-EP")
    public void testCompressMioOrigUnreadable() throws IOException {
        Path path = tempDir.resolve("unreadableOrig.txt");
        File unreadableOrig = path.toFile();
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("---------"));// value: permit  '-': forbid

        assertThrows(IOException.class,
                () -> MioCompress.compressMio(unreadableOrig, validDest, validDesiredName));
    }

    @Test
    @Tag("orig-oom")
    @DisabledOnOs(OS.WINDOWS) // Large file tests often work better on Unix-like systems
    void testOrigVeryLargeFile() throws IOException {
        File largeFile = tempDir.resolve("hugeFile.txt").toFile();

        // Create a sparse file that appears large without consuming disk space
        RandomAccessFile raf = new RandomAccessFile(largeFile, "rw");
        raf.setLength(3L * 1024 * 1024 * 1024); // 3GB
        raf.close();

        assertThrows(OutOfMemoryError.class,
                () -> MioCompress.compressMio(largeFile, validDest, validDesiredName));
    }

    @Test
    @Tag("dest-null")
    @Tag("dest-EP")
    public void testCompressMioDestNull()  {
        assertThrows(NullPointerException.class,
                () -> MioCompress.compressMio (validOrig, null, validDesiredName));
    }

    @Test
    @Tag("dest-Dir")
    @Tag("dest-EP")
    void testDestIsDirectory() {
        File invalidDest = tempDir.toFile();
        assertThrows(FileNotFoundException.class,
                () -> MioCompress.compressMio(validOrig, invalidDest, validDesiredName));
    }

    @Test
    @Tag("dest-Unwritable")
    @Tag("dest-EP")
    void testUnwritableDest() throws IOException {
        File readOnlyDest = tempDir.resolve("readOnlyDest.zip").toFile();
        readOnlyDest.setReadOnly();

        assertThrows(FileNotFoundException.class,
                () -> MioCompress.compressMio(validOrig, readOnlyDest, validDesiredName));
    }

    @Test
    @Tag("desiredName-null")
    @Tag("desiredName-EP")
    public void testCompressMioDesiredNameNull()  {
        assertThrows(NullPointerException.class,
                () -> MioCompress.compressMio (validOrig, validDest, null));
    }

    @Test
    @Tag("desiredName-length-LARGE")
    public void testDesiredName_MaxLength()  {
        // Create a 65,536-character string (1 over limit)
        String tooLongName = "a".repeat(0xFFFF + 1);  // 65,536 chars

        assertThrows(IllegalArgumentException.class,
                () -> MioCompress.compressMio(validOrig, validDest, tooLongName));
    }


    @Test
    @Tag("orig-EP")
    @Tag("dest-EP")
    @Tag("desiredName-EP")
    @Tag("desiredName-length>0")
    @Tag("Orig-readable")
    @Tag("Dest-writeable")
    void testValid() throws IOException {
        System.out.println(validDest.length());
        MioCompress.compressMio(validOrig, validDest,validDesiredName);

        // Verify ZIP file exists and is not empty
        System.out.println(validDest.length());
        assertTrue(validDest.length() > 0);

        //Verify ZIP contains the expected entry with correct content
        ZipFile zip = new ZipFile(validDest);
        ZipEntry entry = zip.getEntry(validDesiredName);
        assertNotNull(entry);

        // Verify entry content matches original file
        String zipContent = new String(zip.getInputStream(entry).readAllBytes());
        String origContent = Files.readString(validOrig.toPath());
        assertEquals(origContent, zipContent);
    }









}
