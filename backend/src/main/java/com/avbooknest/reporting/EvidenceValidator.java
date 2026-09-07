package com.avbooknest.reporting;

import com.avbooknest.common.exception.BadRequestException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class EvidenceValidator {
  public static final int MAX_BYTES = 2 * 1024 * 1024;

  public List<ReportDtos.Evidence> validate(List<MultipartFile> files) {
    if (files == null) return List.of();
    if (files.size() > 3) throw new BadRequestException("Maximum 3 evidence images");
    return files.stream().map(this::image).toList();
  }

  private ReportDtos.Evidence image(MultipartFile file) {
    if (file.isEmpty() || file.getSize() > MAX_BYTES)
      throw new BadRequestException("Each image must be between 1 byte and 2 MB");
    try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext())
        throw new BadRequestException("Only valid PNG and JPEG images are accepted");
      var reader = readers.next();
      try {
        reader.setInput(input, true, true);
        String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
        if (!List.of("png", "jpeg", "jpg").contains(format))
          throw new BadRequestException("Only PNG and JPEG images are accepted");
        int width = reader.getWidth(0), height = reader.getHeight(0);
        if (width < 1
            || height < 1
            || width > 4096
            || height > 4096
            || (long) width * height > 12_000_000)
          throw new BadRequestException(
              "Image dimensions are too large (maximum 4096 px and 12 megapixels)");
        BufferedImage decoded = reader.read(0);
        // Re-encode pixels only: discard EXIF/GPS metadata and any appended payload.
        var output = new ByteArrayOutputStream();
        String outputFormat = format.equals("png") ? "png" : "jpeg";
        if (!ImageIO.write(decoded, outputFormat, output) || output.size() > MAX_BYTES)
          throw new BadRequestException("Processed image exceeds 2 MB");
        return new ReportDtos.Evidence("image/" + outputFormat, output.toByteArray());
      } finally {
        reader.dispose();
      }
    } catch (IOException | IllegalArgumentException e) {
      throw new BadRequestException("Invalid evidence image");
    }
  }
}
