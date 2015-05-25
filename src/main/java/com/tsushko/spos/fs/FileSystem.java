package com.tsushko.spos.fs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Emulates simple file system
 *
 * @author Artem Tsushko
 */
public class FileSystem {

    private static final Logger logger = LogManager.getLogger();

    private Storage storage;

    private FileSystemParams params;

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

        // create OFT
        OFT = new File[params.openFilesTableSize];

        //open directory in OFT[0]
        OFT[0] = new File(directory);
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
    private class INode {

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
    private INode findFreeINode() {

        for (int i = 0; i < params.iNodesNumber; ++i) {
            INode iNode = readINodeFromStorage(i);
            if (iNode.length == -1) {
                return iNode;
            }
        }
        return null;
    }

    /**
     * An entry in open files table (OFT), keeps track of an open file
     */
    private class File{
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
         * Indicates whether the buffered block was modified since
         * it was loaded from or last written to the Storage
         */
        boolean modified;

        /**
         * The current position in the file
         */
        int position = 0;

        /**
         * The index of the block link corresponding to current position
         * in the file
         */
        int currentBlockLinkIndex = 0;

        /**
         * The index of the block link corresponding to the block in the buffer
         */
        int bufferedBlockLinkIndex = -1;

        /**
         * Constructs a file table entry for the file
         * the specified iNode points to.
         *
         * @param iNode INode object representing a used iNode, that is
         *              an iNode is assigned to some file, it's length
         *              and first block index are not -1
         */
        public File(INode iNode) {
            if(iNode.length == -1 || iNode.blockIndexes[0] == -1) {
                throw new IllegalArgumentException(
                        "the specified iNode is free, "
                        + "that is doesn't point to any file");
            }
            this.iNode = iNode;
        }


        /**
         * Reads <code>count</code> bytes from this file starting from
         * the current position in the file
         * @param count number of bytes to read
         * @return the bytes read from this file
         * @throws ReadWriteException if end of file is reached before reading
         *         count bytes
         */
        public byte[] read(int count) throws ReadWriteException{

            if (position + count > iNode.length) {
                throw new ReadWriteException("End of file will be reached before reading "
                        + count + " bytes!");
            }

            // the buffer to collect the data read from this file
            byte[] result = new byte[count];

            if(currentBlockLinkIndex != bufferedBlockLinkIndex) {
                loadCurrentBlock();
            }

            // position in the current block
            int offsetInBlock = position % params.blockSize;

            // number of bytes to read from the current block
            int bytesToRead = Math.min(count, params.blockSize - offsetInBlock);

            // current write position in result buffer
            int offsetInResult = 0;

            System.arraycopy(buffer,offsetInBlock,result,offsetInResult,bytesToRead);

            offsetInResult += bytesToRead;
            position += bytesToRead;
            currentBlockLinkIndex =  position / params.blockSize;
            count -= bytesToRead;

            offsetInBlock = 0;
            while (count != 0) {
                assert count > 0;
                loadCurrentBlock();
                bytesToRead = Math.min(count, params.blockSize);
                System.arraycopy(buffer,offsetInBlock,result,offsetInResult,bytesToRead);
                offsetInResult += bytesToRead;
                position += bytesToRead;
                currentBlockLinkIndex =  position / params.blockSize;
                count -= bytesToRead;
            }
            return result;
        }

        /**
         * Loads the block corresponding to <code>currentBlockLinkIndex</code>
         * (if it exists) into <code>buffer</code> or allocates a new block
         * if <code>currentBlockLinkIndex</code> is -1.
         * @throws ReadWriteException if no free space left to allocate a new block
         */
        void loadCurrentBlock() throws ReadWriteException{
            if(modified) {
                // should not happen because in this case modified will be false
                assert bufferedBlockLinkIndex != -1;

                storage.writeBlock(buffer,iNode.blockIndexes[bufferedBlockLinkIndex]);
            }
            if (iNode.blockIndexes[currentBlockLinkIndex] != -1) {
                buffer = storage.readBlock(iNode.blockIndexes[currentBlockLinkIndex]);
            } else {
                int freeBlockIndex = findFreeBlock();
                if (freeBlockIndex == -1) {
                    // update file length
                    iNode.length = position;
                    iNode.writeToStorage();

                    throw new ReadWriteException("No free space left "
                            + "to expand the file");
                }
                iNode.blockIndexes[currentBlockLinkIndex] = freeBlockIndex;
                markBlockAsUsed(freeBlockIndex);
                iNode.writeToStorage();
                buffer = new byte[params.blockSize];
            }
            modified = false;
            bufferedBlockLinkIndex = currentBlockLinkIndex;
        }

        /**
         * sequentially writes all bytes from <code>src</code> to this file
         * starting from current position in the file
         *
         * @param src array of bytes to write to file
         * @throws ReadWriteException if no free space left to expand the file
         */
        public void write(byte[] src) throws ReadWriteException{

            // number of bytes to write
            int count = src.length;

            if(currentBlockLinkIndex != bufferedBlockLinkIndex) {
                loadCurrentBlock();
            }

            // position in the current block
            int offsetInBlock = position % params.blockSize;

            // number of bytes to write to the current block
            int bytesToWrite = Math.min(count, params.blockSize - offsetInBlock);

            // current read position in source buffer
            int offsetInSource = 0;

            // write to the buffer
            System.arraycopy(src,offsetInSource,buffer,offsetInBlock,bytesToWrite);
            modified = true;

            offsetInSource += bytesToWrite;
            position += bytesToWrite;
            currentBlockLinkIndex =  position / params.blockSize;
            count -= bytesToWrite;

            offsetInBlock = 0;
            while (count != 0) {
                assert count > 0;
                loadCurrentBlock();
                bytesToWrite = Math.min(count, params.blockSize);

                // write to the buffer
                System.arraycopy(src,offsetInSource,buffer,offsetInBlock,bytesToWrite);
                modified = true;

                offsetInSource += bytesToWrite;
                position += bytesToWrite;
                currentBlockLinkIndex =  position / params.blockSize;
                count -= bytesToWrite;
            }

            // update file length
            iNode.length = Math.max(iNode.length, position);
            iNode.writeToStorage();

        }

        /**
         * moves the current position of this file to <code>pos</code>,
         * where pos is an integer specifying the number of bytes
         * from the beginning of the file.
         *
         * @throws IndexOutOfBoundsException if pos is greater than
         * the file length or less then zero.
         */
        public void lseek(int pos) {
            if (pos > iNode.length || pos < 0)
                throw new IndexOutOfBoundsException("Invalid pos! "
                        + "The maximum allowed pos is immediately after "
                        + "the end of file, the minimum pos is 0, "
                        + "the specified pos is " + pos);
            currentBlockLinkIndex = pos / params.blockSize;
            position = pos;
        }
    };


    /**
     * sequentially read a number of bytes from the file with specified index
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

        File file = OFT[index];

        if(file == null) {
            throw new IllegalArgumentException("No file opened with index " + index);
        }

        return file.read(count);
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
    public void write(int index, byte[] src) throws ReadWriteException{

        File file = OFT[index];

        if(file == null) {
            throw new IllegalArgumentException("No file opened with index " + index);
        }
        if (file.position + src.length > params.maxFileSize) {
            throw new ReadWriteException("Max file size will be reached " +
                    "before writing " + src.length + " bytes");
        }

        file.write(src);

    }


    /**
     * moves the current position of the file with specified index
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
    public void lseek(int index, int pos) {
        File file = OFT[index];
        if(file == null) {
            throw new IllegalArgumentException("No file opened with index " + index);
        }
        file.lseek(pos);
    }

    /**
     * a convenience class to represent a directory entry
     */
    private class DirectoryEntry {

        /**
         * the name of file or null if entry is empty
         */
        String name;

        /**
         * the index of iNode corresponding to the file
         */
        int iNodeIndex;


        /**
         * constructs an object representation of a directory entry
         * with given parameters, that can be later converted to bytes
         * and written to directory file on storage
         *
         * @param fileName name of file,
         *                 max {@link FileSystemParams#BYTES_PER_FILE_NAME}
         *                 bytes. If the name is larger, it will be cut.
         * @param iNodeIndex index of iNode representing the file
         */
        public DirectoryEntry(String fileName, int iNodeIndex) {

            // construct name
            if(fileName != null
                    && fileName.length()
                    > FileSystemParams.BYTES_PER_FILE_NAME)
                this.name = fileName.substring(
                        0,FileSystemParams.BYTES_PER_FILE_NAME);
            else
                this.name = fileName;

            // construct iNode index
            this.iNodeIndex = iNodeIndex;
        }

        /**
         * Constructs an instance of the DirectoryEntry class
         * form a directory entry's byte representation
         * in the directory file on storage
         * @param byteRepresentation directory entry's byte representation
         *                           in the directory file on storage
         */
        public DirectoryEntry(byte[] byteRepresentation) {

            // construct name
            StringBuilder buffer = new StringBuilder();
            int i;
            for (i = 0; i < FileSystemParams.BYTES_PER_FILE_NAME; ++i) {
                if (byteRepresentation[i] == 0)
                    break;
                buffer.append((char) byteRepresentation[i]);
            }
            if (i != 0) {
                name = buffer.toString();
            } else {
                name = null;
            }

            // get iNode index
            iNodeIndex = ByteBuffer.wrap(byteRepresentation,
                            FileSystemParams.BYTES_PER_FILE_NAME,
                            FileSystemParams.BYTES_PER_INODE_INDEX)
                    .getInt();

        }



        /**
         * returns byte representation of this entry
         * @return byte representation of this entry
         */
        byte[] getBytes() {
            byte[] bytes = new byte[FileSystemParams.BYTES_PER_DIRECTORY_ENTRY];

            // get name bytes
            if (name != null && name.length() > 0) {
                byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
                System.arraycopy(nameBytes, 0, bytes, 0,
                        Math.min(nameBytes.length,
                                FileSystemParams.BYTES_PER_DIRECTORY_ENTRY));
            }

            //get iNodeIndex bytes
            ByteBuffer buffer =
                    ByteBuffer.allocate(FileSystemParams.BYTES_PER_INODE_INDEX);
            buffer.putInt(iNodeIndex);
            buffer.flip();
            buffer.get(bytes,
                    FileSystemParams.BYTES_PER_DIRECTORY_ENTRY,
                    FileSystemParams.BYTES_PER_INODE_INDEX);

            return bytes;
        }

        /**
         * writes this directory entry to the directory slot
         * with specified index
         *
         * @param index index of the target directory slot
         */
        public void writeToDirectory(int index) {
            try {
                lseek(0, index * FileSystemParams.BYTES_PER_DIRECTORY_ENTRY);
                write(0, getBytes());

            } catch (Exception e) {
                throw new IllegalArgumentException("The directory slot " +
                        "with index " + index + " doesn't exist");
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DirectoryEntry that = (DirectoryEntry) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    /**
     * returns index of the directory entry corresponding to the specified file
     * or -1 if the file is not present in the directory
     * @param fileName symbolic name of the file to look for
     * @return index of the directory entry corresponding to the specified file
     *         or -1 if the file is not present in the directory
     */
    private int findFileInDirectory(String fileName) {
        DirectoryEntry searchPattern = new DirectoryEntry(fileName,-1);
        DirectoryEntry currentEntry;
        File directory = OFT[0];

        directory.lseek(0);
        for (int pos = 0;
             pos < directory.iNode.length;
             pos += FileSystemParams.BYTES_PER_DIRECTORY_ENTRY) {
            try {
                currentEntry = new DirectoryEntry(
                        directory.read(FileSystemParams.BYTES_PER_DIRECTORY_ENTRY));
                if (currentEntry.equals(searchPattern)) {
                    return pos / FileSystemParams.BYTES_PER_DIRECTORY_ENTRY;
                }
            } catch (ReadWriteException e) {
                logger.error(e);
            }
        }
        return -1;
    }

    /**
     * searches through the directory and returns the index of first free slot
     * or -1 if the directory is full
     * @return  index of the first free slot in the directory
     *          or -1 if the directory is full
     */
    private int findFreeDirectoryEntry() {
        int slotIndex = 0;
        File directory = OFT[0];
        byte[] searchPattern
                = new byte[FileSystemParams.BYTES_PER_DIRECTORY_ENTRY];
        byte[] currentEntry;


        directory.lseek(0);
        for (int pos = 0;
             pos < directory.iNode.length;
             pos += FileSystemParams.BYTES_PER_DIRECTORY_ENTRY) {
            try {
                currentEntry = directory.read(FileSystemParams.BYTES_PER_DIRECTORY_ENTRY);
                if (Arrays.equals(currentEntry, searchPattern)) {
                    return pos / FileSystemParams.BYTES_PER_DIRECTORY_ENTRY;
                }
            } catch (ReadWriteException e) {
                logger.error(e);
            }
        }
        if(params.maxFileSize - directory.iNode.length
                > FileSystemParams.BYTES_PER_DIRECTORY_ENTRY) {
            return directory.iNode.length
                    / FileSystemParams.BYTES_PER_DIRECTORY_ENTRY;
        } else {
            return -1;
        }
    }

    /**
     * reads the directory entry with specified index
     * from the directory on storage
     *
     * @param index the index of the directory entry
     * @return object representation of the directory
     *         entry with specified index
     */
    private DirectoryEntry readDirectoryEntry(int index) {
        try {
            lseek(0, index * FileSystemParams.BYTES_PER_DIRECTORY_ENTRY);
            byte[] directoryEntryBytes
                    = read(0,FileSystemParams.BYTES_PER_DIRECTORY_ENTRY);
            return new DirectoryEntry(directoryEntryBytes);

        } catch (Exception e) {
            throw new IllegalArgumentException("The directory entry " +
                    "with index " + index + " doesn't exist");
        }
    }

    /**
     * frees the directory slot with specified index
     * @param index index of the directory entry
     *              that will be removed
     */
    private void removeDirectoryEntry(int index) {
        byte[] freeSlot
                = new byte[FileSystemParams.BYTES_PER_DIRECTORY_ENTRY];
        try {
            lseek(0, index * FileSystemParams.BYTES_PER_DIRECTORY_ENTRY);
            write(0, freeSlot);

        } catch (Exception e) {
            throw new IllegalArgumentException("The directory entry " +
                    "with index " + index + " doesn't exist");
        }
    }

    /**
     * creates a new file with specified name
     * @param name symbolic file name, maximum length is
     *             {@link FileSystemParams#MAX_FILE_NAME_LENGTH}
     * @throws FileAlreadyExistsException if the file with specified
     *                                    symbolic name already exists
     *                                    in the directory
     * @throws ReadWriteException   if the file system is out of some resource,
     *                              like storage blocks, iNodes
     *                              or directory slots
     */
    public void create(String name)
            throws  FileAlreadyExistsException,
                    ReadWriteException {

        // check if the file with specified name exists
        if (findFileInDirectory(name) != -1)
            throw new FileAlreadyExistsException();

        // find free directory slot
        int dirEntryIndex = findFreeDirectoryEntry();
        if (dirEntryIndex == -1) {
            throw new ReadWriteException("Out of free directory slots");
        }

        // find free iNode
        INode iNode = findFreeINode();
        if (iNode == null) {
            throw new ReadWriteException("Out of free iNodes");
        }
        int iNodeIndex = iNode.index;

        // find free storage block
        int blockIndex = findFreeBlock();
        if (blockIndex == -1) {
            throw new ReadWriteException("Out of free space");
        }

        // create a file
        iNode.length = 0;
        iNode.blockIndexes[0] = blockIndex;
        markBlockAsUsed(blockIndex);
        iNode.writeToStorage();
        DirectoryEntry directoryEntry = new DirectoryEntry(name,iNodeIndex);
        directoryEntry.writeToDirectory(dirEntryIndex);

    }

    /**
     * removes the file with specified name and frees resources
     *
     * @param name symbolic name of the file
     */
    public void destroy(String name)
            throws  FileNotFoundException {

        int dirEntryIndex = findFileInDirectory(name);

        // check if the file with specified name exists
        if (dirEntryIndex == -1)
            throw new FileNotFoundException();

        // find occupied resources
        DirectoryEntry directoryEntry = readDirectoryEntry(dirEntryIndex);
        int iNodeIndex = directoryEntry.iNodeIndex;
        INode iNode = readINodeFromStorage(iNodeIndex);
        int[] blockIndexes = iNode.blockIndexes;

        // release resources

        // TODO: close file (?)

        // remove directory entry
        removeDirectoryEntry(dirEntryIndex);

        // release iNode
        iNode = new INode(iNodeIndex);
        iNode.writeToStorage();

        // release storage blocks
        for (int blockIndex : blockIndexes) {
            markBlockAsFree(blockIndex);
        }
    }

    /**
     * lists the names of all file and their length
     * @return list of strings like file_name|file_size
     */
    public List<String> directory() {
        List<String> result = new LinkedList<>();
        DirectoryEntry emptyEntry = new DirectoryEntry(null,-1);
        DirectoryEntry currentEntry;
        File directory = OFT[0];

        directory.lseek(0);
        for (int pos = 0;
             pos < directory.iNode.length;
             pos += FileSystemParams.BYTES_PER_DIRECTORY_ENTRY) {
            try {
                currentEntry = new DirectoryEntry(
                        directory.read(FileSystemParams.BYTES_PER_DIRECTORY_ENTRY));
                if (!currentEntry.equals(emptyEntry)) {
                    int size = readINodeFromStorage(currentEntry.iNodeIndex).length;
                    result.add(currentEntry.name + "\t" + size + "B");
                }
            } catch (ReadWriteException e) {
                logger.error(e);
            }
        }

        return result;
    }

}
