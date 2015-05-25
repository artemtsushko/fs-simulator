package com.tsushko.spos.fs;

/**
 * Describes the exception to be thrown when a file
 * with specified symbolic name already exists
 * in the directory
 */
public class FileAlreadyExistsException extends Exception{

    public FileAlreadyExistsException() {}

    public FileAlreadyExistsException(String msg) {
        super(msg);
    }

    public FileAlreadyExistsException(String msg, Exception cause) {
        super(msg,cause);
    }

    public FileAlreadyExistsException(Exception cause) {
        super(cause);
    }

}
