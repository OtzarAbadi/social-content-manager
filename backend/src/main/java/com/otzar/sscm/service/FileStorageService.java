package com.otzar.sscm.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp",
            "mp4", "webm", "mov", "avi", "mkv"
    );

    private final Path uploadDirectory;
    private final CloudinaryStorageClient cloudinaryStorageClient;
    private final boolean requireCloudinary;

    public FileStorageService(CloudinaryStorageClient cloudinaryStorageClient) throws IOException {
        this(cloudinaryStorageClient, false, "");
    }

    @Autowired
    public FileStorageService(
            CloudinaryStorageClient cloudinaryStorageClient,
            @Value("${sscm.storage.require-cloudinary:false}") boolean requireCloudinary,
            @Value("${sscm.storage.path:${SSCM_STORAGE_PATH:}}") String configuredStoragePath) throws IOException {
        this.cloudinaryStorageClient = cloudinaryStorageClient;
        this.requireCloudinary = requireCloudinary;
        this.uploadDirectory = resolveUploadDirectory(configuredStoragePath);
        Files.createDirectories(uploadDirectory);
    }

    private Path resolveUploadDirectory(String configuredStoragePath) {
        if (StringUtils.hasText(configuredStoragePath)) {
            return Paths.get(configuredStoragePath.trim()).toAbsolutePath().normalize();
        }

        Path workingDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (isContainerRuntime(workingDirectory)) {
            return Paths.get(System.getProperty("java.io.tmpdir"), "uploads").toAbsolutePath().normalize();
        }

        return workingDirectory.getFileName() != null
                && "backend".equalsIgnoreCase(workingDirectory.getFileName().toString())
                ? workingDirectory.resolve("uploads")
                : workingDirectory.resolve("backend").resolve("uploads");
    }

    private boolean isContainerRuntime(Path workingDirectory) {
        return System.getenv("RAILWAY_ENVIRONMENT") != null
                || System.getenv("RAILWAY_PROJECT_ID") != null
                || workingDirectory.startsWith(Paths.get("/app"))
                || (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux")
                    && Files.exists(Paths.get("/.dockerenv")));
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null) {
            return null;
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String contentType = file.getContentType();
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = getExtension(originalName);

        if (contentType == null
                || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))
                || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file type");
        }

        if (contentType.startsWith("image/") || contentType.startsWith("video/")) {
            if (!cloudinaryStorageClient.isConfigured()) {
                if (requireCloudinary) {
                    throw new IOException("Production media storage is not configured");
                }
            } else {
                try {
                    return contentType.startsWith("video/")
                            ? cloudinaryStorageClient.uploadVideo(file.getBytes())
                            : cloudinaryStorageClient.uploadImage(file.getBytes());
                } catch (IOException | RuntimeException exception) {
                    throw new IOException("Could not upload media to Cloudinary", exception);
                }
            }
        }

        String storedName = UUID.randomUUID() + "." + extension;
        Path destination = uploadDirectory.resolve(storedName).normalize();
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + storedName;
    }

    public Path getUploadDirectory() {
        return uploadDirectory;
    }

    public boolean isManagedUploadAvailable(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return true;
        }

        String storedName = fileUrl.substring("/uploads/".length());
        if (storedName.isEmpty()) {
            return false;
        }

        Path candidate = uploadDirectory.resolve(storedName).normalize();
        return candidate.startsWith(uploadDirectory) && Files.isRegularFile(candidate);
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
