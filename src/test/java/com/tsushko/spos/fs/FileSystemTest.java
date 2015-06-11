/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tsushko.spos.fs;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.*;

import static org.junit.Assert.*;

/**
 *
 * @author chernuhaiv@gmail.com
 */
public class FileSystemTest {
    private static FileSystemParams fsp;
    private static Storage storage;
    private static FileSystem fs;
    private static int opened;
    public FileSystemTest() {

    }
    
    @BeforeClass
    public static void setUpClass() throws FileNotFoundException,
            OpenFilesNumberException, FileAlreadyExistsException,
            ReadWriteException {
        fsp = FileSystemParams.getInstance(64, 64, 24, 5);
        fs = new FileSystem(fsp);
        System.out.println("FS created");

    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() throws FileNotFoundException,
            OpenFilesNumberException, FileAlreadyExistsException,
            ReadWriteException {
        fs.create("testFile");
        opened = fs.open("testFile");
        byte[] arr = "xyxy".getBytes();
        fs.write(0, arr);
        fs.close(opened);
        opened = fs.open("testFile");
    }
    
    @After
    public void tearDown() throws FileNotFoundException {
        fs.close(opened);
        fs.destroy("testFile");
    }

    /**
     * Test of read method, of class FileSystem.
     */
    @Test
    public void testRead() throws Exception {
        System.out.println("read on read write exception");
        int index = 0;
        int count = 4;
        byte[] expResult = {'x','y','x','y'};
        byte[] result = fs.read(index, count);
        System.out.println("byes count " + result.length);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of read method, of class FileSystem.
     */
    @Test(expected = ReadWriteException.class)
    public void testReadOnReadWriteException() throws Exception {
        System.out.println("read");
        int index = 0;
        int count = 5;
        byte[] expResult = {'x','y','x','y'};
        byte[] result = fs.read(index, count);
        assertArrayEquals(expResult, result);

    }
    /**
     * Test of write method, of class FileSystem.
     */
    @Test
    public void testWrite() throws Exception {
        System.out.println("write");
        byte[] src = {'x','y',
                    'x','y',
                    'x','y',
                    'x','y'};
        fs.write(opened, src);
    }
    /**
     * Test of write method, of class FileSystem.
     * Test on illegal file index.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteOnWriteExceptionIllegalIndex() throws Exception {
        System.out.println("write");
        byte[] src = {'x','y',
                'x','y',
                'x','y',
                'x','y'};

        fs.write(100, src);
    }

    /**
     * Test of write method, of class FileSystem.
     * Test on exceeding file size.
     */
    @Test(expected = ReadWriteException.class)
    public void testWriteOnWriteException() throws Exception {
        System.out.println("write");
        byte[] src = new byte[2048];
        for (int i = 0; i < 2048;++i)
            src[i] = 'x';
        fs.write(opened, src);
    }



    /**
     * Test of lseek method, of class FileSystem.
     */
    @Test
    public void testLseek() throws ReadWriteException {
        System.out.println("lseek");
        int pos = 4;
        fs.read(0, 4);
    }
    /**
     * Test of lseek method, of class FileSystem.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testLseekOutOfBound() {
        System.out.println("lseek");
        int pos = 30;
        fs.lseek(opened, pos);
    }


    /**
     * Test of directory method, of class FileSystem.
     */
    @Test
    public void testDirectory() {
        System.out.println("directory");
        List<String> expResult = new ArrayList<>();
        expResult.add("test\t0B");
        List<String> result = fs.directory();
        System.out.println(result);
        assertEquals(expResult, result);
    }
    
}
