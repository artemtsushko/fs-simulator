package com.tsushko.spos.fs;

/**
 * Defines basic operations that every storage device emulator must support
 *
 * @author Artem Tsushko
 * @version 1.0
 */
public interface Storage {

    /**
     * @return number of blocks
     */
    int getBlocksNumber();

    /**
     * @return size of each block in bytes
     */
    int getBlockSize();

    /**
     * reads a block with specified <code>blockNumber</code>
     * and returns a copy of it's contents
     *
     * @param blockNumber the index of block to read
     * @return copy of contents of specified block as byte[]
     *
     * @throws IndexOutOfBoundsException if blockNumber is not in range
     *         from 0 inclusive to blocksNumber-1 exclusive.
     *
     * @see #getBlocksNumber()
     */
    byte[] readBlock(int blockNumber);

    /**
     * writes the <code>data</code> to block with specified
     * <code>blockNumber</code>.
     *
     * @param data the data to be copied to the specified block.
     *             The length of the data must match the blockSize.
     * @param blockNumber the index of block to write data to
     *
     * @throws IllegalArgumentException if the length of data
     *         doesn't match the blockSize
     * @throws IndexOutOfBoundsException if blockNumber is not in range
     *         from 0 inclusive to blocksNumber-1 exclusive.
     *
     * @see #getBlockSize()
     */
    void writeBlock(byte[] data, int blockNumber);
}
