package com.tsushko.spos.fs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

/**
 * Created by artem on 12.05.15.
 */
public class InMemoryStorage implements Storage, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LogManager.getLogger();
    private int blocksNumber;
    private int blockSize;
    private byte[] storage;

    public InMemoryStorage() {
        blocksNumber = 0;
        blockSize = 0;
        storage = null;
    }

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

    public void saveToFile(File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(this);
        }
    }

    public static InMemoryStorage getStorage(int blocksNumber, int blockSize) {
        return new InMemoryStorage(blocksNumber, blockSize);
    }

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
