package com.taskbackend.taskbackend.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("Not authenticated");
    }
}
