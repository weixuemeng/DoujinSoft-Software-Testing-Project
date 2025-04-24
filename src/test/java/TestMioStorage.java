package test.java;
import com.difegue.doujinsoft.utils.MioCompress;
import com.difegue.doujinsoft.utils.MioStorage;
import com.difegue.doujinsoft.utils.MioUtils;
import com.xperia64.diyedit.FileByteOperations;
import com.xperia64.diyedit.editors.GameEdit;
import com.xperia64.diyedit.metadata.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import javax.swing.plaf.metal.MetalTabbedPaneUI;

import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class TestMioStorage {
    @TempDir
    Path tempDir;
    MioStorage mioStorage;
    Logger logger;

    Connection connectionMocked;
    PreparedStatement preparedStatementMocked;
    Metadata mockMeta;

    Path dataDir;
    Path mioDir;
    Path file;

    @BeforeEach
    public void setUp() throws SQLException, IOException {
        mioStorage = new MioStorage();
        logger = mock(Logger.class);

        connectionMocked = mock(Connection.class);
        preparedStatementMocked = mock(PreparedStatement.class);
        when(connectionMocked.prepareStatement(anyString())).thenReturn(preparedStatementMocked);

        mockMeta = mock(Metadata.class);
        when(mockMeta.getName()).thenReturn("MetaName!");
        when(mockMeta.getCreator()).thenReturn("MetaCreator");
        when(mockMeta.getBrand()).thenReturn("MetaBrand");
        when(mockMeta.getDescription()).thenReturn("MetaDescription");
        when(mockMeta.getTimestamp()).thenReturn(100);

        dataDir = Files.createDirectories(tempDir.resolve("data"));
        mioDir  = Files.createDirectories(dataDir.resolve("mio"));
        file    = mioDir.resolve("file.mio");
    }

    public String computeMD5Hash(String input){
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Convert input string to bytes (UTF-8 encoding)
            byte[] inputBytes = input.getBytes("UTF-8");

            // Compute the MD5 hash
            byte[] hashBytes = md.digest(inputBytes);

            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            return hexString.toString();
        }catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
    }

    public static String byteToHex(byte[] byteData) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : byteData) {
            String hex = String.format("%02x", b);
            stringBuilder.append(hex);
        }
        return stringBuilder.toString();
    }

    @Test
    @Tag("computeMioHash-EP")
    void testNullInput() {
        assertThrows(NullPointerException.class,
                () -> mioStorage.computeMioHash(null));
    }

    @Test
    @Tag("computeMioHash-EP")
    void testEmptyInput() {
        byte[] input = "".getBytes(StandardCharsets.UTF_8);
        String hash = mioStorage.computeMioHash( input);
        String expected = computeMD5Hash("");
        assertTrue(expected.equals(hash));
    }

    @Test
    @Tag("computeMioHash-EP")
    void testValidInput() {
        byte[] input = "sfssef".getBytes(StandardCharsets.UTF_8);
        String hash = mioStorage.computeMioHash( input);
        String expected = computeMD5Hash("sfssef");
//        System.out.println(hash);
//        System.out.println(expected);
        assertTrue(expected.equals(hash));
    }

    @Test
    void testNoSuchAlgorithmException() throws  Exception{
        try (var mocked = mockStatic(MessageDigest.class)) {
            // Force the exception when getInstance is called
            mocked.when(() -> MessageDigest.getInstance("MD5"))
                    .thenThrow(new NoSuchAlgorithmException());

            byte[] data = "sfssef".getBytes();
            String result = mioStorage.computeMioHash(data);
            assertEquals("", result);
        }

    }

    @Test
    @Tag("convertByteToHex-EP")
    public  void testConvertEmptyByteToHex(){
        byte[] input = "".getBytes(StandardCharsets.UTF_8);
        String hash = mioStorage.convertByteToHex( input);
        String expected = byteToHex(input);
        assertTrue(expected.equals(hash));

    }
    @Test
    @Tag("convertByteToHex-EP")
    public  void testConvertNonEmptyByteToHex(){
        byte[] input = "sdfsdf".getBytes(StandardCharsets.UTF_8);
        String hash = mioStorage.convertByteToHex( input);
        String expected = byteToHex(input);
        assertTrue(expected.equals(hash));

    }
    // -------------------------ScanForNewMioFiles--------------------------------
    @Test
    @Tag(("EP"))
    public void testScanForNewMioFilesNullDataDir(){
        assertThrows(NullPointerException.class,
                () -> mioStorage.ScanForNewMioFiles(null,logger));

    }
    @Test
    public void testScanForNewMioFilesNullLogger(){
        Path fakePath = tempDir.resolve("fake");
        assertThrows(NullPointerException.class,
                () -> mioStorage.ScanForNewMioFiles(fakePath.toString(),null));

    }

    @Test
    void testScanForNewMioFilesNoMioFolder() throws Exception {
        //dataDir exists, no mio/ sub‑folder
        Path dataDir = Files.createDirectories(tempDir.resolve("data"));
        assertThrows(NullPointerException.class,
                () -> mioStorage.ScanForNewMioFiles(dataDir.toString(), logger));
    }
    @Test
    //exists, mio/ exists but empty
    void testScanForNewMioFilesNoFileInMioFolder() throws Exception {
        //dataDir exists, has mio/ sub‑folder, but no file inside
        Path mioDir = Files.createDirectories(tempDir.resolve("data/mio"));
        mioStorage.ScanForNewMioFiles(tempDir.resolve("data").toString(), logger);
        verifyNoInteractions(logger);
    }
    @Test
    void testScanForNewMioFilesNoFileButFolder() throws Exception {
        Path dataDir = Files.createDirectories(tempDir.resolve("data"));
        Path mioDir  = Files.createDirectories(dataDir.resolve("mio"));
        Path subDir    = mioDir.resolve("subDir");

        mioStorage.ScanForNewMioFiles(dataDir.toString(), logger);
        verifyNoInteractions(logger);
        verify(logger, never()).log(eq(Level.INFO), startsWith("Game;"));
        verify(logger, never()).log(eq(Level.INFO), startsWith("Manga;"));
        verify(logger, never()).log(eq(Level.INFO), startsWith("Record;"));
    }

    @Test
    void testGameFile_logsInfoAndSetters() throws Exception {
        Path dataDir = Files.createDirectories(tempDir.resolve("data"));
        Files.createDirectories(dataDir.resolve("mio"));
        File f = dataDir.resolve("mio/game.bin").toFile();
        Files.write(f.toPath(), new byte[MioUtils.Types.GAME]); // exact length

        try (MockedStatic<FileByteOperations> fo = mockStatic(FileByteOperations.class);
             MockedStatic<MioStorage>       ms = mockStatic(MioStorage.class);
             MockedStatic<MioUtils>         mu = mockStatic(MioUtils.class);
             MockedStatic<DriverManager>    dm = mockStatic(DriverManager.class);
             MockedConstruction<Connection> connCons = mockConstruction(Connection.class,
                     (conn, ctx) -> when(conn.prepareStatement(any())).thenReturn(mock(PreparedStatement.class)));
             MockedConstruction<PreparedStatement> prepCons = mockConstruction(PreparedStatement.class)
        ) {
            byte[] gameBytes = new byte[MioUtils.Types.GAME];
            fo.when(() -> FileByteOperations.read(f.getAbsolutePath()))
                    .thenReturn(gameBytes);
            ms.when(() -> MioStorage.computeMioHash(gameBytes)).thenReturn("HASH123");
            mu.when(() -> MioUtils.computeMioID(any())).thenReturn("ID123");
            mu.when(() -> MioUtils.mapColorByte(anyByte())).thenReturn("#FFF");
            mu.when(() -> MioUtils.getBase64GamePreview(gameBytes)).thenReturn("PREVIEW");

            Connection mockConn = connCons.constructed().get(0);
            PreparedStatement mockPrep = prepCons.constructed().get(0);
            dm.when(() -> DriverManager.getConnection(anyString()))
                    .thenReturn(mockConn);

            MioStorage.ScanForNewMioFiles(dataDir.toString(), logger);

            // inside GAME branch → setters 9–15 must be called
            verify(mockPrep).setString(eq(9), anyString());
            verify(mockPrep).setString(eq(10), anyString());
            verify(mockPrep).setInt   (eq(11), anyInt());
            verify(mockPrep).setString(eq(12), anyString());
            verify(mockPrep).setString(eq(13), anyString());
            verify(mockPrep).setBoolean(eq(14), anyBoolean());
            verify(mockPrep).setString(eq(15), anyString());

            // INFO log
            verify(logger).log(eq(java.util.logging.Level.INFO),
                    contains("Game;HASH123;ID123"));
        }
    }


//
//    @Test
//    void testScanForNewMioFilesOneGameFile() throws Exception {
//        // MOCK Testing : blackbox if I could nt really act on the real db?
//        Path dataDir = Files.createDirectories(tempDir.resolve("data"));
//        Path mioDir  = Files.createDirectories(dataDir.resolve("mio"));
//        Path file    = mioDir.resolve("file.mio");
//        Files.write(file, "file".getBytes(StandardCharsets.UTF_8));
//
//        byte[] gameBytes = new byte[MioUtils.Types.GAME];//metadata
//
//        PreparedStatement preparedStatement = mock(PreparedStatement.class);
//        Connection connection = mock(Connection.class);
//        when(connection.prepareStatement(any())).thenReturn(preparedStatement);
//
//        try (MockedStatic<DriverManager> driverManagerMocked = mockStatic(DriverManager.class);
//             MockedStatic<FileByteOperations> fileByteOperationsMocked = mockStatic(FileByteOperations.class);
//             MockedStatic<MioStorage> mioStorageMocked = mockStatic(MioStorage.class);
//             MockedStatic<MioUtils>   mioUtilsMocked = mockStatic(MioUtils.class)) {
//
//            driverManagerMocked.when(() -> DriverManager.getConnection(anyString())).thenReturn(connection);
//            fileByteOperationsMocked.when(() -> FileByteOperations.read(anyString())).thenReturn(gameBytes);
//
//            // metadata, hash, id. creatorId, cartridgeId,insertQuery
//            mioStorageMocked.when(() -> MioStorage.computeMioHash(gameBytes)).thenReturn("HASH");
//            mioUtilsMocked.when(() -> MioUtils.computeMioID(any())).thenReturn("ID");
//
//            mioUtilsMocked.when(() -> MioUtils.mapColorByte(anyByte())).thenReturn("#FFF");
//            mioUtilsMocked.when(() -> MioUtils.getBase64GamePreview(gameBytes)).thenReturn("PREVIEW");
//
//            Logger logger = mock(Logger.class);
//
//            // call the function
//            MioStorage.ScanForNewMioFiles(dataDir.toString(), logger);
//
//            fileByteOperationsMocked.verify(() -> FileByteOperations.read(file.toString()));
//            mioStorageMocked.verify(() -> MioStorage.computeMioHash(gameBytes));
////            mioUtilsMocked.verify(() -> MioUtils.computeMioID(gameBytes));
//
//            verify(preparedStatement).setString(9,  "#FFF");          // cart color
//            verify(preparedStatement).setString(10, "#FFF");          // logo color
//            verify(preparedStatement).setInt   (11, 0);               // game logo (stubbed default)
//            verify(preparedStatement).setString(12, "PREVIEW");
//            verify(preparedStatement).setString(13, "");     // creatorId (Metadata stub defaults)
//            verify(preparedStatement).setBoolean(14, false);
//            verify(preparedStatement).setString(15, "");// cartridgeId
//            verify(preparedStatement).executeUpdate();
//
//        }
//    }

    @Test
    public void testScanForNewMioFilesMultipleFiles() throws Exception {
        // MOCK Testing : blackbox if I could nt really act on the real db?
        File mioGamesDir = new File(tempDir.toFile(), "mio/games");
        Files.createDirectories(mioGamesDir.toPath());

        File unCompressedMio = new File(mioGamesDir, "game.mio"); // orig
        try (FileWriter writer = new FileWriter(unCompressedMio)) {
            writer.write("unCompressed content");
        }
        assertTrue(Files.exists(tempDir.resolve("mio/games/game.mio")));
        File[] gameFiles = new File(mioGamesDir.getAbsolutePath()).listFiles(); // LENGTH 1

        byte[] gameBytes = new byte[MioUtils.Types.GAME];//metadata

        System.out.println(gameFiles.length);

        Connection mockConn        = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        System.out.println(gameFiles.length);

        when(mockConn.prepareStatement(anyString()))
                .thenReturn(mockStmt);
        System.out.println(gameFiles.length);

        try (var fo = mockStatic(FileByteOperations.class);
             var ms = mockStatic(MioStorage.class);
             var mu = mockStatic(MioUtils.class);
             var dm = mockStatic(DriverManager.class);
        ) {
            fo.when(() -> FileByteOperations.read(anyString()))
                    .thenReturn(gameBytes);
            System.out.println(gameFiles.length);
            ms.when(() -> MioStorage.computeMioHash(gameBytes))
                    .thenReturn("HASH123");
            System.out.println(gameFiles.length);
            mu.when(() -> MioUtils.computeMioID(any()))
                    .thenReturn("ID123");
            System.out.println(gameFiles.length);
            mu.when(() -> MioUtils.mapColorByte(anyByte()))
                    .thenReturn("#ABC");
            mu.when(() -> MioUtils.getBase64GamePreview(gameBytes))
                    .thenReturn("PREVIEW");

            // 4) THIS is the important bit: pass in the root dataDir, not the subfolder
            MioStorage.ScanForNewMioFiles(dataDir.toString(), logger);
            System.out.println("3");

            // 5) verify it ran the GAME branch once (sets + logs)
            verify(logger).log(eq(Level.INFO),
                    contains("Game;HASH123;ID123"));
            // and that prepareStatement was called once
            verify(mockConn, times(1)).prepareStatement(any());
        }

//        MioStorage.ScanForNewMioFiles(mioGamesDir.toString(), logger);

    }
//    @Test
//    public void testNullMetadataThrowsException() throws  Exception {
//        Method method = MioStorage.class.getDeclaredMethod(
//                "parseMioBase", Metadata.class, String.class, String.class, String.class, Connection.class, int.class);
//        method.setAccessible(true);
//
//        assertThrows(NullPointerException.class, () -> {
//            method.invoke(null, null, "hash", "id", "creatorID", connectionMocked, MioUtils.Types.GAME);
//        });
//    }

//    @Test
//    public void testParseMioBaseWithGameType() throws Exception { // Correct method signature
//        System.out.println("3");
////         Get reference to the private static method
//        Method method = MioStorage.class.getDeclaredMethod(
//                "parseMioBase", Metadata.class, String.class, String.class, String.class, Connection.class, int.class);
//        method.setAccessible(true);  // allow access
//
//        when(connectionMocked.prepareStatement(anyString())).thenReturn(preparedStatementMocked);
//        when(mockMeta.getName()).thenReturn("MetaName");
//        when(mockMeta.getCreator()).thenReturn("MetaCreator");
//        when(mockMeta.getBrand()).thenReturn("MetaBrand");
//        when(mockMeta.getDescription()).thenReturn("MetaDescription");
//        when(mockMeta.getTimestamp()).thenReturn(100);
//
//        // Invoke with mock data
//        Object result = method.invoke(null, mockMeta, "hash", "id", "creatorID", connectionMocked, MioUtils.Types.GAME);
//
//        assertNotNull(result);
//        verify(connectionMocked).prepareStatement(startsWith("INSERT INTO Games")); // query
//        verify(preparedStatementMocked).setString(1, "hash");
//        verify(preparedStatementMocked).setString(2, "id");
//        verify(preparedStatementMocked).setString(3, "MetaName");
//        verify(preparedStatementMocked).setString(4, "MetaNamez");
//        verify(preparedStatementMocked).setString(5, "MetaCreator");
//        verify(preparedStatementMocked).setString(6, "MetaBrand");
//        verify(preparedStatementMocked).setString(7, "MetaDescription");
//        verify(preparedStatementMocked).setInt(eq(8), 100);
//
//    }
    // test mega
    // test record


    // -------------------ConsumeMio-----------------------

//    @Test
//    public  void testConsumeMioFileNull() throws Exception {
//        // call function
//        Method method = MioStorage.class.getDeclaredMethod("consumeMio", File.class, String.class, int.class);
//        method.setAccessible(true);
//        String hash = "fakehash123";
//        boolean result = (boolean) method.invoke(null, null, hash, MioUtils.Types.GAME);
//
//        // temp/data/mio/game/
//        Path moveToFolder = dataDir.resolve("mio").resolve("game");
//        Path moveToPath = moveToFolder.resolve(hash+".mio.zip");
//
//        assertTrue(result);
//        assertFalse(Files.exists(file)); // original should be deleted
//        assertTrue(Files.exists(moveToPath));   // zip should exist
//
//    }

//    @Test
//    public  void testConsumeMioGameType() throws Exception {
//        // temp/data/mio/file.mio
//        Path dataDir = Files.createDirectories(tempDir.resolve("data"));
//        Path mioDir  = Files.createDirectories(dataDir.resolve("mio"));
//        Path file    = mioDir.resolve("file.mio");
//        Files.write(file, "file".getBytes(StandardCharsets.UTF_8));
//        assertTrue(Files.exists(file), "The .mio file should exist before running");
//
//        String hash = "fakehash123";
//
//        // temp/data/mio/game/
//        Path moveToFolder = dataDir.resolve("mio").resolve("game");
//        assertFalse(Files.exists(moveToFolder)); // not create game folder (before)
//        Path moveToPath = moveToFolder.resolve(hash+".mio.zip");
//
//
//        // call function
//        Method method = MioStorage.class.getDeclaredMethod("consumeMio", File.class, String.class, int.class);
//        method.setAccessible(true);
//        boolean result = (boolean) method.invoke(null, file, hash, MioUtils.Types.GAME);
//
//
//        assertTrue(result);
//        assertFalse(Files.exists(file)); // original should be deleted
//        assertTrue(Files.exists(moveToPath));   // zip should exist
//
//    }
//
//    @Test
//    public  void testConsumeMioMANGAType() throws Exception {
//        // temp/data/mio/file.mio
//        Path dataDir = Files.createDirectories(tempDir.resolve("data"));
//        Path mioDir  = Files.createDirectories(dataDir.resolve("mio"));
//        Path file    = mioDir.resolve("file.mio");
//        Files.write(file, "file".getBytes(StandardCharsets.UTF_8));
//        assertTrue(Files.exists(file), "The .mio file should exist before running");
//
//        String hash = "fakehash123";
//
//        // call function
//        Method method = MioStorage.class.getDeclaredMethod("consumeMio", File.class, String.class, int.class);
//        method.setAccessible(true);
//        boolean result = (boolean) method.invoke(null, file, hash, MioUtils.Types.MANGA);
//
//        // temp/data/mio/manga/
//        Path mangaDir = Files.createDirectories(mioDir.resolve("manga"));// make dir exist
//        Path moveToPath = mangaDir.resolve(hash+".mio.zip");
//        assertTrue(Files.exists(mangaDir)); // skip mkdir
//
//        assertTrue(result);
//        assertFalse(Files.exists(file)); // original should be deleted
//        assertTrue(Files.exists(moveToPath));   // zip should exist
//
//    }
//
//    @Test
//    public  void testConsumeMioRECORDType() throws Exception {
//        // temp/data/mio/file.mio
//        Path dataDir = Files.createDirectories(tempDir.resolve("data"));
//        Path mioDir  = Files.createDirectories(dataDir.resolve("mio"));
//        Path file    = mioDir.resolve("file.mio");
//        Files.write(file, "file".getBytes(StandardCharsets.UTF_8));
//        assertTrue(Files.exists(file), "The .mio file should exist before running");
//
//        String hash = "fakehash123";
//
//        // call function
//        Method method = MioStorage.class.getDeclaredMethod("consumeMio", File.class, String.class, int.class);
//        method.setAccessible(true);
//        boolean result = (boolean) method.invoke(null, file, hash, MioUtils.Types.RECORD);
//
//        // temp/data/mio/record/
//        Path moveToPath = dataDir.resolve("mio").resolve("record").resolve(hash+".mio.zip");
//
//        assertTrue(result);
//        assertFalse(Files.exists(file)); // original should be deleted
//        assertTrue(Files.exists(moveToPath));   // zip should exist
//
//    }
//
//    @Test
//    public  void testConsumeMioReturnFalse() throws Exception {
//        // temp/data/mio/file.mio
//        Path dataDir = Files.createDirectories(tempDir.resolve("data"));
//        Path mioDir  = Files.createDirectories(dataDir.resolve("mio"));
//        Path file    = mioDir.resolve("file.mio");
//        Files.write(file, "file".getBytes(StandardCharsets.UTF_8));
//        assertTrue(Files.exists(file), "The .mio file should exist before running");
//
//        String hash = "fakehash123";
//
//        // temp/data/mio/game/
//        Path gameDir = Files.createDirectories(mioDir.resolve("game"));// make dir exist
//        Path moveToPath = Files.createDirectories(gameDir.resolve(hash+".mio.zip"));
//        assertTrue(Files.exists(gameDir)); // skip mkdir
//
//
//        // call function
//        Method method = MioStorage.class.getDeclaredMethod("consumeMio", File.class, String.class, int.class);
//        method.setAccessible(true);
//        boolean result = (boolean) method.invoke(null, file, hash, MioUtils.Types.GAME);
//
//        assertFalse(result);
//    }
    // -------------------UpdataMetadata-----------------------
    @Test
    void testUpdateMetadataConnectionNull() throws Exception {
        assertThrows(NullPointerException.class,
                () -> mioStorage.UpdateMetadata(connectionMocked, null, logger, null));
    }
    @Test
    void testUpdateMetadataProcessesOneFile() throws Exception {
        // prepare
        // Setup directory structure: tempDir/mio/games/mio.games
        File mioGamesDir = new File(tempDir.toFile(), "mio/games");
        assertTrue(mioGamesDir.mkdirs());

        // tempDir/mio/games/game.mio  1 file
        File unCompressedMio = new File(mioGamesDir, "game.mio"); // orig
        try (FileWriter writer = new FileWriter(unCompressedMio)) {
            writer.write("unCompressed content");
        }
        assertTrue(Files.exists(tempDir.resolve("mio/games/game.mio")));

        File compressedMio = new File(String.valueOf(tempDir.resolve("mio/games/game.miozip"))); // dest
//        File compressedMio = new File(mioGamesDir, "game.miozip");
//        boolean created = compressedMio.createNewFile(); // create the file (empty zip file)
        MioCompress.compressMio(unCompressedMio, compressedMio,"compressGame");
        assertTrue(compressedMio.exists()); // true

        File[] gameFiles = new File(mioGamesDir.getAbsolutePath()).listFiles(); // LENGTH 1
        assertTrue(gameFiles.length==2);

        mockStatic(MioCompress.class);
        when(MioCompress.uncompressMio(compressedMio)).thenReturn(unCompressedMio);

        // Stub file reader
        mockStatic(FileByteOperations.class);
        byte[] dummyData = new byte[MioUtils.Types.GAME];
        when(FileByteOperations.read(unCompressedMio.getAbsolutePath())).thenReturn(dummyData);

        // Stub metadata behavior
        Metadata mockMeta = mock(Metadata.class);
        when(mockMeta.getName()).thenReturn("Name");
        when(mockMeta.getDescription()).thenReturn("Description");
        when(mockMeta.getBrand()).thenReturn("Brand");
        when(mockMeta.getCreator()).thenReturn("Creator");
        when(mockMeta.getCreatorId()).thenReturn("CreatorId");
        when(mockMeta.getCartridgeId()).thenReturn("CartridgeId");

        // Metadata constructor should return our mock (needs wrapping, or inject with test seam)
        mockConstruction(Metadata.class, (mock, context) -> {
            when(mock.getName()).thenReturn("Name");
            when(mock.getCreatorId()).thenReturn("CreatorId");
        });

        //
        when(preparedStatementMocked.toString()).thenReturn("UPDATE ...");

        // execute
        MioStorage.UpdateMetadata(connectionMocked, tempDir.toString(), logger, "games");

        // Verify update executed
        verify(preparedStatementMocked).executeUpdate();

        // Verify logging
        verify(logger, atLeastOnce()).log(eq(Level.INFO), contains("Looping Files"));

        // Check uncompressed file was deleted
        assertFalse(unCompressedMio.exists());
    }

    @Test
    void testUpdateMetadataSkipsDirectory() throws Exception {
        //  tempDir/mio/games/
//        Path mioGames   = Files.createDirectories(tempDir.resolve("mio/games"));
//
//        // tempDir/mio/games/folder
//        File dummyDir = tempDir.resolve("dummyFolder").toFile();
//        assertTrue(dummyDir.isDirectory());
//
//        File[] gameFiles = new File(mioGames.toString()).listFiles(); // LENGTH 1
//        System.out.println(gameFiles[0].isDirectory());

        // mio/game
        Path dir     = Paths.get(tempDir.toString(), "mio");
        Files.createDirectories(dir);

        Path file    = dir.resolve("game.miozip");
        Files.write(file, "dummy".getBytes());

        File[] gameFiles = new File(dataDir + "/mio/"+"game").listFiles();
        for (File f : gameFiles) {
            System.out.println(f.getName() + " -> " + f.isDirectory());  // false
            // !f.isDirectory() 为 true，进入处理逻辑
        }

        MioStorage.UpdateMetadata(connectionMocked, dir.toString(), logger, "games");
    }


    @Test
    void testUpdateMetadataProcessesFiles() throws Exception {
        // public static void UpdateMetadata(Connection connection, String dataDir, Logger logger, String subdirectory) throws SQLException {
        // Setup directory structure: tempDir/mio/games/
        File mioGamesDir = new File(tempDir.toFile(), "mio/games");
        assertTrue(mioGamesDir.mkdirs());


        // tempDir/mio/games/file.miozip   1 file
        File compressedMio = new File(mioGamesDir, "file.miozip");
        try (FileWriter writer = new FileWriter(compressedMio)) {
            writer.write("compressed content");
        }
        assertTrue(Files.exists(tempDir.resolve("mio/games/file.miozip")));

        File[] gameFiles = new File(mioGamesDir.getAbsolutePath()).listFiles(); // LENGTH 1
        assertFalse(gameFiles[0].isDirectory());

        // tempDir/temp.mio
        File fakeUncompressed = new File(tempDir.toFile(), "temp.mio");
        try (FileWriter fw = new FileWriter(fakeUncompressed)) {
            fw.write("dummy bytes");
        }

        // ---- MOCK STATIC-LIKE CLASSES ----
        // Stub compress/uncompress
        mockStatic(MioCompress.class);
        when(MioCompress.uncompressMio(compressedMio)).thenReturn(fakeUncompressed);

        // Stub file reader
        mockStatic(FileByteOperations.class);
        byte[] dummyData = new byte[100];
        when(FileByteOperations.read(fakeUncompressed.getAbsolutePath())).thenReturn(dummyData);

        // Stub metadata behavior
        Metadata mockMeta = mock(Metadata.class);
        when(mockMeta.getName()).thenReturn("Name");
        when(mockMeta.getDescription()).thenReturn("Description");
        when(mockMeta.getBrand()).thenReturn("Brand");
        when(mockMeta.getCreator()).thenReturn("Creator");
        when(mockMeta.getCreatorId()).thenReturn("CID123");
        when(mockMeta.getCartridgeId()).thenReturn("Cart456");

        // Metadata constructor should return our mock (needs wrapping, or inject with test seam)
        mockConstruction(Metadata.class, (mock, context) -> {
            when(mock.getName()).thenReturn("Name");
            when(mock.getCreatorId()).thenReturn("CID123");
        });

        // ---- MOCK DB ----
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.toString()).thenReturn("UPDATE ...");


        // Method under test
        MioStorage.UpdateMetadata(conn, tempDir.toString(), logger, "games");

        // Verify update executed
        verify(stmt).executeUpdate();

        // Verify logging
        verify(logger, atLeastOnce()).log(eq(Level.INFO), contains("Looping Files"));

        // Check uncompressed file was deleted
        assertFalse(fakeUncompressed.exists());
    }





}
