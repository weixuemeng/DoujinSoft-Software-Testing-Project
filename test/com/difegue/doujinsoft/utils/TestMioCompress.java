package com.difegue.doujinsoft.utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
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
    private String originalTmpDir;
    static Path createZip(Path target, Map<String, byte[]> entries) throws IOException {
        int size = 0;
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(target))) {
            for (Map.Entry<String,byte[]> e : entries.entrySet()) {
                ZipEntry zipEntry =  new ZipEntry(e.getKey());// get entry
                zos.putNextEntry(zipEntry);
                size++;
                zos.write(e.getValue());
                zos.closeEntry();
            }
        }
        System.out.println(size);
        return target;
    }

    @BeforeEach
    public void setUp() throws IOException{
        mioCompress = new MioCompress();
        validOrig  = tempDir.resolve("orig.txt").toFile(); // create valid orig file
        String content =  "Content in orig.txt";
//        System.out.println(content.getBytes().length);  // 19 bytes
        Files.write(validOrig.toPath(), content.getBytes()); // write content to orig
        validDest = tempDir.resolve("zipedOrig.zip").toFile();
        validDesiredName = "content.txt";

        originalTmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", tempDir.toString());

    }

    @AfterEach
    void restoreTmpDir() {
        System.setProperty("java.io.tmpdir", originalTmpDir);
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
        raf.setLength(3L *1024 *1024* 1024); // 3GB
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
//        System.out.println(validDest.length());
        MioCompress.compressMio(validOrig, validDest,validDesiredName);

        // Verify ZIP file exists and is not empty
//        System.out.println(validDest.length());
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

    @Test
    public void testCompressFileInSameFolder() throws IOException{
        // Setup directory structure: tempDir/mio/games/mio.games
        File mioGamesDir = new File(tempDir.toFile(), "mio/games");
        assertTrue(mioGamesDir.mkdirs());

        // tempDir/mio/games/game.mio  1 file
        File unCompressedMio = new File(mioGamesDir, "game.mio");
        try (FileWriter writer = new FileWriter(unCompressedMio)) {
            writer.write("unCompressed content");
        }
        assertTrue(Files.exists(tempDir.resolve("mio/games/game.mio")));
        File compressedMio = new File(String.valueOf(tempDir.resolve("mio/games/game.miozip")));

//        File compressedMio = new File(mioGamesDir, "game.miozip");
//        boolean created = compressedMio.createNewFile(); // create the file (empty zip file)
        MioCompress.compressMio(unCompressedMio, compressedMio,"compressGame");
        assertTrue(compressedMio.exists()); // true

        File[] gameFiles = new File(mioGamesDir.getAbsolutePath()).listFiles(); // LENGTH 1
        assertTrue(gameFiles.length==2);

    }
    // ------------------------------uncompressed----------------------

    @Test
    public void testUncompressedNull() {
        assertThrows(NullPointerException.class, () -> MioCompress.uncompressMio(null));
    }

    @Test
    public void testUncompressedFileNotFount() {
        File fakeMio = tempDir.resolve("fake.miozip").toFile();
        assertThrows(FileNotFoundException.class, () -> MioCompress.uncompressMio(fakeMio));
    }

    @Test
    public void testEmptyZip() throws IOException{ // return null
        Path zip = createZip(tempDir.resolve("empty.zip"), Map.of());
        assertNull(MioCompress.uncompressMio(zip.toFile()));
    }

    @Test
    public void testUncompressedZipWithJustOneComponent() throws Exception {
        // file (expected) -> compressed -> uncompressed (output)
        Map map = new HashMap();
        map.put("game.mio","game1".getBytes(StandardCharsets.UTF_8));

        // zip: /var/folders/1l/3gddlk2s50g65pmtw1d8c6lc0000gn/T/junit17924487402855793825/game.miozip
        Path zip = createZip(tempDir.resolve("game.miozip"),map); // after zipped: game.miozip

        // output: /var/folders/1l/3gddlk2s50g65pmtw1d8c6lc0000gn/T/game/game.mio
        File output = MioCompress.uncompressMio(zip.toFile());

        assertNotNull(output);
        assertEquals("game.mio", output.getName());

        String expected = Files.readString(output.toPath(), StandardCharsets.UTF_8); //read component
        assertEquals("game1", expected);
    }

    @Test
    public  void multiEntry_extractsFirstOnly() throws Exception {
        Map map = new HashMap();
        map.put("game1.mio","game1".getBytes(StandardCharsets.UTF_8));
        map.put("game2.mio","game2".getBytes(StandardCharsets.UTF_8));
        Path zipFile = createZip(tempDir.resolve("games.miozip"),map);

        File output = MioCompress.uncompressMio(zipFile.toFile());//call function
        System.out.println(output.toPath().toString());

        assertEquals("game2.mio", output.getName()); //name
        String outputData = Files.readString(output.toPath(), StandardCharsets.UTF_8);
        assertEquals("game2",outputData);
//
//        Path maybeSecond = output.toPath().getParent().resolve("game2.mio");
//        assertFalse(Files.exists(maybeSecond));
    }

    @Test
    void testUnCompressedDirExistsAndCache() throws Exception {

        Path zip = createZip(tempDir.resolve("game.miozip"), Map.of("game.mio","game".getBytes())); // tempdir: /game not exist

        // out:/var/folders/1l/3gddlk2s50g65pmtw1d8c6lc0000gn/T/junit7649633234284504019/game/game.mio
        // outname: game.mio
        File out = MioCompress.uncompressMio(zip.toFile());

        assertEquals("game.mio", out.getName());
        assertEquals("game", Files.readString(out.toPath()));

        zip = createZip(tempDir.resolve("game.miozip"), Map.of("game.mio","game".getBytes())); // tempdir: /game exist, game.mio cached
        out = MioCompress.uncompressMio(zip.toFile());

        assertEquals("game.mio", out.getName());
        assertEquals("game", Files.readString(out.toPath()));
    }










}
