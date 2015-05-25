package com.tsushko.spos.fs;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

/**
 * A command line shell that users will use
 * to communicate with the emulated file system
 *
 * @author Artem Tsushko
 * @version 1.0
 */
public class Shell {

    /**
     * input stream
     */
    private Scanner in;

    /**
     * output stream
     */
    private PrintStream out;

    /**
     * the emulated file system
     */
    private FileSystem fileSystem;


    /**
     * takes input and output streams as constructor parameters
     * @param in the input stream
     * @param out the output stream
     */
    public Shell(InputStream in, PrintStream out) {
        this.in = new Scanner(in);
        this.out = out;
    }

    /**
     * starts the shell and dispatches the commands
     * until the exit command is met
     */
    public void run() {
        String command;
        out.println("File System Simulator v1.0");
        do {
            out.print("FS> ");
            command = in.next();
            switch (command) {
                case "in":
                    load();
                    break;
                case "sv":
                    save();
                    break;
                case "cr":
                    create();
                    break;
                case "de":
                    destroy();
                    break;
                case "op":
                    open();
                    break;
                case "cl":
                    close();
                    break;
                case "rd":
                    read();
                    break;
                case "wr":
                    write();
                    break;
                case "sk":
                    seek();
                    break;
                case "dr":
                    directory();
                    break;
                case "exit":
                    break;
                default:
                    out.println("Command not recognized!");
                    break;
            }
        } while (!command.equals("exit"));
    }

    /**
     * loads the FileSystem.
     * The first argument specifies where to take parameters:
     * <ul>
     *     <li>backup - 2nd argument specifies backup file name,
     *         3rd - maximum number of open files</li>
     *     <li>input - 2nd argument specifies block size,
     *         3rd - blocks number,
     *         4th - iNodes number,
     *         5th - maximum number of open files</li>
     *     <li>properties - 2nd argument specifies the name
     *         of the properties file, that holds all the parameters</li>
     * </ul>
     */
    private void load() {
        String command = in.next();
        switch (command) {
            case "backup":
                loadFromBackup();
                break;
            case "input":
                loadFromInput();
                break;
            case "properties":
                loadFromProperties();
                break;
            default:
                out.println("Command not recognized!");
                break;
        }
    }

    /**
     * loads the file system from storage backup file.
     * The first argument specifies backup file name,
     * the second - maximum number of open files
     */
    private void loadFromBackup() {
        String fileName = in.next();
        File file = new File(fileName);
        Storage storage;
        try {
            storage = InMemoryStorage.getStorageFromFile(file);
        } catch (Exception e) {
            out.println("error: " + e.getMessage());
            return;
        }

        int maxOpenFilesNumber = in.nextInt();
        FileSystemParams params;
        try {
            params = FileSystemParams.getInstance(storage, maxOpenFilesNumber);
        } catch (VersionMismatchException e) {
            out.println("error: " + e.getMessage());
            return;
        }

        fileSystem = new FileSystem(params,storage);
        out.println("disk restored");
    }

    /**
     * loads a new file system. File system parameters are taken from input.
     * 1st argument specifies block size,
     * 2nd - blocks number,
     * 3rd - iNodes number,
     * 4th - maximum number of open files.
     */
    private void loadFromInput() {
        int blockSize = in.nextInt();
        int blocksNumber = in.nextInt();
        int iNodesNumber = in.nextInt();
        int maxOpenFilesNumber = in.nextInt();
        FileSystemParams params;
        params = FileSystemParams.getInstance(
                blockSize,
                blocksNumber,
                iNodesNumber,
                maxOpenFilesNumber);
        fileSystem = new FileSystem(params);
        out.println("disk initialized");
    }

    /**
     * loads a new file system. File system parameters are taken
     * from properties file. The next argument is properties file name.
     * The properties file must contain at least such properties:
     * <ul>
     * <li><code>blockSize</code> - size of block in emulated IO device</li>
     * <li><code>blocksNumber</code> - number of blocks in emulated IO device</li>
     * <li><code>iNodesNumber</code> - number of iNodes</li>
     * <li><code>maxOpenFilesNumber</code> -  maximum number of files
     * that can be opened at the same time, excluding the directory</li>
     * </ul>
     */
    private void loadFromProperties() {
        String fileName = in.next();
        InputStream file;
        Properties properties = new Properties();
        try {
            file = new FileInputStream(fileName);
            properties.load(file);
        } catch (IOException e) {
            out.println("error: " + e.getMessage());
            return;
        }
        int blockSize = Integer.parseInt(
                properties.getProperty("blockSize"));
        int blocksNumber = Integer.parseInt(
                properties.getProperty("blocksNumber"));
        int iNodesNumber = Integer.parseInt(
                properties.getProperty("iNodesNumber"));
        int maxOpenFilesNumber = Integer.parseInt(
                properties.getProperty("maxOpenFilesNumber"));
        FileSystemParams params;
        params = FileSystemParams.getInstance(
                blockSize,
                blocksNumber,
                iNodesNumber,
                maxOpenFilesNumber);
        fileSystem = new FileSystem(params);
        out.println("disk initialized");
    }

    /**
     * saves a copy of the storage to the specified file
     */
    private void save() {
        String fileName = in.next();
        File file = new File(fileName);
        try {
            fileSystem.backupStorage(file);
        } catch (IOException e) {
            out.println("error: " + e.getMessage());
        }
        out.println("disk saved");
    }

    /**
     * creates a new file.
     * The next argument is the file's name.
     * The name's size in bytes should be at most
     * {@link FileSystemParams#BYTES_PER_FILE_NAME}
     */
    private void create() {
        String fileName = in.next();
        try {
            fileSystem.create(fileName);
            out.println("file " + fileName + " created");
        } catch (FileAlreadyExistsException e) {
            out.println("error: the file with name "
                        + fileName
                        + " already exists.");
        } catch (ReadWriteException e) {
            out.println("error: " + e.getMessage());
        }
    }

    /**
     * removes the file with specified name and frees resources
     * The next argument is the file's name.
     */
    private void destroy() {
        String fileName = in.next();
        try {
            fileSystem.destroy(fileName);
            out.println("file " + fileName + " destroyed");
        } catch (FileNotFoundException e) {
            out.println("error: the file with name "
                    + fileName
                    + " doesn't exist.");
        }

    }

    /**
     * opens the file with specified name and prints it's index,
     * which can be used for read, write, lseek, and close operations
     * The next argument is the file's name.
     */
    private void open() {
        String fileName = in.next();
        try {
            int index = fileSystem.open(fileName);
            out.println("file " + fileName + " opened, index=" + index);
        } catch (FileNotFoundException e) {
            out.println("error: the file with name "
                    + fileName
                    + " doesn't exist.");
        } catch (OpenFilesNumberException e) {
            out.println("error:  the maximum number of open files "
                    + "was exceeded");
        }
    }

    /**
     * closes the file with specified index.
     * The next argument is the file's index,
     * that was returned by {@link #open()}
     */
    private void close() {
        int index = in.nextInt();
        try {
            fileSystem.close(index);
            out.println("file with index " + index + " closed");
        } catch (FileNotFoundException e) {
            out.println("error: " + e.getMessage());
        }
    }

    /**
     * reads <code>count</code> bytes from file specified <code>index</code>.
     * The 1st argument is file's <code>index</code>,
     * the 2nd one is byte <code>count</code>.
     */
    private void read() {
        int index = in.nextInt();
        int count = in.nextInt();
        try {
            byte[] bytes = fileSystem.read(index,count);
            out.println(count + " bytes read: " + getBytesString(bytes));
        } catch (ReadWriteException | IllegalArgumentException e) {
            out.println("error: " + e.getMessage());
        }

    }

    /**
     * converts a bytes array to string: treats each byte in the array
     * as ASCII code of some symbol and returns a string of this symbols
     *
     * @param bytes an array of bytes, where each byte is treated
     *              as ASCII code of some symbol
     * @return  a string that is a concatenation of all symbols that
     *          correspond to ASCII codes in input array
     */
    private String getBytesString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes)
            result.append((char) b);
        return result.toString();
    }

    /**
     * writes <code>count</code> bytes to the file
     * with specified <code>index</code>, where each byte
     * is an ASCII code of the given <code>character</code>.
     * The 1st argument is file's <code>index</code>,
     * the 2nd one is the <code>character</code>,
     * the 3rd one is the bytes <code>count</code>.
     */
    private void write() {
        int index = in.nextInt();
        String character = in.next();
        int count = in.nextInt();
        byte[] bytes = getStringBytes(character,count);
        try {
            fileSystem.write(index,bytes);
            out.println(count + " bytes written");
        } catch (ReadWriteException | IllegalArgumentException e) {
            out.println("error: " + e.getMessage());
        }

    }

    /**
     * creates a byte array where each byte is an ASCII code
     * of the specified character
     *
     * @param character the character whose ASCII code will be used
     *                  to fill in the bytes array
     * @param count the size of the result byte array
     * @return  a byte array where each byte is an ASCII code
     *          of the specified character
     */
    private byte[] getStringBytes(String character, int count) {
        byte[] bytes = new byte[count];
        byte ch = character.getBytes()[0];
        Arrays.fill(bytes,ch);
        return bytes;
    }

    /**
     * moves the position of the file with specified <code>index</code>
     * to the desired position <code>pos</code>.
     * The 1st argument is file's <code>index</code>,
     * the 2nd one is <code>pos</code>.
     */
    private void seek() {
        int index = in.nextInt();
        int pos = in.nextInt();
        try {
            fileSystem.lseek(index, pos);
            out.println("current position is " + pos);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            out.println("error: " + e.getMessage());
        }
    }

    /**
     * list's the files in the directory and their sizes
     */
    private void directory() {
        List<String> directory = fileSystem.directory();
        for (String entry : directory) {
            out.println(entry);
        }
    }
}
