package com.difegue.doujinsoft.templates;

import com.difegue.doujinsoft.utils.MioUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestCollection {
    Collection collection;
    @BeforeEach
    void setUp() {
        collection = new Collection();
    }

    @AfterEach
    void tearDown() {
        collection = null;
    }

    // test GetMioSQL
    @Test
    public void testGetMioSQLWithEmptyMio() {
        collection.mios = new String[]{};
        Assertions.assertEquals("()", collection.getMioSQL());
    }

    @Test
    public void testGetMioSQLWithNullMio() {
        collection.mios = null;
        Assertions.assertEquals("()", collection.getMioSQL());
    }

    @Test
    public void testGetMioSQLWithOneMio() {
        collection.mios = new String[]{"1c44f131f5e616cd19c826ceeb450f8e"};
        Assertions.assertEquals("(\"1c44f131f5e616cd19c826ceeb450f8e\")", collection.getMioSQL());
    }

    @Test
    public void testGetMioSQLWithMoreThanOneMio() {
        collection.mios = new String[]{"1c44f131f5e616cd19c826ceeb450f8e", "4777afb2f1760d7c9cb6be735be98c57"};
        Assertions.assertEquals("(\"1c44f131f5e616cd19c826ceeb450f8e\",\"4777afb2f1760d7c9cb6be735be98c57\")", collection.getMioSQL());
    }

    // test getType
    @Test
    public void testGetTypeWithGameType() {
        collection.collection_type = "game";
        Assertions.assertEquals(MioUtils.Types.GAME, collection.getType());
    }

    @Test
    public void testGetTypeWithMangaType() {
        collection.collection_type = "manga";
        Assertions.assertEquals(MioUtils.Types.MANGA, collection.getType());
    }

    @Test
    public void testGetTypeWithRecordType() {
        collection.collection_type = "record";
        Assertions.assertEquals(MioUtils.Types.RECORD, collection.getType());
    }

    @Test
    public void testGetTypeWithEmptyType() {
        collection.collection_type = "";
        Assertions.assertEquals(MioUtils.Types.GAME, collection.getType());
    }

    @Test
    public void testGetTypeWithOtherType() {
        collection.collection_type = "random_input";
        Assertions.assertEquals(MioUtils.Types.GAME, collection.getType());
    }

    // test addMioHash
    @Test
    public void testAddMioHashToNullMio() {
        collection.mios = null;
        collection.addMioHash("1c44f131f5e616cd19c826ceeb450f8e");
        Assertions.assertArrayEquals(new String[]{"1c44f131f5e616cd19c826ceeb450f8e"},collection.mios);
    }

    @Test
    public void testAddMioHashToEmptyMio() {
        collection.mios = new String[]{};
        collection.addMioHash("1c44f131f5e616cd19c826ceeb450f8e");
        Assertions.assertArrayEquals(new String[]{"1c44f131f5e616cd19c826ceeb450f8e"},collection.mios);
    }

    @Test
    public void testAddMioHashToNotEmptyNotDuplicatedMio() {
        collection.mios = new String[]{"1c44f131f5e616cd19c826ceeb450f8e"};
        collection.addMioHash("4777afb2f1760d7c9cb6be735be98c57");
        Assertions.assertArrayEquals(new String[]{"1c44f131f5e616cd19c826ceeb450f8e", "4777afb2f1760d7c9cb6be735be98c57"},collection.mios);
    }

    @Test
    public void testAddMioHashToNotEmptyDuplicatedMio() {
        collection.mios = new String[]{"1c44f131f5e616cd19c826ceeb450f8e"};
        collection.addMioHash("1c44f131f5e616cd19c826ceeb450f8e");
        Assertions.assertArrayEquals(new String[]{"1c44f131f5e616cd19c826ceeb450f8e"},collection.mios);
    }

    @Test
    public void testAddMioHashToEmptyHashWithNullHash() {
        collection.mios = new String[]{};
        collection.addMioHash(null);
        Assertions.assertArrayEquals(new String[]{},collection.mios);
    }

    @Test
    public void testAddMioHashToEmptyHashWithEmptyHash() {
        collection.mios = new String[]{};
        collection.addMioHash("");
        Assertions.assertArrayEquals(new String[]{},collection.mios);
    }
}
