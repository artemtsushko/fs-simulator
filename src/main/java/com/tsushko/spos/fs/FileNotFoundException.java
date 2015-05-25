package com.tsushko.spos.fs;

/**
 * Describes the exception to be thrown
 * when the requested file doesn't exist
 */
public class FileNotFoundException extends Exception{

    public FileNotFoundException() {}

    public FileNotFoundException(String msg) {
        super(msg);
    }

    public FileNotFoundException(String msg, Exception cause) {
        super(msg,cause);
    }

    public FileNotFoundException(Exception cause) {
        super(cause);
    }

}