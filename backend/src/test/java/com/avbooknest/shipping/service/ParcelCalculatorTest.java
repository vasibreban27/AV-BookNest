package com.avbooknest.shipping.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.avbooknest.book.model.Book;
import com.avbooknest.shipment.model.PackageSize;
import com.avbooknest.shipping.model.ParcelMetrics;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParcelCalculatorTest {
  private final ParcelCalculator calculator = new ParcelCalculator();

  @Test
  void addsPackagingAndChoosesSmallestFittingEasyboxCompartment() {
    Book first = Book.builder().weightGrams(500).lengthMm(210).widthMm(140).heightMm(30).build();
    Book second = Book.builder().weightGrams(700).lengthMm(240).widthMm(170).heightMm(45).build();

    ParcelMetrics parcel = calculator.calculate(List.of(first, second));

    assertEquals(1350, parcel.weightGrams());
    assertEquals(260, parcel.lengthMm());
    assertEquals(190, parcel.widthMm());
    assertEquals(95, parcel.heightMm());
    assertEquals(PackageSize.S, parcel.packageSize());
  }
}
