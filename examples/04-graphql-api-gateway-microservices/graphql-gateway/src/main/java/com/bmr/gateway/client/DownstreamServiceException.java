package com.bmr.gateway.client;

public class DownstreamServiceException extends RuntimeException {
    private final int statusCode;
    private final String downstreamUrl;

    public DownstreamServiceException(String message, int statusCode, String downstreamUrl) {
        super(message);
        this.statusCode = statusCode;
        this.downstreamUrl = downstreamUrl;
    }

    public int statusCode() {
        return statusCode;
    }

    public String downstreamUrl() {
        return downstreamUrl;
    }
}
