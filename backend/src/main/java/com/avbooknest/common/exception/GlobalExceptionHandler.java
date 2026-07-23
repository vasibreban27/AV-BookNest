package com.avbooknest.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
    }
    return response(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiError> handleConflict(
      ConflictException exception, HttpServletRequest request) {
    return response(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiError> handleBadRequest(
      BadRequestException exception, HttpServletRequest request) {
    return response(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiError> handleUploadTooLarge(
      MaxUploadSizeExceededException exception, HttpServletRequest request) {
    return response(
        HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded cover image is too large", request, Map.of());
  }

  @ExceptionHandler(ImageStorageException.class)
  public ResponseEntity<ApiError> handleImageStorage(
      ImageStorageException exception, HttpServletRequest request) {
    return response(HttpStatus.BAD_GATEWAY, exception.getMessage(), request, Map.of());
  }

  @ExceptionHandler(ExternalServiceException.class)
  public ResponseEntity<ApiError> handleExternalService(
      ExternalServiceException exception, HttpServletRequest request) {
    return response(HttpStatus.BAD_GATEWAY, exception.getMessage(), request, Map.of());
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiError> handleUnauthorized(
      UnauthorizedException exception, HttpServletRequest request) {
    return response(HttpStatus.UNAUTHORIZED, exception.getMessage(), request, Map.of());
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(
      NotFoundException exception, HttpServletRequest request) {
    return response(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ApiError> handleForbidden(
      ForbiddenException exception, HttpServletRequest request) {
    return response(HttpStatus.FORBIDDEN, exception.getMessage(), request, Map.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(
      Exception exception, HttpServletRequest request) {
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, Map.of());
  }

  private ResponseEntity<ApiError> response(
      HttpStatus status,
      String message,
      HttpServletRequest request,
      Map<String, String> fieldErrors) {
    ApiError body =
        new ApiError(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            fieldErrors);
    return ResponseEntity.status(status).body(body);
  }
}
