package com.tsushko.spos.fs;

import java.io.*;
import java.util.Properties;
import java.util.Scanner;

/**
 * A command line shell that users will use
 * to communicate with file system
 *
 * @author Artem Tsushko
 * @version 1.0
 */
public class Shell {

    private Scanner in;
    private PrintStream out;
    private FileSystem fileSystem;


    public Shell(InputStream in, PrintStream out) {
        this.in = new Scanner(in);
        this.out = out;
    }

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
            out.println("error: " + e.getLocalizedMessage());
        }
        out.println("disk saved");
    }
}
