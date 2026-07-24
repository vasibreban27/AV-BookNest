package com.avbooknest.shipping.service;

import com.avbooknest.book.model.Book;
import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.shipment.model.PackageSize;
import com.avbooknest.shipping.model.ParcelMetrics;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ParcelCalculator {
  static final int PACKAGING_WEIGHT_GRAMS = 150;
  static final int PACKAGING_ALLOWANCE_MM = 20;
  static final int MAX_WEIGHT_GRAMS = 20_000;

  public ParcelMetrics calculate(List<Book> books) {
    if (books.isEmpty()) {
      throw new ConflictException("Cannot calculate transport for an empty parcel");
    }
    int weight = PACKAGING_WEIGHT_GRAMS + books.stream().mapToInt(Book::getWeightGrams).sum();
    int length =
        PACKAGING_ALLOWANCE_MM + books.stream().mapToInt(Book::getLengthMm).max().orElseThrow();
    int width =
        PACKAGING_ALLOWANCE_MM + books.stream().mapToInt(Book::getWidthMm).max().orElseThrow();
    int height = PACKAGING_ALLOWANCE_MM + books.stream().mapToInt(Book::getHeightMm).sum();
    if (weight > MAX_WEIGHT_GRAMS) {
      throw new ConflictException("The parcel exceeds the 20 kg Easybox limit");
    }
    PackageSize size = fittingSize(length, width, height);
    return new ParcelMetrics(weight, length, width, height, size);
  }

  private PackageSize fittingSize(int length, int width, int height) {
    if (fits(length, width, height, 445, 100, 470)) return PackageSize.S;
    if (fits(length, width, height, 445, 200, 470)) return PackageSize.M;
    if (fits(length, width, height, 445, 390, 470)) return PackageSize.L;
    throw new ConflictException("The parcel dimensions exceed the largest Easybox compartment");
  }

  private boolean fits(
      int length, int width, int height, int boxLength, int boxWidth, int boxHeight) {
    int[] parcel = {length, width, height};
    int[] box = {boxLength, boxWidth, boxHeight};
    Arrays.sort(parcel);
    Arrays.sort(box);
    return parcel[0] <= box[0] && parcel[1] <= box[1] && parcel[2] <= box[2];
  }
}
