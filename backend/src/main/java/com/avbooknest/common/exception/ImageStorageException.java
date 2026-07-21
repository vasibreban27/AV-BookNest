package com.avbooknest.common.exception;

public class ImageStorageException extends RuntimeException {
  public ImageStorageException(String message) {
    super(message);
  }

  public ImageStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
