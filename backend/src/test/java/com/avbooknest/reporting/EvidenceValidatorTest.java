package com.avbooknest.reporting;

import static org.junit.jupiter.api.Assertions.*;

import com.avbooknest.common.exception.BadRequestException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class EvidenceValidatorTest {
  final EvidenceValidator validator = new EvidenceValidator();

  @Test
  void noEvidenceIsOptional() {
    assertTrue(validator.validate(null).isEmpty());
  }

  @Test
  void rejectsForgedContentTypeAndSvg() {
    assertThrows(
        BadRequestException.class,
        () ->
            validator.validate(
                List.of(
                    new MockMultipartFile(
                        "evidence",
                        "fake.png",
                        "image/png",
                        "<svg onload='alert(1)'/>".getBytes()))));
  }

  @Test
  void rejectsTooManyImages() {
    var file = new MockMultipartFile("evidence", new byte[] {1});
    assertThrows(
        BadRequestException.class, () -> validator.validate(List.of(file, file, file, file)));
  }

  @Test
  void rejectsOversizedAndEmptyFiles() {
    assertThrows(
        BadRequestException.class,
        () ->
            validator.validate(
                List.of(
                    new MockMultipartFile("evidence", new byte[EvidenceValidator.MAX_BYTES + 1]))));
    assertThrows(
        BadRequestException.class,
        () -> validator.validate(List.of(new MockMultipartFile("evidence", new byte[0]))));
  }

  @Test
  void stripsAppendedPayloadAndUsesDetectedType() throws Exception {
    var out = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
    byte[] original = out.toByteArray();
    out.write("private GPS and appended payload".getBytes());
    var clean =
        validator
            .validate(
                List.of(
                    new MockMultipartFile(
                        "evidence", "wrong.jpg", "image/jpeg", out.toByteArray())))
            .getFirst();
    assertEquals("image/png", clean.contentType());
    assertArrayEquals(original, clean.content());
  }

  @Test
  void rejectsLargeDimensionsBeforeDecoding() throws Exception {
    var out = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(4097, 1, BufferedImage.TYPE_INT_RGB), "png", out);
    assertThrows(
        BadRequestException.class,
        () -> validator.validate(List.of(new MockMultipartFile("evidence", out.toByteArray()))));
  }
}
