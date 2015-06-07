/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tsushko.spos.fs;

import java.io.File;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author chernuhaiv@gmail.com
 */
public class FileSystemTest {
    
    public FileSystemTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of read method, of class FileSystem.
     */
    @Test
    public void testRead() throws Exception {
        System.out.println("read");
        int index = 0;
        int count = 0;
        FileSystem instance = null;
        byte[] expResult = null;
        byte[] result = instance.read(index, count);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of write method, of class FileSystem.
     */
    @Test
    public void testWrite() throws Exception {
        System.out.println("write");
        int index = 0;
        byte[] src = null;
        FileSystem instance = null;
        instance.write(index, src);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of lseek method, of class FileSystem.
     */
    @Test
    public void testLseek() {
        System.out.println("lseek");
        int index = 0;
        int pos = 0;
        FileSystem instance = null;
        instance.lseek(index, pos);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of create method, of class FileSystem.
     */
    @Test
    public void testCreate() throws Exception {
        System.out.println("create");
        String name = "";
        FileSystem instance = null;
        instance.create(name);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of destroy method, of class FileSystem.
     */
    @Test
    public void testDestroy() throws Exception {
        System.out.println("destroy");
        String name = "";
        FileSystem instance = null;
        instance.destroy(name);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of directory method, of class FileSystem.
     */
    @Test
    public void testDirectory() {
        System.out.println("directory");
        FileSystem instance = null;
        List<String> expResult = null;
        List<String> result = instance.directory();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of open method, of class FileSystem.
     */
    @Test
    public void testOpen() throws Exception {
        System.out.println("open");
        String name = "";
        FileSystem instance = null;
        int expResult = 0;
        int result = instance.open(name);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of close method, of class FileSystem.
     */
    @Test
    public void testClose() throws Exception {
        System.out.println("close");
        int index = 0;
        FileSystem instance = null;
        instance.close(index);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of backupStorage method, of class FileSystem.
     */
    @Test
    public void testBackupStorage() throws Exception {
        System.out.println("backupStorage");
        File file = null;
        FileSystem instance = null;
        instance.backupStorage(file);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
