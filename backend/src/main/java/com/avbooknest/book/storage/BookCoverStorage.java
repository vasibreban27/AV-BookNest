package com.avbooknest.book.storage;

import org.springframework.web.multipart.MultipartFile;

public interface BookCoverStorage {
  StoredBookCover upload(MultipartFile file);

  void delete(String publicId);
}
