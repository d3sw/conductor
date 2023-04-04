package com.netflix.conductor.core.exceptions;

public class ServerShutdownException extends RuntimeException {

    public ServerShutdownException(String message) {
        super(message);
    }
}
