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
        Files.write(validOrig.toPath(), "Content in orig.txt".getBytes()); // write content to orig
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
    public void testCompressMioOrigNull()  {
        assertThrows(NullPointerException.class,
                () -> MioCompress.compressMio (null, validDest, validDesiredName));
    }
    @Test
    @Tag("orig-each")
    // FileNotFoundException ( subclass of IOException)
    public void testCompressMioOrigInvalid ()  {
        File nonExistent =tempDir.resolve("fakeOrig.txt").toFile();
        assertThrows(IOException.class,
                () -> MioCompress.compressMio (nonExistent, validDest, validDesiredName));
    }

    @Test
    @Tag("orig-unreadable")
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
    @Tag("orig-valid")
    void testOrigValid() throws IOException {
        MioCompress.compressMio(validOrig, validDest,validDesiredName);

//        String zipFileName = dest.getAbsolutePath();
//        // Creates a file output stream to write to the file(zipFileName ) with the specified name.
//        // Creates a new ZIP output stream
//        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName));
//        zos.putNextEntry(new ZipEntry(desiredName));
//
//        byte[] bytes = Files.readAllBytes(orig.toPath()); // read the byte from the orig
//        zos.write(bytes, 0, bytes.length); // write to
//        zos.closeEntry();
//        zos.close();



    }





}
