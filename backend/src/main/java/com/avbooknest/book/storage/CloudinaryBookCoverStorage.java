package com.avbooknest.book.storage;

import com.avbooknest.book.config.CloudinaryProperties;
import com.avbooknest.common.exception.BadRequestException;
import com.avbooknest.common.exception.ImageStorageException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class CloudinaryBookCoverStorage implements BookCoverStorage {
  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  private final Cloudinary cloudinary;
  private final CloudinaryProperties properties;

  public CloudinaryBookCoverStorage(Cloudinary cloudinary, CloudinaryProperties properties) {
    this.cloudinary = cloudinary;
    this.properties = properties;
  }

  @Override
  public StoredBookCover upload(MultipartFile file) {
    byte[] content = readContent(file);
    validate(file, content);
    requireConfiguration();
    try {
      Map<?, ?> result =
          cloudinary
              .uploader()
              .upload(
                  content,
                  ObjectUtils.asMap(
                      "public_id",
                      UUID.randomUUID().toString(),
                      "folder",
                      properties.getFolder(),
                      "resource_type",
                      "image",
                      "overwrite",
                      false));
      Object publicId = result.get("public_id");
      Object secureUrl = result.get("secure_url");
      if (publicId == null || secureUrl == null) {
        throw new ImageStorageException("Cloudinary returned an incomplete upload response");
      }
      return new StoredBookCover(publicId.toString(), secureUrl.toString());
    } catch (IOException | RuntimeException exception) {
      if (exception instanceof ImageStorageException imageStorageException) {
        throw imageStorageException;
      }
      throw new ImageStorageException("The cover image could not be uploaded", exception);
    }
  }

  @Override
  public void delete(String publicId) {
    if (publicId == null || publicId.isBlank()) return;
    requireConfiguration();
    try {
      cloudinary
          .uploader()
          .destroy(publicId, ObjectUtils.asMap("resource_type", "image", "invalidate", true));
    } catch (IOException | RuntimeException exception) {
      throw new ImageStorageException("The cover image could not be deleted", exception);
    }
  }

  private byte[] readContent(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("A cover image is required");
    }
    try {
      return file.getBytes();
    } catch (IOException exception) {
      throw new BadRequestException("The cover image could not be read");
    }
  }

  private void validate(MultipartFile file, byte[] content) {
    if (file.getSize() > properties.getMaxFileSize().toBytes()) {
      throw new BadRequestException(
          "The cover image must not exceed " + properties.getMaxFileSize().toMegabytes() + " MB");
    }
    if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
      throw new BadRequestException("Only JPEG, PNG and WebP cover images are accepted");
    }
    if (!hasExpectedSignature(file.getContentType(), content)) {
      throw new BadRequestException("The cover image content does not match its file type");
    }
  }

  private boolean hasExpectedSignature(String contentType, byte[] content) {
    return switch (contentType) {
      case "image/jpeg" ->
          content.length >= 3
              && unsigned(content[0]) == 0xFF
              && unsigned(content[1]) == 0xD8
              && unsigned(content[2]) == 0xFF;
      case "image/png" ->
          content.length >= 8
              && unsigned(content[0]) == 0x89
              && content[1] == 'P'
              && content[2] == 'N'
              && content[3] == 'G'
              && unsigned(content[4]) == 0x0D
              && unsigned(content[5]) == 0x0A
              && unsigned(content[6]) == 0x1A
              && unsigned(content[7]) == 0x0A;
      case "image/webp" ->
          content.length >= 12
              && content[0] == 'R'
              && content[1] == 'I'
              && content[2] == 'F'
              && content[3] == 'F'
              && content[8] == 'W'
              && content[9] == 'E'
              && content[10] == 'B'
              && content[11] == 'P';
      default -> false;
    };
  }

  private int unsigned(byte value) {
    return value & 0xFF;
  }

  private void requireConfiguration() {
    if (!properties.isConfigured()) {
      throw new ImageStorageException("Cloudinary is not configured");
    }
  }
}
