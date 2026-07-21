package com.avbooknest.book.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
@ConfigurationProperties(prefix = "app.cloudinary")
public class CloudinaryProperties {
  private String cloudName;
  private String apiKey;
  private String apiSecret;
  private String folder = "booknest/book-covers";
  private DataSize maxFileSize = DataSize.ofMegabytes(5);

  public boolean isConfigured() {
    return hasText(cloudName) && hasText(apiKey) && hasText(apiSecret);
  }

  public String getCloudName() {
    return cloudName;
  }

  public void setCloudName(String cloudName) {
    this.cloudName = cloudName;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getApiSecret() {
    return apiSecret;
  }

  public void setApiSecret(String apiSecret) {
    this.apiSecret = apiSecret;
  }

  public String getFolder() {
    return folder;
  }

  public void setFolder(String folder) {
    this.folder = folder;
  }

  public DataSize getMaxFileSize() {
    return maxFileSize;
  }

  public void setMaxFileSize(DataSize maxFileSize) {
    this.maxFileSize = maxFileSize;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
