package com.tsushko.spos.fs;

/**
 * Describes the exception to be thrown
 * when exceeded the maximum number
 * of open files
 */
public class OpenFilesNumberException extends Exception{

    public OpenFilesNumberException() {}

    public OpenFilesNumberException(String msg) {
        super(msg);
    }

    public OpenFilesNumberException(String msg, Exception cause) {
        super(msg,cause);
    }

    public OpenFilesNumberException(Exception cause) {
        super(cause);
    }

}