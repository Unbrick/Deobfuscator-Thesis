package org.thesis.dexprocessor.exceptions;

public class RegisterNotInitializedException extends RuntimeException{

    public RegisterNotInitializedException(int registerNumber) {
        super("RegisterNotInitializedException at register " + registerNumber);
    }
}
