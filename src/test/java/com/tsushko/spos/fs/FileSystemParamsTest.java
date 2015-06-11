package com.tsushko.spos.fs;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by artem on 15.05.15.
 */
public class FileSystemParamsTest {
    @Test
    public void testGetInstance() {
        FileSystemParams fsp = FileSystemParams.getInstance(64,64,24,10);
        assertEquals(fsp.blocksForBitmap,1);
        assertEquals(fsp.blocksForINodes,6);
        assertEquals(fsp.filesBlockIndex,8);
    }

}
