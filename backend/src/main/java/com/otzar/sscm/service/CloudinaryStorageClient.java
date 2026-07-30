package com.otzar.sscm.service;

import java.io.IOException;

public interface CloudinaryStorageClient {
    boolean isConfigured();
    String uploadImage(byte[] bytes) throws IOException;
    String uploadVideo(byte[] bytes) throws IOException;
}
