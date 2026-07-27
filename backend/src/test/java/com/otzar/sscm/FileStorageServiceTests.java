package com.otzar.sscm;

import com.otzar.sscm.service.CloudinaryStorageClient;
import com.otzar.sscm.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileStorageServiceTests {
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
