package test.java;

import com.difegue.doujinsoft.templates.BaseMio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.xperia64.diyedit.metadata.Metadata;
import com.difegue.doujinsoft.utils.MioUtils;
import org.mockito.Mockito;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TestBaseMio {
    private byte[] createFileOfLength(int len) {
        byte[] file = new byte[len];
        return file;
    }
    ResultSet resultSet;
    @BeforeEach
    public void setUp() throws SQLException{
        resultSet = Mockito.mock(ResultSet.class); // mock the result set
        when(resultSet.getInt("timeStamp")).thenReturn(1200);
        when(resultSet.getString("hash")).thenReturn("emptyTitleHash");
        when(resultSet.getString("brand")).thenReturn("brandA");
        when(resultSet.getString("creator")).thenReturn("John");
        when(resultSet.getString("creatorID")).thenReturn("creatorIDA");
        when(resultSet.getString("cartridgeID")).thenReturn("cartridgeIDA");
        when(resultSet.getString("color")).thenReturn("blue");
        when(resultSet.getInt("logo")).thenReturn(9);

    }

    @Test
    @Tag("EP_NULL")
    public void testContructorMetadataNull(){
        assertThrows(NullPointerException.class, () -> {
            new BaseMio((Metadata) null);
        });
    }

    @Test
    @Tag("EP_NON_NULL")
    @Tag("Branch_TYPE_GAME")
    public void testConstructorMetadataGAME(){
        byte[] file = createFileOfLength(65536);
        Metadata metadata = new Metadata(file);
        metadata.setName("GAME header");
        metadata.setBrand("GAME brand");
        metadata.setCreater("John");
        metadata.setTimestamp(20);
        String description = "This is a description for metadata type GAME";
        metadata.setDescription(description);
        String expectedMioID = "G-"+MioUtils.computeMioID(metadata);
        BaseMio baseMio = new BaseMio(metadata);
        assertEquals(expectedMioID, baseMio.mioID);
        assertEquals(description.substring(0, 18), baseMio.mioDesc1);
        assertEquals(description.substring(18), baseMio.mioDesc2);
    }

    @Test
    @Tag("Branch_TYPE_MANGA")
    public void testConstructorMetadataMANGA(){
        byte[] file = createFileOfLength(14336);
        Metadata metadata = new Metadata(file);
        metadata.setName("MANGA header");
        metadata.setBrand("MANGA brand");
        metadata.setCreater("Jack");
        metadata.setTimestamp(30);
        String description = "This is a description for metadata type MANGA";
        metadata.setDescription(description);
        String expectedMioID = "M-"+MioUtils.computeMioID(metadata);
        BaseMio baseMio = new BaseMio(metadata);
        assertEquals(expectedMioID, baseMio.mioID);
        assertEquals(description.substring(0, 18), baseMio.mioDesc1);
        assertEquals(description.substring(18), baseMio.mioDesc2);
    }

    @Test
    @Tag("Branch_TYPE_RECORD")
    @Tag("Branch_TYPE_Description_more_than_19")
    public void testConstructorMetadataRECORD(){
        byte[] file = createFileOfLength(8192);
        Metadata metadata = new Metadata(file);
        metadata.setName("RECORD header");
        metadata.setBrand("RECORD brand");
        metadata.setCreater("Mary");
        metadata.setTimestamp(120);
        String description = "This is a description for metadata type RECORD";
        metadata.setDescription(description);
        String expectedMioID = "R-"+MioUtils.computeMioID(metadata);
        BaseMio baseMio = new BaseMio(metadata);
        assertEquals(expectedMioID, baseMio.mioID);
        assertEquals(description.substring(0, 18), baseMio.mioDesc1);
        assertEquals(description.substring(18), baseMio.mioDesc2);
    }

    @Test
    @Tag("Branch_TYPE_Description_less_than_19")
    public void testConstructorMetadataDescriptionLess19(){
        byte[] file = createFileOfLength(8192);
        Metadata metadata = new Metadata(file);
        metadata.setName("RECORD another header ");
        metadata.setBrand("RECORD another brand");
        metadata.setCreater("Bob");
        metadata.setTimestamp(40);
        String description = "Description <19";
        metadata.setDescription(description);
        String expectedMioID = "R-"+MioUtils.computeMioID(metadata);
        BaseMio baseMio = new BaseMio(metadata);
        assertEquals(expectedMioID, baseMio.mioID);
        assertEquals(description.substring(0, 18), baseMio.mioDesc1);
        assertEquals(null, baseMio.mioDesc2);
    }

    @Test
    void testConstructorWithNullResultSet() {
        assertThrows(IllegalArgumentException.class, () -> {
            new BaseMio((ResultSet) null);
        });
    }

    @Test
    @Tag("mock-testing")
    public void testResultSetDescriptionLonger() throws SQLException{
        when(resultSet.getString("colorLogo")).thenReturn("grey darken-4");
        when(resultSet.getString("description")).thenReturn("This is the description for mock testing 1");
        when(resultSet.getString("name")).thenReturn("   "); // title
        when(resultSet.getString("id")).thenReturn("emptyTitleId"); // mioID

        // crease baseMio
        BaseMio baseMio  = new BaseMio(resultSet);

        // color -> grey
        assertEquals( "grey", baseMio.colorLogo);
        // description > 18
        assertEquals(resultSet.getString("description").substring(0,18) ,baseMio.mioDesc1);
        assertEquals(resultSet.getString("description").substring(18) ,baseMio.mioDesc2);
        assertEquals("No Title", baseMio.name);
        assertEquals(null, baseMio.specialBrand);

        // general field
        assertEquals("emptyTitleId", baseMio.mioID);
        assertEquals("emptyTitleHash", baseMio.hash);
        assertEquals("brandA", baseMio.brand);
        assertEquals("John", baseMio.creator);
        assertEquals("creatorIDA", baseMio.creatorId);
        assertEquals("cartridgeIDA", baseMio.cartridgeId);
        assertEquals("blue", baseMio.colorCart);
        assertEquals(9, baseMio.logo);
    }

    @Test
    @Tag("mock-testing")
    public void testResultSetDescriptionShorter() throws SQLException{
        when(resultSet.getString("colorLogo")).thenReturn("brownLogo");
        when(resultSet.getString("description")).thenReturn("Comic");
        when(resultSet.getString("name")).thenReturn("basename");
        when(resultSet.getString("id")).thenReturn("ComicId"); // mioID


        // crease baseMio
        BaseMio baseMio  = new BaseMio(resultSet);

        // color -> same
        assertEquals( "brownLogo", baseMio.colorLogo);
        // description > 18
        assertEquals(resultSet.getString("description") ,baseMio.mioDesc1);
        assertEquals(null ,baseMio.mioDesc2);
        assertEquals("basename", baseMio.name);
        assertEquals(null, baseMio.specialBrand);
    }

    @Test
    @Tag("mock-testing")
    public void testResultSetDescriptionEmpty() throws SQLException{
        when(resultSet.getString("colorLogo")).thenReturn("brownLogo");
        when(resultSet.getString("description")).thenReturn("    ");
        when(resultSet.getString("name")).thenReturn("basename");
        when(resultSet.getString("id")).thenReturn(" Id");

        // crease baseMio
        BaseMio baseMio  = new BaseMio(resultSet);

        // color -> same
        assertEquals( "brownLogo", baseMio.colorLogo);
        // description > 18
        assertEquals("No Description." ,baseMio.mioDesc1);
        assertEquals(null ,baseMio.mioDesc2);
        assertEquals("basename", baseMio.name);
        assertEquals(null, baseMio.specialBrand);
    }

    @PrameterizedTest
    @ValueSource(ids = {"themId","wario","nintendo"})
    public void testResultSetSpecialBrand(String id) throws SQLException{
        when(resultSet.getString("colorLogo")).thenReturn("brownLogo");
        when(resultSet.getString("description")).thenReturn("   ");
        when(resultSet.getString("name")).thenReturn("basename");
        when(resultSet.getString("id")).thenReturn(id);

        // crease baseMio
        BaseMio baseMio  = new BaseMio(resultSet);

        // color -> same
        assertEquals( "brownLogo", baseMio.colorLogo);
        // description > 18
        assertEquals("No Description." ,baseMio.mioDesc1);
        assertEquals(null ,baseMio.mioDesc2);
        assertEquals("basename", baseMio.name);
        assertEquals("theme", baseMio.specialBrand); // special brand
    }

}
