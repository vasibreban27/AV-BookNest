package com.avbooknest.shipment.model;

import com.avbooknest.order.model.SellerOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "shipments")
public class Shipment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seller_order_id", nullable = false, unique = true)
  private SellerOrder sellerOrder;

  @Column(name = "easybox_id", nullable = false, length = 100)
  private String easyboxId;

  @Column(name = "easybox_name", nullable = false, length = 255)
  private String easyboxName;

  @Column(name = "easybox_address", length = 255)
  private String easyboxAddress;

  @Column(name = "easybox_city", length = 100)
  private String easyboxCity;

  @Column(name = "easybox_county", length = 100)
  private String easyboxCounty;

  @Column(name = "easybox_postal_code", length = 20)
  private String easyboxPostalCode;

  @Column(name = "tracking_number", unique = true, length = 100)
  private String trackingNumber;

  @Column(name = "sameday_parcel_id", length = 100)
  private String samedayParcelId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ShipmentStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "package_size", length = 10)
  private PackageSize packageSize;

  @Column(name = "package_weight_grams")
  private Integer packageWeightGrams;

  @Column(name = "package_length_mm")
  private Integer packageLengthMm;

  @Column(name = "package_width_mm")
  private Integer packageWidthMm;

  @Column(name = "package_height_mm")
  private Integer packageHeightMm;

  @Column(name = "provider_status", length = 100)
  private String providerStatus;

  @Column(name = "status_updated_at")
  private Instant statusUpdatedAt;

  @Column(name = "label_url", length = 2048)
  private String labelUrl;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Shipment() {}

  private Shipment(Builder builder) {
    id = builder.id;
    sellerOrder = builder.sellerOrder;
    easyboxId = builder.easyboxId;
    easyboxName = builder.easyboxName;
    easyboxAddress = builder.easyboxAddress;
    easyboxCity = builder.easyboxCity;
    easyboxCounty = builder.easyboxCounty;
    easyboxPostalCode = builder.easyboxPostalCode;
    trackingNumber = builder.trackingNumber;
    samedayParcelId = builder.samedayParcelId;
    status = builder.status;
    packageSize = builder.packageSize;
    packageWeightGrams = builder.packageWeightGrams;
    packageLengthMm = builder.packageLengthMm;
    packageWidthMm = builder.packageWidthMm;
    packageHeightMm = builder.packageHeightMm;
    providerStatus = builder.providerStatus;
    statusUpdatedAt = builder.statusUpdatedAt;
    labelUrl = builder.labelUrl;
    createdAt = builder.createdAt;
    updatedAt = builder.updatedAt;
  }

  public Long getId() {
    return id;
  }

  public SellerOrder getSellerOrder() {
    return sellerOrder;
  }

  public String getEasyboxId() {
    return easyboxId;
  }

  public String getEasyboxName() {
    return easyboxName;
  }

  public String getEasyboxAddress() {
    return easyboxAddress;
  }

  public String getEasyboxCity() {
    return easyboxCity;
  }

  public String getEasyboxCounty() {
    return easyboxCounty;
  }

  public String getEasyboxPostalCode() {
    return easyboxPostalCode;
  }

  public String getTrackingNumber() {
    return trackingNumber;
  }

  public String getSamedayParcelId() {
    return samedayParcelId;
  }

  public ShipmentStatus getStatus() {
    return status;
  }

  public PackageSize getPackageSize() {
    return packageSize;
  }

  public Integer getPackageWeightGrams() {
    return packageWeightGrams;
  }

  public Integer getPackageLengthMm() {
    return packageLengthMm;
  }

  public Integer getPackageWidthMm() {
    return packageWidthMm;
  }

  public Integer getPackageHeightMm() {
    return packageHeightMm;
  }

  public String getProviderStatus() {
    return providerStatus;
  }

  public Instant getStatusUpdatedAt() {
    return statusUpdatedAt;
  }

  public String getLabelUrl() {
    return labelUrl;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void queueAwb(Instant now) {
    if (packageSize == null
        || packageWeightGrams == null
        || packageLengthMm == null
        || packageWidthMm == null
        || packageHeightMm == null) {
      throw new IllegalStateException("Shipment package measurements are missing");
    }
    status = ShipmentStatus.AWB_PENDING;
    statusUpdatedAt = now;
    updatedAt = now;
  }

  public void registerAwb(String awbNumber, String parcelId, String newLabelUrl, Instant now) {
    trackingNumber = awbNumber;
    samedayParcelId = parcelId;
    labelUrl = newLabelUrl;
    status = ShipmentStatus.AWB_CREATED;
    statusUpdatedAt = now;
    updatedAt = now;
  }

  public boolean updateProviderStatus(
      ShipmentStatus mappedStatus, String rawProviderStatus, Instant providerUpdatedAt) {
    if (statusUpdatedAt != null && providerUpdatedAt.isBefore(statusUpdatedAt)) {
      return false;
    }
    if (mappedStatus != null) {
      status = mappedStatus;
    }
    providerStatus = rawProviderStatus;
    statusUpdatedAt = providerUpdatedAt;
    updatedAt = Instant.now();
    return true;
  }

  public void cancel(Instant now) {
    status = ShipmentStatus.CANCELLED;
    statusUpdatedAt = now;
    updatedAt = now;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Long id;
    private SellerOrder sellerOrder;
    private String easyboxId;
    private String easyboxName;
    private String easyboxAddress;
    private String easyboxCity;
    private String easyboxCounty;
    private String easyboxPostalCode;
    private String trackingNumber;
    private String samedayParcelId;
    private ShipmentStatus status;
    private PackageSize packageSize;
    private Integer packageWeightGrams;
    private Integer packageLengthMm;
    private Integer packageWidthMm;
    private Integer packageHeightMm;
    private String providerStatus;
    private Instant statusUpdatedAt;
    private String labelUrl;
    private Instant createdAt;
    private Instant updatedAt;

    public Builder id(Long value) {
      id = value;
      return this;
    }

    public Builder sellerOrder(SellerOrder value) {
      sellerOrder = value;
      return this;
    }

    public Builder easyboxId(String value) {
      easyboxId = value;
      return this;
    }

    public Builder easyboxName(String value) {
      easyboxName = value;
      return this;
    }

    public Builder easyboxAddress(String value) {
      easyboxAddress = value;
      return this;
    }

    public Builder easyboxCity(String value) {
      easyboxCity = value;
      return this;
    }

    public Builder easyboxCounty(String value) {
      easyboxCounty = value;
      return this;
    }

    public Builder easyboxPostalCode(String value) {
      easyboxPostalCode = value;
      return this;
    }

    public Builder trackingNumber(String value) {
      trackingNumber = value;
      return this;
    }

    public Builder samedayParcelId(String value) {
      samedayParcelId = value;
      return this;
    }

    public Builder status(ShipmentStatus value) {
      status = value;
      return this;
    }

    public Builder packageSize(PackageSize value) {
      packageSize = value;
      return this;
    }

    public Builder packageWeightGrams(Integer value) {
      packageWeightGrams = value;
      return this;
    }

    public Builder packageLengthMm(Integer value) {
      packageLengthMm = value;
      return this;
    }

    public Builder packageWidthMm(Integer value) {
      packageWidthMm = value;
      return this;
    }

    public Builder packageHeightMm(Integer value) {
      packageHeightMm = value;
      return this;
    }

    public Builder providerStatus(String value) {
      providerStatus = value;
      return this;
    }

    public Builder statusUpdatedAt(Instant value) {
      statusUpdatedAt = value;
      return this;
    }

    public Builder labelUrl(String value) {
      labelUrl = value;
      return this;
    }

    public Builder createdAt(Instant value) {
      createdAt = value;
      return this;
    }

    public Builder updatedAt(Instant value) {
      updatedAt = value;
      return this;
    }

    public Shipment build() {
      return new Shipment(this);
    }
  }
}
