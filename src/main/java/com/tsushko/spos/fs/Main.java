package com.tsushko.spos.fs;

import java.io.*;

/**
 * Launcher for the shell
 *
 * @author Artem Tsushko
 * @version 1.0
 */
public class Main {

    /**
     * launches the shell with given input and output files if specified,
     * otherwise input is taken from <code>System.in</code>
     * and output is streamed to <code>System.out</code>
     * @param args args[0] - name of input file
     *             args[1] - name of output file
     */
    public static void main(String[] args) {
        InputStream in;
        PrintStream out;

        if (args.length > 0) {
            try {
                in = new FileInputStream(args[0]);
            } catch (java.io.FileNotFoundException e) {
                System.err.println("unable to open file " + args[0]);
                return;
            }
        } else {
            in = System.in;
        }
        if (args.length > 1) {
            try {
                out = new PrintStream(args[1]);
            } catch (java.io.FileNotFoundException e) {
                System.err.println("unable to open file " + args[1]);
                return;
            }
        } else {
            out = System.out;
        }

        new Shell(in,out).run();
    }
}
