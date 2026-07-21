package com.avbooknest.book.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avbooknest.book.config.CloudinaryProperties;
import com.avbooknest.common.exception.BadRequestException;
import com.avbooknest.common.exception.ImageStorageException;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class CloudinaryBookCoverStorageTest {
  @Mock private Cloudinary cloudinary;
  @Mock private Uploader uploader;
  private CloudinaryProperties properties;
  private CloudinaryBookCoverStorage storage;

  @BeforeEach
  void setUp() {
    properties = new CloudinaryProperties();
    properties.setCloudName("booknest");
    properties.setApiKey("api-key");
    properties.setApiSecret("api-secret");
    storage = new CloudinaryBookCoverStorage(cloudinary, properties);
  }

  @Test
  void uploadReturnsCloudinaryPublicIdAndSecureUrl() throws IOException {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "cover.webp",
            "image/webp",
            new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'});
    when(cloudinary.uploader()).thenReturn(uploader);
    when(uploader.upload(any(byte[].class), anyMap()))
        .thenReturn(
            Map.of(
                "public_id",
                "booknest/book-covers/cover-id",
                "secure_url",
                "https://res.cloudinary.com/booknest/cover.webp"));

    StoredBookCover result = storage.upload(file);

    assertEquals("booknest/book-covers/cover-id", result.publicId());
    assertEquals("https://res.cloudinary.com/booknest/cover.webp", result.secureUrl());
  }

  @Test
  void uploadRejectsUnsupportedContentType() throws IOException {
    MockMultipartFile file =
        new MockMultipartFile("file", "cover.gif", "image/gif", new byte[] {1, 2, 3});

    assertThrows(BadRequestException.class, () -> storage.upload(file));
    verify(uploader, never()).upload(any(byte[].class), anyMap());
  }

  @Test
  void uploadRejectsContentThatDoesNotMatchDeclaredType() throws IOException {
    MockMultipartFile file =
        new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[] {1, 2, 3});

    assertThrows(BadRequestException.class, () -> storage.upload(file));
    verify(uploader, never()).upload(any(byte[].class), anyMap());
  }

  @Test
  void uploadRejectsMissingCloudinaryConfiguration() {
    CloudinaryProperties emptyProperties = new CloudinaryProperties();
    CloudinaryBookCoverStorage unconfiguredStorage =
        new CloudinaryBookCoverStorage(cloudinary, emptyProperties);
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "cover.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

    assertThrows(ImageStorageException.class, () -> unconfiguredStorage.upload(file));
  }
}
