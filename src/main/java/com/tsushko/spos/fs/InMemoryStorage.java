package com.tsushko.spos.fs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

/**
 * Emulates HDD as a sequence of blocks represented by array of bytes.
 * <p>
 * Implements <code>readBlock</code> and <code>writeBlock</code> methods of
 * <code>Storage</code> interface, as well as some additional methods
 * for saving it's state to file, such as {@link #saveToFile(File)} and
 * {@link #getStorageFromFile(File)}.
 *
 * @author Artem Tsushko
 * @version 1.0
 */
public class InMemoryStorage implements Storage, Serializable {

    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 1L;

    /**
     * log4j2 <code>Logger</code> object for this class
     */
    private static final Logger logger = LogManager.getLogger();

    /**
     * Number of blocks
     * @serial non-negative
     */
    private int blocksNumber;

    /**
     * Size of each block in bytes
     * @serial non-negative
     */
    private int blockSize;

    /**
     * Represents blocks of this emulated HDD
     * @serial array of length <code>blockSize * blocksNumber</code>
     */
    private byte[] storage;


    /**
     * Default public constructor for deserialization purposes
     */
    public InMemoryStorage() {
        blocksNumber = 0;
        blockSize = 0;
        storage = null;
    }

    /**
     * Creates instance with specified number of blocks and block size
     *
     * @param blocksNumber  number of blocks
     * @param blockSize     size of block in bytes
     */
    public InMemoryStorage(int blocksNumber, int blockSize) {
        this.blocksNumber = blocksNumber;
        this.blockSize = blockSize;
        this.storage = new byte[blocksNumber * blockSize];
    }

    public int getBlocksNumber() {
        return blocksNumber;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public byte[] readBlock(int blockNumber) {
        if (blockNumber < 0 || blockNumber >= blocksNumber) {
            String errorMessage = "Wrong block number: "
                    + "expected in range [0," + blocksNumber + "], "
                    + "actual " + blockNumber;
            throw new IndexOutOfBoundsException(errorMessage);
        }
        byte[] block = new byte[blockSize];
        System.arraycopy(storage, blockNumber * blockSize, block, 0, blockSize);
        return block;
    }

    public void writeBlock(byte[] data, int blockNumber) {

        if (data.length != blockSize) {
            String errorMessage = "Block size mismatch: "
                    + "expected " + blockSize + ", "
                    + "actual " + data.length;
            throw new IllegalArgumentException(errorMessage);
        }
        if (blockNumber < 0 || blockNumber >= blocksNumber) {
            String errorMessage = "Wrong block number: "
                    + "expected in range [0," + blocksNumber + "], "
                    + "actual " + blockNumber;
            throw new IndexOutOfBoundsException(errorMessage);
        }

        System.arraycopy(data, 0, storage, blockNumber * blockSize, blockSize);
    }

    /**
     * Saves this instance's state to specified file
     *
     * @param file the file to write a serialized form
     *             of this Storage to
     * @throws IOException if any usual I/O error occurs
     */
    public void saveToFile(File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(this);
        }
    }

    /**
     * Creates instance with specified number of blocks and block size
     *
     * @param blocksNumber  number of blocks
     * @param blockSize     size of block in bytes
     * @return a new instance of InMemoryStorage with specified parameters
     */
    public static InMemoryStorage getStorage(int blocksNumber, int blockSize) {
        return new InMemoryStorage(blocksNumber, blockSize);
    }

    /**
     * creates a new instance and recovers it's state from the specified file
     *
     * @param file the file that contains serialized instance of InMemoryStorage
     * @return a new instance with it's state recovered from specified file
     *
     * @throws IOException if any general I/O error occurs
     * @throws ClassNotFoundException if Class of a serialized object cannot be found
     */
    public static InMemoryStorage getStorageFromFile(File file)
            throws IOException, ClassNotFoundException {
        InMemoryStorage storage;
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            storage = (InMemoryStorage) ois.readObject();
        }
        return storage;
    }

}
