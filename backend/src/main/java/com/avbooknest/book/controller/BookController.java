package com.avbooknest.book.controller;

import com.avbooknest.book.dto.BookRequest;
import com.avbooknest.book.dto.BookResponse;
import com.avbooknest.book.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService bookService;
    public BookController(BookService bookService) { this.bookService = bookService; }
    @GetMapping public List<BookResponse> list() { return bookService.list(); }
    @GetMapping("/mine") public List<BookResponse> mine(Authentication authentication) { return bookService.listMine(authentication.getName()); }
    @GetMapping("/{bookId}") public BookResponse get(@PathVariable Long bookId) { return bookService.get(bookId); }
    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.create(request, authentication.getName()));
    }
    @PutMapping("/{bookId}")
    public BookResponse update(@PathVariable Long bookId, @Valid @RequestBody BookRequest request, Authentication authentication) {
        return bookService.update(bookId, request, authentication.getName());
    }
    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> delete(@PathVariable Long bookId, Authentication authentication) {
        bookService.delete(bookId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
