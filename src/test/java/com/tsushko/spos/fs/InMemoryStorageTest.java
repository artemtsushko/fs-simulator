package com.tsushko.spos.fs;

import org.junit.*;
import static org.junit.Assert.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;


/**
 * Created by artem on 13.05.15.
 */
public class InMemoryStorageTest {

    private InMemoryStorage testStorage;
    static final int TEST_BLOCK_SIZE = 16;

    @Before
    public void setUpExampleStorage() {
        testStorage = InMemoryStorage.getStorage(256,TEST_BLOCK_SIZE);
        byte[] block = new byte[TEST_BLOCK_SIZE];
        for (int i = 0; i < 256; ++i) {
            Arrays.fill(block, (byte) (i - 128));
            testStorage.writeBlock(block,i);
        }
    }

    @Test
    public void testReadBlock() {
        byte[] expected = new byte[TEST_BLOCK_SIZE];
        byte[] actual;
        for (int i = 0; i < 256; ++i) {
            Arrays.fill(expected, (byte) (i - 128));
            actual = testStorage.readBlock(i);
            assertArrayEquals(expected, actual);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testShouldNotReadFromNegativeBlockNumber() {
        byte[] block = testStorage.readBlock(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testShouldNotReadFromOversizeBlockNumber() {
        byte[] block = testStorage.readBlock(testStorage.getBlocksNumber() + 1);
    }


    @Test
    public void testWriteBlock() {
        byte[] expected = new byte[TEST_BLOCK_SIZE];
        Arrays.fill(expected, (byte) (125));
        byte[] actual;
        for (int i = 0; i < 256; ++i) {
            testStorage.writeBlock(expected, i);
            actual = testStorage.readBlock(i);
            assertArrayEquals(expected,actual);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShouldNotWriteTooShortBlock() {
        byte[] block = new byte[TEST_BLOCK_SIZE - 1];
        testStorage.writeBlock(block, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShouldNotWriteTooLongBlock() {
        byte[] block = new byte[TEST_BLOCK_SIZE + 1];
        testStorage.writeBlock(block, 0);
    }

    @Test(expected = NullPointerException.class)
    public void testShouldNotNotWriteNullBlock() {
        byte[] block = null;
        testStorage.writeBlock(block, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testShouldNotWriteToNegativeBlockNumber() {
        byte[] block = new byte[TEST_BLOCK_SIZE];
        testStorage.writeBlock(block, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testShouldNotWriteToOversizeBlockNumber() {
        byte[] block = new byte[TEST_BLOCK_SIZE];
        testStorage.writeBlock(block, testStorage.getBlocksNumber() + 1);
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testSaveToFile() throws IOException, ClassNotFoundException {
        File file = temporaryFolder.newFile();
        testStorage.saveToFile(file);
        Storage restoredStorage = InMemoryStorage
                .getStorageFromFile(file);
        byte[] expected = new byte[TEST_BLOCK_SIZE];
        byte[] actual;
        for (int i = 0; i < 256; ++i) {
            Arrays.fill(expected, (byte) (i - 128));
            actual = restoredStorage.readBlock(i);
            assertArrayEquals(expected, actual);
        }
    }

}
