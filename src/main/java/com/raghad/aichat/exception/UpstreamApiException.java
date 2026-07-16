package com.raghad.aichat.exception;

import org.springframework.http.HttpStatusCode;

public class UpstreamApiException extends RuntimeException {

    private final HttpStatusCode upstreamStatus;
    private final boolean timeout;

    public UpstreamApiException(String message, HttpStatusCode upstreamStatus) {
        super(message);
        this.upstreamStatus = upstreamStatus;
        this.timeout = false;
    }

    public UpstreamApiException(String message, Throwable cause) {
        this(message, cause, false);
    }

    public UpstreamApiException(String message, Throwable cause, boolean timeout) {
        super(message, cause);
        this.upstreamStatus = null;
        this.timeout = timeout;
    }

    public HttpStatusCode getUpstreamStatus() {
        return upstreamStatus;
    }

    public boolean isTimeout() {
        return timeout;
    }
}
