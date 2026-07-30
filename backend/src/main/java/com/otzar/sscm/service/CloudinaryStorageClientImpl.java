package com.otzar.sscm.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryStorageClientImpl implements CloudinaryStorageClient {
    private final Cloudinary cloudinary;
    private final boolean configured;

    public CloudinaryStorageClientImpl(
            @Value("${CLOUDINARY_CLOUD_NAME:}") String cloudName,
            @Value("${CLOUDINARY_API_KEY:}") String apiKey,
            @Value("${CLOUDINARY_API_SECRET:}") String apiSecret) {
        this.configured = isPresent(cloudName) && isPresent(apiKey) && isPresent(apiSecret);
        this.cloudinary = configured
                ? new Cloudinary(ObjectUtils.asMap(
                        "cloud_name", cloudName,
                        "api_key", apiKey,
                        "api_secret", apiSecret,
                        "secure", true))
                : null;
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    @Override
    public String uploadImage(byte[] bytes) throws IOException {
        return upload(bytes, "image");
    }

    @Override
    public String uploadVideo(byte[] bytes) throws IOException {
        return upload(bytes, "video");
    }

    private String upload(byte[] bytes, String resourceType) throws IOException {
        if (!configured) {
            throw new IllegalStateException("Cloudinary media storage is not configured");
        }
        Map<?, ?> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                "resource_type", resourceType,
                "folder", "sscm"));
        Object secureUrl = result.get("secure_url");
        if (secureUrl == null || !secureUrl.toString().startsWith("https://")) {
            throw new IOException("Cloudinary did not return a secure image URL");
        }
        return secureUrl.toString();
    }

    private boolean isPresent(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
