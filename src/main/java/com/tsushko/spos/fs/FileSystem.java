package com.tsushko.spos.fs;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Emulates simple file system.
 *
 * @author Artem Tsushko
 * @author Ivan Chernuha
 */
public class FileSystem {

 protected Storage storage;

    protected FileSystemParams params;


    /**
     * Open files table, which keeps track of open files
     * The <code>OFT[0]</code> entry is reserved for the directory,
     * it is always open
     */
    private File[] OFT;

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
        for (int i = 0; i < params.filesBlockIndex; ++i) {
            markBlockAsUsed(i);
        }

        // write free iNodes
        for (int i = 1; i < params.iNodesNumber; ++i) {
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

        // create OFT
        OFT = new File[params.openFilesTableSize];

        //open directory in OFT[0]
        OFT[0] = new File(directory);
        OFT[0].iNode.writeToStorage();
        for (int i = 1; i < params.maxOpenFiles; ++i) {
            OFT[i] = null;
        }
    }


    public void create(String filename) throws IllegalArgumentException {

    }


    public void destroy(String filename) throws IllegalArgumentException {

    }


    public int open(String filename) throws IllegalArgumentException {

        return -1;
    }


    public int close(int index) throws IllegalArgumentException {
        return -1;
    }


    public List<String> directory() {

        return null;
    }

    /**
     * Sequentially read a number of bytes from the file with specified index
     * starting from current position in the file
     *
     * @param index index of the file in OFT
     * @param count number of bytes to read
     * @return array of bytes read from the file
     * @throws ReadWriteException if end of file is reached before reading
     *         count bytes
     * @throws IllegalArgumentException if the file with specified index is not
     *         opened
     */
    public byte[] read(int index, int count) throws ReadWriteException {
        // file we are read from
        File file = OFT[index];

        if(file == null) {
            throw new IllegalArgumentException("No file opened with index " + index);
        }

        if (file.position + count > file.iNode.length) {
            throw new ReadWriteException("End of file will be reached before reading "
                                            + count + " bytes!");
        }

        // result buffer
        byte[] readBuf = new byte[count];
        int offset = 0;

        //until all bytes not read
        while (offset != count) {
            int length = file.buffer.length;

            // current position in file buffer
            int posBuffer = file.position % length;

            // current position in file buffer
            int dif = length - posBuffer;
            int toRead = count < dif ? count : dif;
//            if( dif == 0){
            System.arraycopy(file.buffer, posBuffer, readBuf, offset, toRead);
            offset += toRead;
            if(offset == count){
                break;
            }
//            System.arraycopy(file.buffer, 0, readBuf, offset, count );
            file.readNextBlockFromStorage();

        }

        return readBuf;
    }


    /**
     * Sequentially writes all bytes from <code>src</code> to the file
     * with specified index starting from current position in the file
     *
     * @param index index of the file in OFT
     * @param src array of bytes to write to file
     * @throws ReadWriteException if max file size was reached
     *         or if no free space left to expand the file
     * @throws IllegalArgumentException if the file with specified index
     *         is not opened
     */
    public int write(int index, byte[] src) throws ReadWriteException {
        File file = OFT[index];

        if(file == null) {
            throw new IllegalArgumentException("No file opened with index " + index);
        }

        INode inode = file.iNode;
        int offset = 0;

        while (offset != src.length) {
            int blength = file.buffer.length;

            // current position in file buffer
            int posBuffer = file.position % blength;

            //count bytes can be written in buffer
            int diff = blength - posBuffer;
            int count = diff < src.length ? diff : src.length;
            //full buffer
            if (count == 0) {
                file.writeBufferToStorage();
                posBuffer = 0;
            }
            System.arraycopy(src, offset, file.buffer, posBuffer, count);
            offset += count;
        }
        return offset;
    }

    /**
     * Moves the current position of the file with specified index
     * to <code>pos</code>, where pos is an integer specifying
     * the number of bytes from the beginning of the file.
     * <p>
     * When a file is initially opened, the current position
     * is automatically set to zero. After each read or write operation,
     * it points to the byte immediately following the one
     * that was accessed last.
     * <code>lseek</code> permits the position to be explicitly changed
     * without reading or writing the data. Seeking to position 0
     * implements a reset command, so that the entire file
     * can be reread or rewritten from the beginning.
     *
     * @param index index of the file in OFT
     * @param pos an integer specifying the number of bytes
     *            from the beginning of the file
     *
     * @throws IllegalArgumentException if if the file with specified index
     *         is not opened
     * @throws IndexOutOfBoundsException if pos is greater than the file length
     */
    public void lseek(int index, int pos) throws ReadWriteException {
        File f = OFT[index];
        if(f == null) {
            throw new IllegalArgumentException("No file opened with index " + index);
        }
        if (pos > f.iNode.length || pos < 0)
            throw new ReadWriteException(
                    "Invalid pos! The end of the file was reached.");
        int currentOffsetBlocks = f.position / params.blockSize;
        int targetOffsetBlocks = pos / params.blockSize;
        if(targetOffsetBlocks != currentOffsetBlocks) {
            // save current buffer to Storage
            storage.writeBlock(f.buffer,f.iNode.blockIndexes[currentOffsetBlocks]);
            // load target block from storage to buffer
            storage.readBlock(f.iNode.blockIndexes[targetOffsetBlocks]);
        }
        f.position = pos;
    }


    /**
     * An entry in open files table (OFT), keeps track of an open file
     */
    private class File {
        /**
         * An INode object representing the iNode assigned to this file
         */
        INode iNode;

        /**
         * A buffer used by read and write operations.
         * The buffer size is size of one block in the Storage
         */
        byte[] buffer;

        /**
         * The current position in the file
         */
        int position = 0;

        /**
         * Constructs a file table entry for the file
         * the specified iNode points to.
         *
         * @param iNode INode object representing a used iNode, that is
         *              an iNode is assigned to some file, it's length
         *              and first block index are not -1
         */
        public File(INode iNode) {
            assert iNode.length != -1;
            assert iNode.blockIndexes[0] != -1;
            this.iNode = iNode;
            //buffer = new byte[params.blockSize];
            //this.name = filename;
            buffer = storage.readBlock(iNode.blockIndexes[0]);
//            if( buffer != null)
//                position = buffer.length;
        }

        public void readNextBlockFromStorage() throws ReadWriteException {
            // current block of file
            int blockIndex = position / buffer.length;

            final int bound = FileSystemParams.INODE_BLOCK_LINKS_NUMBER;
            if (blockIndex < bound - 1 && blockIndex >= 0 &&
                    iNode.blockIndexes[blockIndex] != -1 ) {//+1
                buffer = storage.readBlock(iNode.blockIndexes[blockIndex + 1]);
                position += buffer.length;
            }else{
                throw new ReadWriteException("Cannot read file!");
            }
        }

        public void writeBufferToStorage() throws ReadWriteException {
            // current block of file
            int blockIndex = position / buffer.length;

            final int bound = FileSystemParams.INODE_BLOCK_LINKS_NUMBER;
            // some block was written
            boolean isWritten = false;
            for (int i = 0; i < bound; ++i) {
                //if free
                if (iNode.blockIndexes[i] == -1) {
                    int allBlock = findFreeBlock();
                    iNode.blockIndexes[i] = allBlock;
                    storage.writeBlock(buffer, allBlock);
                    markBlockAsUsed(allBlock);
                    iNode.length += buffer.length;
                    position += buffer.length;
                    isWritten = true;
                    break;
                }
            }
            if (!isWritten) {
                throw new ReadWriteException(
                        "File is too large! Not enough memory to hold it.");
            }
        }

    }

    ;

    /**
     * A high level representation of iNode on the Storage.
     * Allows to manipulate iNode fields. In order to apply any
     * changes this object should be written back to storage.
     */
    protected class INode {

        /**
         * index of the iNode represented, a value in range
         * from 0 to <code>params.iNodesNumber-1</code>
         *
         * @see FileSystemParams#iNodesNumber
         */
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
         *
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
         *
         * @param index        index of iNode this object will represent
         * @param length       length of file the iNode points to
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
            for (int blockIndex : blockIndexes) {
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
            System.arraycopy(iNodeBytes, 0, block1, offsetBytes, lengthInBlock1);
            storage.writeBlock(block1, params.iNodesBlockIndex + offsetBlocks);

            // if our iNode resides in two disk blocks
            if (lengthInBlock2 != 0) {
                byte[] block2 = storage.readBlock(params.iNodesBlockIndex + offsetBlocks + 1);
                System.arraycopy(iNodeBytes, lengthInBlock1, block2, 0, lengthInBlock2);
                storage.writeBlock(block2, params.iNodesBlockIndex + offsetBlocks + 1);
            }
        }
    }

    /**
     * writes down file system parameters to the superblock
     * of the specified <code>Storage</code>
     *
     * @see FileSystemParams
     * @see Storage
     */
    protected void writeSuperblock() {
        ByteBuffer superblockBuffer = ByteBuffer.allocate(params.blockSize);
//        ByteBuffer superblockBuffer = ByteBuffer.allocate(FileSystemParams.SUPER_BLOCK_SIZE);

        superblockBuffer.putInt(FileSystemParams.FILE_SYSTEM_VERSION);
        superblockBuffer.putInt(params.blockSize);
        superblockBuffer.putInt(params.blocksNumber);
        superblockBuffer.putInt(params.iNodesNumber);
        superblockBuffer.flip();
        byte[] superblock = new byte[params.blockSize];
//        byte[] superblock = new byte[FileSystemParams.SUPER_BLOCK_SIZE];
        superblockBuffer.get(superblock);
        storage.writeBlock(superblock, FileSystemParams.SUPER_BLOCK_INDEX);

    }

    /**
     * marks the block with specified index as free
     * in free blocks bitmap
     *
     * @param blockNumber index of the block
     */
    protected void markBlockAsFree(int blockNumber) {
        int offsetBlocks = FileSystemParams.BITMAP_BLOCK_INDEX
                + blockNumber / (params.blockSize * Byte.SIZE);
        int offsetBytes = (blockNumber % (params.blockSize * Byte.SIZE)) / Byte.SIZE;
        int offsetBits = (blockNumber % (params.blockSize * Byte.SIZE)) % Byte.SIZE;
        byte[] bitmapBlock = storage.readBlock(offsetBlocks);
        byte b = bitmapBlock[offsetBytes];
        b = (byte) (b & ~(1 << offsetBits));
        bitmapBlock[offsetBytes] = b;
        storage.writeBlock(bitmapBlock, offsetBlocks);
    }

    /**
     * marks the block with specified index as used
     * in free blocks bitmap
     *
     * @param blockNumber index of the block
     */
    protected void markBlockAsUsed(int blockNumber) {
        int offsetBlocks = FileSystemParams.BITMAP_BLOCK_INDEX
                + blockNumber / (params.blockSize * Byte.SIZE);
        int offsetBytes = (blockNumber % (params.blockSize * Byte.SIZE)) / Byte.SIZE;
        int offsetBits = (blockNumber % (params.blockSize * Byte.SIZE)) % Byte.SIZE;
        byte[] bitmapBlock = storage.readBlock(offsetBlocks);
        byte b = bitmapBlock[offsetBytes];
        b = (byte) (b | (1 << offsetBits));
        bitmapBlock[offsetBytes] = b;
        storage.writeBlock(bitmapBlock, offsetBlocks);
    }

    /**
     * Searches through the free space bitmap and returns
     * the index of a free block or -1 if no free space left
     *
     * @return the index of a free block or -1 if not found
     */
    protected int findFreeBlock() {
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
                    if (((bitmapByte >> bitmapBitIndex) & 1) != 0) {
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
     * constructs a new INode object that is a representation
     * of the iNode with specified index stored on Storage
     *
     * @param index index of iNode to read from Storage
     * @return An INode object representing the iNode
     * with specified index on Storage
     */
    protected INode readINodeFromStorage(int index) {
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
        System.arraycopy(block1, offsetBytes, iNodeBytes, 0, lengthInBlock1);

        // if our iNode resides in two disk blocks
        if (lengthInBlock2 != 0) {
            byte[] block2 = storage.readBlock(params.iNodesBlockIndex + offsetBlocks + 1);
            System.arraycopy(block2, 0, iNodeBytes, lengthInBlock1, lengthInBlock2);
        }

        int length;
        int[] blockIndexes = new int[FileSystemParams.INODE_BLOCK_LINKS_NUMBER];

        // take length and blockIndexes out from byte representation
        ByteBuffer buffer = ByteBuffer.wrap(iNodeBytes);
        length = buffer.getInt();
        for (int i = 0; i < blockIndexes.length; ++i) {
            blockIndexes[i] = buffer.getInt();
        }

        // construct object
        return new INode(index, length, blockIndexes);
    }

    /**
     * Looks for an iNode not yet assigned to any file and
     * returns the INode object representing it or
     * <code>null</code> in case no free iNodes left
     *
     * @return an INode object representing the free iNode
     * or null if not found
     */
    protected INode findFreeINode() {

        for (int i = 0; i < params.iNodesNumber; ++i) {
            INode iNode = readINodeFromStorage(i);
            if (iNode.length == -1) {
                return iNode;
            }
        }
        return null;
    }


}
