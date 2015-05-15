package com.tsushko.spos.fs;

/**
 * Describes the exception to be thrown when trying to load
 * a file system of another version from Storage backup
 *
 * @see FileSystemParams#getInstance(Storage, int);
 */
public class VersionMismatchException extends Exception{

    public VersionMismatchException() {}

    public VersionMismatchException(String msg) {
        super(msg);
    }

    public VersionMismatchException(String msg, Exception cause) {
        super(msg,cause);
    }

    public VersionMismatchException(Exception cause) {
        super(cause);
    }


}
