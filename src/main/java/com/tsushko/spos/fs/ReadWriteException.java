package com.tsushko.spos.fs;

/**
 * Describes the exception to be thrown when some exceptional
 * situation occurs during reading from/writing to a file
 */
public class ReadWriteException extends Exception{

    public ReadWriteException() {}

    public ReadWriteException(String msg) {
        super(msg);
    }

    public ReadWriteException(String msg, Exception cause) {
        super(msg,cause);
    }

    public ReadWriteException(Exception cause) {
        super(cause);
    }

}