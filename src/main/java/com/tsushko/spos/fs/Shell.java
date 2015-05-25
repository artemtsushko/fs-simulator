package com.tsushko.spos.fs;

import java.io.InputStream;
import java.io.PrintStream;
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
                case "exit":
                    break;
                default:
                    out.println("Command not recognized!");
                    break;
            }
        } while (!command.equals("exit"));
    }
}
