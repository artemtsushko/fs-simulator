package com.tsushko.spos.fs;

import java.nio.ByteBuffer;
import java.util.Properties;

/**
 * Specifies all application-wide parameters, used for
 * both initialization and runtime.
 * <p>
 * Three parameters can be specified during initialization:
 * size of block, number of blocks and number of iNodes.
 * Values of the rest of parameters are either constant
 * or derived (calculated).
 *
 * @author Artem Tsushko
 * @version 1.0
 */
public class FileSystemParams {

    /**
     * current version of file system data format
     */
    public static final int FILE_SYSTEM_VERSION = 1;

    /**
     * number of bytes to store index of some iNode,
     * for example in directory entry or in OFT
     */
    public static final int BYTES_PER_INODE_INDEX = Integer.BYTES;

    /**
     * number of bytes to store index of some block,
     * for example in iNode
     */
    public static final int BYTES_PER_BLOCK_INDEX = Integer.BYTES;

    /**
     * number of bytes to store file size,
     * for example in iNode
     */
    public static final int BYTES_PER_FILE_SIZE = Integer.BYTES;

    /**
     * number of bytes to store a name of file
     * for example in directory entry
     */
    public static final int BYTES_PER_FILE_NAME = 4;

    /**
     * number of bytes to store a directory entry
     */
    public static final int BYTES_PER_DIRECTORY_ENTRY
            = BYTES_PER_FILE_NAME + BYTES_PER_INODE_INDEX;

    /**
     * Number of links to blocks of the <code>Storage</code> in each iNode
     */
    public static final int INODE_BLOCK_LINKS_NUMBER = 3;

    /**
     * Number of bytes that one iNode occupies on <code>Storage</code>
     */
    public static final int INODE_SIZE = BYTES_PER_FILE_SIZE
            + INODE_BLOCK_LINKS_NUMBER * BYTES_PER_BLOCK_INDEX;

    /**
     * Maximal length of file name in bytes
     */
    public static final int MAX_FILE_NAME_LENGTH = BYTES_PER_FILE_NAME;

    /**
     * Index of superblock - the block of emulated IO device
     * that contains filesystem metadata:
     * <ul>
     *     <li>FILE_SYSTEM_VERSION</li>
     *     <li>blockSize</li>
     *     <li>blocksNumber</li>
     *     <li>iNodesNumber</li>
     * </ul>
     */
    public static final int SUPER_BLOCK_INDEX = 0;

    /**
     * Size of superblock - the block of emulated IO device
     * that contains filesystem metadata:
     * <ul>
     *     <li>FILE_SYSTEM_VERSION</li>
     *     <li>blockSize</li>
     *     <li>blocksNumber</li>
     *     <li>iNodesNumber</li>
     * </ul>
     */
    public static final int SUPER_BLOCK_SIZE = 4 * Integer.BYTES;

    /**
     * Index of the first block of emulated IO device
     * that contains bitmap of free blocks
     */
    public static final int BITMAP_BLOCK_INDEX = 1;

    /**
     * The minimal size of emulated IO device block
     * so that it can hold the superblock
     * and that an iNode can occupy at most 2 blocks
     */
    public static final int MIN_BLOCK_SIZE = Math.max(SUPER_BLOCK_SIZE, INODE_SIZE);

    /**
     * Size of block in emulated IO device
     */
    public final int blockSize;

    /**
     * Number of blocks in emulated IO device
     */
    public final int blocksNumber;

    /**
     * Number of iNodes
     */
    public final int iNodesNumber;

    /**
     * Number of blocks allocated for free blocks bitmap in emulated IO device.
     */
    public final int blocksForBitmap;

    /**
     * Index of the first block of emulated IO device
     * that contains iNodes
     */
    public final int iNodesBlockIndex;

    /**
     * Number of blocks allocated for iNodes in emulated IO device.
     */
    public final int blocksForINodes;

    /**
     * Index of the first block of emulated IO device
     * that contains files and directories
     */
    public final int filesBlockIndex;

    /**
     * The maximum number of files that can be opened
     * at the same time, excluding the directory
     */
    public final int maxOpenFiles;

    /**
     * Size of the open files table
     */
    public final int openFilesTableSize;

    /**
     * Maximum length of file in bytes
     */
    public final int maxFileSize;


    /**
     * Takes all user-specified parameters as constructor arguments.
     * The rest of parameters are either constant or derived (calculated).
     *
     * @param blockSize    size of each block of emulated IO device
     * @param blocksNumber number of blocks in emulated IO device
     * @param iNodesNumber number of iNodes
     * @param maxOpenFilesNumber maximum number of files that can be opened
     *                           at the same time, excluding the directory
     *
     * @see Storage
     */
    private FileSystemParams(int blockSize, int blocksNumber, int iNodesNumber,
                             int maxOpenFilesNumber) {
        this.blockSize = blockSize;
        this.blocksNumber = blocksNumber;
        this.iNodesNumber = iNodesNumber;
        this.maxOpenFiles = maxOpenFilesNumber;

        if (this.blocksNumber % (Byte.SIZE * this.blockSize) == 0) {
            blocksForBitmap = this.blocksNumber / (Byte.SIZE * this.blockSize);
        } else {
            blocksForBitmap = this.blocksNumber / (Byte.SIZE * this.blockSize) + 1;
        }

        iNodesBlockIndex = BITMAP_BLOCK_INDEX + blocksForBitmap;

        if ((this.iNodesNumber * INODE_SIZE) % this.blockSize == 0) {
            blocksForINodes = (this.iNodesNumber * INODE_SIZE) / this.blockSize;
        } else {
            blocksForINodes = (this.iNodesNumber * INODE_SIZE) / this.blockSize + 1;
        }

        filesBlockIndex = iNodesBlockIndex + blocksForINodes;

        openFilesTableSize = maxOpenFiles + 1;

        maxFileSize = Math.min(blockSize * INODE_BLOCK_LINKS_NUMBER, Integer.MAX_VALUE);
    }

    /**
     * Takes all user-specified parameters as arguments and returns a new
     * instance of <code>FileSystemParams</code>, with the rest of parameters
     * either constant or derived (calculated).
     *
     * @param blockSize          size of each block of emulated IO device
     * @param blocksNumber       number of blocks in emulated IO device
     * @param iNodesNumber       number of iNodes
     * @param maxOpenFilesNumber maximum number of files that can be opened
     *                           at the same time, excluding the directory
     * @return new instance of FileSystemParams
     * @throws IllegalArgumentException if the input arguments don't fulfill
     *         the minimal requirements or don't match each other
     *
     * @see Storage
     */
    public static FileSystemParams getInstance(int blockSize, int blocksNumber,
                                               int iNodesNumber, int maxOpenFilesNumber) {
        if(blockSize < MIN_BLOCK_SIZE) {
            throw new IllegalArgumentException("The specified size of block is too small. "
                    + "The minimal supported block size is " + MIN_BLOCK_SIZE);
        }
        FileSystemParams instance = new FileSystemParams(blockSize,blocksNumber,
                                                         iNodesNumber,maxOpenFilesNumber);
        if(blocksNumber <= instance.filesBlockIndex) {
            throw new IllegalArgumentException("The specified number of blocks is too small. "
                    + "You need at least " + instance.filesBlockIndex + " blocks to hold "
                    + "the file system meta data + some number of blocks "
                    + "for your files and directories.");
        }
        return instance;
    }

    /**
     * Reads file system parameters from <code>Properties</code> object.
     * The latter must contain at least such properties:
     * <ul>
     * <li><code>blockSize</code> - size of block in emulated IO device</li>
     * <li><code>blocksNumber</code> - number of blocks in emulated IO device</li>
     * <li><code>iNodesNumber</code> - number of iNodes</li>
     * <li><code>maxOpenFilesNumber</code> -  maximum number of files
     * that can be opened at the same time, excluding the directory</li>
     * </ul>
     *
     * @param properties Properties object containing the properties listed above
     * @return new instance of FileSystemParams
     * @throws IllegalArgumentException if the specified parameters don't fulfill
     *         the minimal requirements or don't match each other
     * @see Storage
     */
    public static FileSystemParams getInstance(Properties properties) {
        int blockSize = Integer.parseInt(properties.getProperty("blockSize"));
        int blocksNumber = Integer.parseInt(properties.getProperty("blocksNumber"));
        int iNodesNumber = Integer.parseInt(properties.getProperty("iNodesNumber"));
        int maxOpenFilesNumber = Integer.parseInt(
                properties.getProperty("maxOpenFilesNumber"));
        return getInstance(blockSize, blocksNumber, iNodesNumber, maxOpenFilesNumber);
    }

    /**
     * Takes file system parameters from the superblock of specified Storage.
     *
     * @param storage a Storage object, restored from backup file
     * @param maxOpenFilesNumber maximum number of files that can be opened
     *                           at the same time, excluding the directory
     * @return new instance of FileSystemParams
     * @throws VersionMismatchException if the version of file system on the storage
     * doesn't match the current version of file system data format
     * @see #FILE_SYSTEM_VERSION
     * @see Storage
     */
    public static FileSystemParams getInstance(Storage storage, int maxOpenFilesNumber)
            throws VersionMismatchException{
        byte[] superblock = storage.readBlock(SUPER_BLOCK_INDEX);
        ByteBuffer byteBuffer = ByteBuffer.wrap(superblock);
        int version = byteBuffer.getInt();
        if(version != FILE_SYSTEM_VERSION)
            throw new VersionMismatchException();
        int blockSize = byteBuffer.getInt();
        int blocksNumber = byteBuffer.getInt();
        int iNodesNumber = byteBuffer.getInt();
        return new FileSystemParams(blockSize,blocksNumber,
                                    iNodesNumber,maxOpenFilesNumber);
    }
}