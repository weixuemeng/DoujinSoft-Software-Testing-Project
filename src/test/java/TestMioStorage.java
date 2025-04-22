package test.java;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class TestMioStorage {
    @TempDir
    Path tempDir;
    MioStorage mioStorage;
    Logger logger;

    @BeforeEach
    public void setUp(){
        mioStorage = new MioStorage();
        logger = mock(Logger.class);
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

    @Test
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
        Path dataDir = Files.createDirectories(tempDir.resolve("data"));
        Path mioDir  = Files.createDirectories(dataDir.resolve("mio"));
        Path file    = mioDir.resolve("file.mio");
        Files.write(file, "file".getBytes(StandardCharsets.UTF_8));

        System.out.println(file.toFile().isDirectory());      // false
        System.out.println(mioDir.toFile().isDirectory());

        byte[] gameBytes = new byte[MioUtils.Types.GAME];//metadata

        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(any())).thenReturn(preparedStatement);

        try (MockedStatic<DriverManager> drv = mockStatic(DriverManager.class);
             MockedStatic<FileByteOperations> rd = mockStatic(FileByteOperations.class);
             MockedStatic<MioStorage> ms = mockStatic(MioStorage.class);
             MockedStatic<MioUtils>   mu = mockStatic(MioUtils.class)) {

            drv.when(() -> DriverManager.getConnection(anyString()))
                    .thenReturn(connection);
            rd.when(() -> FileByteOperations.read(anyString()))
                    .thenReturn(gameBytes);

            // stub the helpers used inside the branch
            ms.when(() -> MioStorage.computeMioHash(gameBytes)).thenReturn("HASH");
            mu.when(() -> MioUtils.computeMioID(any())).thenReturn("ID");
            mu.when(() -> MioUtils.mapColorByte(anyByte())).thenReturn("#FFF");
            mu.when(() -> MioUtils.getBase64GamePreview(gameBytes)).thenReturn("PREVIEW");

            try (MockedConstruction<Metadata> metaCons =
                         mockConstruction(Metadata.class,
                                 (m, ctx) -> { when(m.getCreatorId()).thenReturn("");
                                     when(m.getCartridgeId()).thenReturn(""); });
                 MockedConstruction<GameEdit> gameCons = mockConstruction(GameEdit.class,
                                 (g, ctx) -> { when(g.getCartColor()).thenReturn((byte)0);
                                     when(g.getLogoColor()).thenReturn((byte)0);
                                     when(g.getLogo()).thenReturn((byte) 0);
                                     when(g.getName()).thenReturn("Test Game"); })) {

                /* ---- 2c. stub parseMioBase to hand back our ps ---- */
//                ms.when(() -> MioStorage.parseMioBase(any(), anyString(), anyString(),
//                                        anyString(), eq(connection), anyInt()))
//                        .thenReturn(preparedStatement);

                Logger log = mock(Logger.class);

                /* ---- 3.  execute ---- */
                MioStorage.ScanForNewMioFiles(dataDir.toString(), log);

                /* ---- 4.  verify setters on *ps* -------------------- */
//                verify(preparedStatement).setString(9 , "#FFF");
                verify(preparedStatement).setString(10, "#FFF");
                verify(preparedStatement).setInt   (11, 0);
                verify(preparedStatement).setString(12, "PREVIEW");
                verify(preparedStatement).setString(13, "");
                verify(preparedStatement).setBoolean(14, false);
                verify(preparedStatement).setString(15, "");
                verify(preparedStatement).executeUpdate();
            }

        }
    }




}
