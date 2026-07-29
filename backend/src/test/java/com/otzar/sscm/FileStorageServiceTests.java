package com.otzar.sscm;

import com.otzar.sscm.service.CloudinaryStorageClient;
import com.otzar.sscm.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileStorageServiceTests {
    @TempDir
    Path tempDirectory;

    @Test
    void configuredStoragePathIsCreatedAndKeepsLocalUploadUrl() throws Exception {
        CloudinaryStorageClient cloudinary = mock(CloudinaryStorageClient.class);
        when(cloudinary.isConfigured()).thenReturn(false);
        Path configuredStoragePath = tempDirectory.resolve("custom-uploads");
        FileStorageService service = new FileStorageService(cloudinary, false, configuredStoragePath.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file", "example.mp4", "video/mp4", new byte[]{1, 2, 3});

        String url = service.store(file);

        assertEquals(configuredStoragePath.toAbsolutePath().normalize(), service.getUploadDirectory());
        assertTrue(Files.isDirectory(configuredStoragePath));
        assertTrue(url.startsWith("/uploads/"));
        assertTrue(Files.isRegularFile(
                configuredStoragePath.resolve(url.substring("/uploads/".length()))));
    }

    @Test
    void railwayDefaultUsesWritableTemporaryDirectory() throws Exception {
        assumeTrue(System.getenv("RAILWAY_ENVIRONMENT") != null);
        FileStorageService service = new FileStorageService(mock(CloudinaryStorageClient.class));

        assertEquals(
                Path.of(System.getProperty("java.io.tmpdir"), "uploads").toAbsolutePath().normalize(),
                service.getUploadDirectory());
        assertTrue(Files.isDirectory(service.getUploadDirectory()));
    }

    @Test
    void configuredCloudinaryStoresImageAndReturnsSecureUrl() throws Exception {
        CloudinaryStorageClient cloudinary = mock(CloudinaryStorageClient.class);
        when(cloudinary.isConfigured()).thenReturn(true);
        when(cloudinary.uploadImage(any(byte[].class)))
                .thenReturn("https://res.cloudinary.com/demo/image/upload/sscm/example.jpg");
        FileStorageService service = new FileStorageService(cloudinary);
        MockMultipartFile file = new MockMultipartFile(
                "file", "example.jpg", "image/jpeg", new byte[]{1, 2, 3});

        String url = service.store(file);

        assertEquals("https://res.cloudinary.com/demo/image/upload/sscm/example.jpg", url);
        verify(cloudinary).uploadImage(any(byte[].class));
    }

    @Test
    void invalidFileIsRejectedBeforeCloudinaryCall() throws Exception {
        CloudinaryStorageClient cloudinary = mock(CloudinaryStorageClient.class);
        when(cloudinary.isConfigured()).thenReturn(true);
        FileStorageService service = new FileStorageService(cloudinary);
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.exe", "application/octet-stream", new byte[]{1});

        assertThrows(IllegalArgumentException.class, () -> service.store(file));
        verify(cloudinary, never()).uploadImage(any(byte[].class));
    }

    @Test
    void emptyFileIsRejectedWithoutExternalCall() throws Exception {
        CloudinaryStorageClient cloudinary = mock(CloudinaryStorageClient.class);
        when(cloudinary.isConfigured()).thenReturn(true);
        FileStorageService service = new FileStorageService(cloudinary);
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> service.store(file));
        verify(cloudinary, never()).uploadImage(any(byte[].class));
    }
}
