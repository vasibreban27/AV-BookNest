package com.avbooknest.book.service;

import com.avbooknest.auth.model.Role;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.dto.BookRequest;
import com.avbooknest.book.dto.BookResponse;
import com.avbooknest.book.model.Book;
import com.avbooknest.book.model.BookCondition;
import com.avbooknest.book.model.BookStatus;
import com.avbooknest.book.model.Category;
import com.avbooknest.book.repository.BookRepository;
import com.avbooknest.book.repository.CategoryRepository;
import com.avbooknest.common.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    @Mock private BookRepository bookRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, categoryRepository, userRepository);
    }

    @Test
    void createUsesAuthenticatedUserCategoryAndAvailableDefault() {
        User seller = user(10L, "seller@example.com");
        Category category = category(4L);
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(category));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse response = bookService.create(request(null), "seller@example.com");

        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(bookCaptor.capture());
        Book saved = bookCaptor.getValue();
        assertEquals("Clean Code", saved.getTitle());
        assertEquals(seller, saved.getSeller());
        assertEquals(category, saved.getCategory());
        assertEquals(BookStatus.AVAILABLE, saved.getStatus());
        assertEquals(new BigDecimal("45.50"), response.price());
    }

    @Test
    void updateRejectsAUserWhoDoesNotOwnTheBook() {
        User owner = user(2L, "owner@example.com");
        User anotherUser = user(3L, "other@example.com");
        Book book = book(11L, owner);
        when(bookRepository.findById(11L)).thenReturn(Optional.of(book));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(anotherUser));

        assertThrows(ForbiddenException.class, () -> bookService.update(11L, request(BookStatus.SOLD), "other@example.com"));
    }

    private BookRequest request(BookStatus status) {
        return new BookRequest(" Clean Code ", "Robert C. Martin", null, "A programming book", new BigDecimal("45.50"),
                BookCondition.VERY_GOOD, "English", "Prentice Hall", (short) 2008, null, 4L, status);
    }
    private Category category(Long id) {
        return Category.builder().id(id).name("Programming").slug("programming").createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }
    private Book book(Long id, User seller) {
        Instant now = Instant.now();
        return Book.builder().id(id).title("Clean Code").author("Robert C. Martin").price(new BigDecimal("45.50"))
                .bookCondition(BookCondition.GOOD).language("English").seller(seller).category(category(4L))
                .status(BookStatus.AVAILABLE).createdAt(now).updatedAt(now).build();
    }
    private User user(Long id, String email) {
        Instant now = Instant.now();
        return User.builder().id(id).firstName("Test").lastName("User").email(email).passwordHash("password")
                .role(Role.builder().id(1L).name("USER").build()).enabled(true).emailVerified(false).createdAt(now).updatedAt(now).build();
    }
}
