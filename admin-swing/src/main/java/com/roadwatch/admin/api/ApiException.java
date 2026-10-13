package com.roadwatch.admin.api;

/** An error from the server (status 400-599) or from the network (status 0). */
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() { return status; }
}
