package com.tsushko.spos.fs;

import java.nio.ByteBuffer;

/**
 * Emulates simple file system.
 *
 * @author Artem Tsushko
 * @author Ivan Chernuha
 */
public class FileSystem {

    private Storage storage;

    private FileSystemParams params;

    /**
     * Constructs new <code>FileSystem</code> with specified parameters
     * on new <code>InMemoryStorage</code>
     *
     * @param fileSystemParams contains runtime and initialization parameters
     */

    public FileSystem(FileSystemParams fileSystemParams) {
        params = fileSystemParams;
        storage = InMemoryStorage.getStorage(params.blocksNumber, params.blockSize);

        writeSuperblock();

        // mark meta-data blocks as used
        for(int i = 0; i < params.filesBlockIndex; ++i) {
            markBlockAsUsed(i);
        }

        // write free iNodes
        for(int i = 1; i < params.iNodesNumber; ++i) {
            INode iNode = new INode(i);
            iNode.writeToStorage();
        }

        // write directory
        INode directory = new INode(0);
        int firstDirectoryBlock = findFreeBlock();
        assert firstDirectoryBlock == params.filesBlockIndex;
        markBlockAsUsed(firstDirectoryBlock);
        directory.blockIndexes[0] = firstDirectoryBlock;
        directory.length = 0;
        directory.writeToStorage();

        //TODO: open directory in OFT[0]
    }

    /**
     * writes down file system parameters to the superblock
     * of the specified <code>Storage</code>
     *
     * @see FileSystemParams
     * @see Storage
     */
    private void writeSuperblock() {
        ByteBuffer superblockBuffer = ByteBuffer.allocate(params.blockSize);
        superblockBuffer.putInt(FileSystemParams.FILE_SYSTEM_VERSION);
        superblockBuffer.putInt(params.blockSize);
        superblockBuffer.putInt(params.blocksNumber);
        superblockBuffer.putInt(params.iNodesNumber);
        superblockBuffer.flip();
        byte[] superblock = new byte[params.blockSize];
        superblockBuffer.get(superblock);
        storage.writeBlock(superblock,FileSystemParams.SUPER_BLOCK_INDEX);
    }

    /**
     * marks the block with specified index as free
     * in free blocks bitmap
     *
     * @param blockNumber index of the block
     */
    private void markBlockAsFree(int blockNumber) {
        int offsetBlocks = FileSystemParams.BITMAP_BLOCK_INDEX
                + blockNumber / (params.blockSize * Byte.SIZE);
        int offsetBytes = (blockNumber % (params.blockSize * Byte.SIZE)) / Byte.SIZE;
        int offsetBits = (blockNumber % (params.blockSize * Byte.SIZE)) % Byte.SIZE;
        byte[] bitmapBlock = storage.readBlock(offsetBlocks);
        byte b = bitmapBlock[offsetBytes];
        b = (byte) (b & ~(1 << offsetBits));
        bitmapBlock[offsetBytes] = b;
        storage.writeBlock(bitmapBlock,offsetBlocks);
    }

    /**
     * marks the block with specified index as used
     * in free blocks bitmap
     *
     * @param blockNumber index of the block
     */
    private void markBlockAsUsed(int blockNumber) {
        int offsetBlocks = FileSystemParams.BITMAP_BLOCK_INDEX
                + blockNumber / (params.blockSize * Byte.SIZE);
        int offsetBytes = (blockNumber % (params.blockSize * Byte.SIZE)) / Byte.SIZE;
        int offsetBits = (blockNumber % (params.blockSize * Byte.SIZE)) % Byte.SIZE;
        byte[] bitmapBlock = storage.readBlock(offsetBlocks);
        byte b = bitmapBlock[offsetBytes];
        b = (byte) (b | (1 << offsetBits));
        bitmapBlock[offsetBytes] = b;
        storage.writeBlock(bitmapBlock,offsetBlocks);
    }

    /**
     * Searches through the free space bitmap and returns
     * the index of a free block or -1 if no free space left
     *
     * @return the index of a free block or -1 if not found
     */
    private int findFreeBlock() {
        for (int bitmapBlockIndex = 0;
             bitmapBlockIndex < params.blocksForBitmap;
             ++bitmapBlockIndex) {
            byte[] biteBlock = storage.readBlock(
                    bitmapBlockIndex + FileSystemParams.BITMAP_BLOCK_INDEX);
            for (int bitmapByteIndex = 0;
                 bitmapByteIndex < params.blockSize;
                 ++bitmapByteIndex) {
                byte bitmapByte = biteBlock[bitmapByteIndex];
                for (int bitmapBitIndex = 0;
                     bitmapBitIndex < Byte.SIZE;
                     ++bitmapBitIndex) {
                    if (((bitmapByte>>bitmapBitIndex) & 1) != 0) {
                        int index = (((bitmapBlockIndex * params.blockSize)
                                        + bitmapByteIndex) * Byte.SIZE)
                                        + bitmapBitIndex;
                        if (index < params.blocksNumber)
                            return index;
                        else
                            return -1;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * A high level representation of iNode on the Storage.
     * Allows to manipulate iNode fields. In order to apply any
     * changes this object should be written back to storage.
     */
    public class INode {

        /**
         * index of the iNode represented, a value in range
         * from 0 to <code>params.iNodesNumber-1</code>
         *
         * @see FileSystemParams#iNodesNumber
         * */
        public int index;

        /**
         * the length of the file this iNode points to
         * the value -1 means that this iNode is free
         */
        public int length;

        /**
         * the indexes of emulated storage blocks this iNode points to.
         * If next block is not used, it's index is -1.
         * If {@link FileSystemParams#INODE_BLOCK_LINKS_NUMBER}
         * is not sufficient to hold all file blocks, the last block index
         * points to a block, that holds indexes of another file blocks
         */
        public int[] blockIndexes;

        /**
         * constructs free iNode representation with specified index
         * @param index index of iNode this object will represents
         */
        public INode(int index) {
            this.index = index;
            length = -1;
            blockIndexes = new int[FileSystemParams.INODE_BLOCK_LINKS_NUMBER];
            for (int i = 0; i < blockIndexes.length; ++i) {
                blockIndexes[i] = -1;
            }
        }

        /**
         * takes all iNode fields as constructor arguments
         * @param index index of iNode this object will represent
         * @param length length of file the iNode points to
         * @param blockIndexes indexes of emulated IO device blocks
         *                     occupied by the file this iNode points to
         */
        private INode(int index, int length, int[] blockIndexes) {
            this.index = index;
            this.length = length;
            this.blockIndexes = blockIndexes;
        }

        public void writeToStorage() {
            // check index
            if (index >= params.iNodesNumber) {
                throw new IndexOutOfBoundsException("incorrect iNode index: "
                        + "expected in range [0," + params.iNodesNumber + "), "
                        + "actual " + index);
            }

            // get byte[] representation of this iNode
            byte[] iNodeBytes = new byte[FileSystemParams.INODE_SIZE];
            ByteBuffer buffer = ByteBuffer.allocate(FileSystemParams.INODE_SIZE);
            buffer.putInt(length);
            for(int blockIndex : blockIndexes) {
                buffer.putInt(blockIndex);
            }
            buffer.flip();
            buffer.get(iNodeBytes);

            // offset relative to first block containing iNodes: params.iNodesBlockIndex
            int offsetBytes = index * FileSystemParams.INODE_SIZE % params.blockSize;
            int offsetBlocks = index * FileSystemParams.INODE_SIZE / params.blockSize;

            // the length of the part of iNode bytes that will be written to the next block
            int lengthInBlock2 = (offsetBytes + FileSystemParams.INODE_SIZE) % params.blockSize;

            // the length of the part of iNode bytes that will be written to the current block
            int lengthInBlock1 = FileSystemParams.INODE_SIZE - lengthInBlock2;

            byte[] block1 = storage.readBlock(params.iNodesBlockIndex + offsetBlocks);
            System.arraycopy(iNodeBytes,0,block1,offsetBytes,lengthInBlock1);
            storage.writeBlock(block1,params.iNodesBlockIndex + offsetBlocks);

            // if our iNode resides in two disk blocks
            if (lengthInBlock2 != 0) {
                byte[] block2 = storage.readBlock(params.iNodesBlockIndex + offsetBlocks + 1);
                System.arraycopy(iNodeBytes,lengthInBlock1,block2,0,lengthInBlock2);
                storage.writeBlock(block2,params.iNodesBlockIndex + offsetBlocks + 1);
            }
        }
    }

    /**
     * constructs a new INode object that is a representation
     * of the iNode with specified index stored on Storage
     *
     * @param index index of iNode to read from Storage
     * @return An INode object representing the iNode
     *         with specified index on Storage
     */
    private INode readINodeFromStorage(int index) {
        if (index >= params.iNodesNumber) {
            throw new IndexOutOfBoundsException("incorrect iNode index: "
                    + "expected in range [0," + params.iNodesNumber + "), "
                    + "actual " + index);
        }

        // offset relative to first block containing iNodes: params.iNodesBlockIndex
        int offsetBytes = index * FileSystemParams.INODE_SIZE % params.blockSize;
        int offsetBlocks = index * FileSystemParams.INODE_SIZE / params.blockSize;

        // the length of the part of iNode bytes that will be written to the next block
        int lengthInBlock2 = (offsetBytes + FileSystemParams.INODE_SIZE) % params.blockSize;

        // the length of the part of iNode bytes that will be written to the current block
        int lengthInBlock1 = FileSystemParams.INODE_SIZE - lengthInBlock2;

        // get byte[] representation of the iNode constructed
        byte[] iNodeBytes = new byte[FileSystemParams.INODE_SIZE];

        byte[] block1 = storage.readBlock(params.iNodesBlockIndex + offsetBlocks);
        System.arraycopy(block1,offsetBytes,iNodeBytes,0,lengthInBlock1);

        // if our iNode resides in two disk blocks
        if (lengthInBlock2 != 0) {
            byte[] block2 = storage.readBlock(params.iNodesBlockIndex + offsetBlocks + 1);
            System.arraycopy(block2,0,iNodeBytes,lengthInBlock1,lengthInBlock2);
        }

        int length;
        int[] blockIndexes = new int[FileSystemParams.INODE_BLOCK_LINKS_NUMBER];

        // take length and blockIndexes out from byte representation
        ByteBuffer buffer = ByteBuffer.wrap(iNodeBytes);
        length = buffer.getInt();
        for(int i = 0; i < blockIndexes.length; ++i) {
            blockIndexes[i] = buffer.getInt();
        }

        // construct object
        return new INode(index,length,blockIndexes);
    }

    /**
     * Looks for an iNode not yet assigned to any file and
     * returns the INode object representing it or
     * <code>null</code> in case no free iNodes left
     *
     * @return an INode object representing the free iNode
     *         or null if not found
     *
     */
    public INode findFreeINode() {

        for (int i = 0; i < params.iNodesNumber; ++i) {
            INode iNode = readINodeFromStorage(i);
            if (iNode.length == -1) {
                return iNode;
            }
        }
        return null;
    }

}
