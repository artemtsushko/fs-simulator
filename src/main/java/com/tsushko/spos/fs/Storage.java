package com.tsushko.spos.fs;

/**
 * Created by artem on 12.05.15.
 */
public interface Storage {
    int getBlocksNumber();
    int getBlockSize();
    byte[] readBlock(int blockNumber);
    void writeBlock(byte[] data, int blockNumber);
}
