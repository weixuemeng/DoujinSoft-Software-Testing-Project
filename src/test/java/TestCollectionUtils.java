package test.java;

import com.difegue.doujinsoft.templates.Collection;
import com.difegue.doujinsoft.utils.CollectionUtils;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TestCollectionUtils {
    @TempDir
    Path tempDir;
    Path tmp = null;
    Collection validCollection ;
    @BeforeEach
    public void setUp() throws  IOException{
        tmp = Files.createTempFile("tmp", ".json"); // create valid path
        validCollection = new Collection(); // create valid collection
        validCollection.id = "1234";
        validCollection.collection_type = "manga";
        validCollection.collection_name = "Party";
        validCollection.mios = new String[] { "123452", "452452" };
    }

    @AfterEach
    public void clear() throws  IOException{
        if(tmp!= null){
            Files.deleteIfExists(tmp);
        }
    }
    @Test
    @Tag("GetCollectionFromFile-blackbox")
    public void testGetCollectionFromFilePathNull(){
        //If the file does not exist, FileReader immediately throws a FileNotFoundException, which the method declares.
        assertThrows(NullPointerException.class,
                () -> CollectionUtils.GetCollectionFromFile(null));
    }

    @Test
    @Tag("GetCollectionFromFile-blackbox")
    public void testGetCollectionFromFilePathNotExist(){
        String fakePath = "D:/documents/mygame.json";
        assertThrows(FileNotFoundException.class, () ->
                CollectionUtils.GetCollectionFromFile(fakePath));
    }

    @Test
    @Tag("GetCollectionFromFile-blackbox")
    public void testGetCollectionFromFilePathValid() throws IOException {
        //        System.out.println("Temporary file path: " + tmp.toAbsolutePath());

        CollectionUtils collectionUtils = new CollectionUtils();
        collectionUtils.SaveCollectionToFile(validCollection, tmp.toString());
        Collection loadCollection = collectionUtils.GetCollectionFromFile(tmp.toString());

        assertEquals(validCollection.id, loadCollection.id);
        assertEquals(validCollection.collection_type, loadCollection.collection_type);
        assertEquals(validCollection.collection_name, loadCollection.collection_name);
        assertArrayEquals(validCollection.mios, loadCollection.mios);

    }
    /*
     (Collection c, String path):
     c: null not null (2)
     path: null, valid but not exist , valid and exist
     combinatorial: 6

     */

    @Test
    public void testSaveCollectionToFileCollectionNullPathNull(){
        //If the file does not exist, FileReader immediately throws a FileNotFoundException, which the method declares.
        assertThrows(NullPointerException.class,
                () -> CollectionUtils.SaveCollectionToFile(null, null));
    }
    @Test
    public void testSaveCollectionToFileCollectionNullPathinvalid(){
        //If the file does not exist, FileReader immediately throws a FileNotFoundException, which the method declares.
        assertThrows(NullPointerException.class,
                () -> CollectionUtils.SaveCollectionToFile(null, "fake.json"));
    }
    @Test
    public void testSaveCollectionToFileCollectionNullPathValid(){
        //If the file does not exist, FileReader immediately throws a FileNotFoundException, which the method declares.
        assertThrows(NullPointerException.class,
                () -> CollectionUtils.SaveCollectionToFile(null, tmp.toAbsolutePath().toString()));
    }

    @Test
    public void testSaveCollectionToFileCollectionValidPathNull(){
        //If the file does not exist, FileReader immediately throws a FileNotFoundException, which the method declares.
        assertThrows(NullPointerException.class,
                () -> CollectionUtils.SaveCollectionToFile(validCollection, null));
    }
    @Test
    public void testSaveCollectionToFileCollectionValidPathInvalid(){
        //If the file does not exist, FileReader immediately throws a FileNotFoundException, which the method declares.
        assertThrows(IOException.class,
                () -> CollectionUtils.SaveCollectionToFile(validCollection, "fake.json"));
    }
    @Test
    public void testSaveCollectionToFileBothValid() throws  IOException{
        //If the file does not exist, FileReader immediately throws a FileNotFoundException, which the method declares.
        // private static final FileSystem fs = DefaultFileSystem.getFileSystem();
        // path name: Creates a file output stream to write to the file with the specified name.
        CollectionUtils collectionUtils = new CollectionUtils();
        collectionUtils.SaveCollectionToFile(validCollection, tmp.toAbsolutePath().toString());

        String fileContent = Files.readString( tmp);
        Collection deserialized = new Gson().fromJson(fileContent, Collection.class); // file(file content) -> collection

        assertEquals(validCollection.id, deserialized.id);
        assertEquals(validCollection.collection_type, deserialized.collection_type);
        assertEquals(validCollection.collection_name, deserialized.collection_name);
        assertArrayEquals(validCollection.mios, deserialized.mios);

    }






}
