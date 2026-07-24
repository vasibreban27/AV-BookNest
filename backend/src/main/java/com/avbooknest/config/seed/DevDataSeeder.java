package com.avbooknest.config.seed;

import static com.avbooknest.config.seed.DevSeedData.BOOKS;
import static com.avbooknest.config.seed.DevSeedData.BUYER_EMAIL;
import static com.avbooknest.config.seed.DevSeedData.CATEGORIES;
import static com.avbooknest.config.seed.DevSeedData.USERS;

import com.avbooknest.config.seed.DevSeedData.BookSeed;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(DevDataSeeder.class);
  private static final String CURRENCY = "RON";

  private final JdbcTemplate jdbcTemplate;
  private final PasswordEncoder passwordEncoder;
  private final String demoPassword;

  public DevDataSeeder(
      JdbcTemplate jdbcTemplate,
      PasswordEncoder passwordEncoder,
      @Value("${app.seed.password}") String demoPassword) {
    this.jdbcTemplate = jdbcTemplate;
    this.passwordEncoder = passwordEncoder;
    this.demoPassword = demoPassword;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (demoPassword.length() < 8) {
      throw new IllegalStateException("app.seed.password must contain at least 8 characters");
    }
    Map<String, Long> userIds = seedUsers();
    Map<String, Long> categoryIds = seedCategories();
    Map<String, Long> bookIds = seedBooks(userIds, categoryIds);

    seedBuyerCart(userIds.get(BUYER_EMAIL), bookIds);
    seedBuyerWishlist(userIds.get(BUYER_EMAIL), bookIds);
    seedOrderHistory(userIds, bookIds);
    seedNotifications(userIds);

    LOGGER.info(
        "Development data ready: {} users, {} categories and {} books",
        userIds.size(),
        categoryIds.size(),
        bookIds.size());
  }

  private Map<String, Long> seedUsers() {
    Map<String, Long> roleIds =
        Map.of(
            "USER",
            requiredId("SELECT id FROM roles WHERE name = ?", "USER"),
            "ADMIN",
            requiredId("SELECT id FROM roles WHERE name = ?", "ADMIN"));
    String passwordHash = passwordEncoder.encode(demoPassword);
    Map<String, Long> userIds = new HashMap<>();

    for (var user : USERS) {
      jdbcTemplate.update(
          """
          INSERT INTO users (
              first_name, last_name, email, password_hash, role_id,
              email_verified, enabled, created_at, updated_at
          )
          VALUES (?, ?, ?, ?, ?, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          ON CONFLICT (email) DO UPDATE SET
              first_name = EXCLUDED.first_name,
              last_name = EXCLUDED.last_name,
              password_hash = EXCLUDED.password_hash,
              role_id = EXCLUDED.role_id,
              email_verified = TRUE,
              enabled = TRUE
          """,
          user.firstName(),
          user.lastName(),
          user.email(),
          passwordHash,
          roleIds.get(user.role()));
      userIds.put(user.email(), requiredId("SELECT id FROM users WHERE email = ?", user.email()));
    }
    return userIds;
  }

  private Map<String, Long> seedCategories() {
    Map<String, Long> categoryIds = new HashMap<>();
    for (var category : CATEGORIES) {
      Long categoryId = findId("SELECT id FROM categories WHERE slug = ?", category.slug());
      if (categoryId == null) {
        categoryId =
            findId("SELECT id FROM categories WHERE LOWER(name) = LOWER(?)", category.name());
      }

      if (categoryId == null) {
        categoryId =
            jdbcTemplate.queryForObject(
                """
                INSERT INTO categories (name, slug, description, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """,
                Long.class,
                category.name(),
                category.slug(),
                category.description());
      } else {
        jdbcTemplate.update(
            "UPDATE categories SET name = ?, slug = ?, description = ? WHERE id = ?",
            category.name(),
            category.slug(),
            category.description(),
            categoryId);
      }
      categoryIds.put(category.slug(), categoryId);
    }
    return categoryIds;
  }

  private Map<String, Long> seedBooks(Map<String, Long> userIds, Map<String, Long> categoryIds) {
    Map<String, Long> bookIds = new HashMap<>();
    for (BookSeed book : BOOKS) {
      Long existingId =
          findId("SELECT id FROM books WHERE isbn = ? ORDER BY id LIMIT 1", book.isbn());
      Long sellerId = userIds.get(book.sellerEmail());
      Long categoryId = categoryIds.get(book.categorySlug());

      if (existingId == null) {
        existingId =
            jdbcTemplate.queryForObject(
                """
                INSERT INTO books (
                    title, author, isbn, description, price, book_condition, language,
                    publisher, published_year, cover_image_url, seller_id, category_id,
                    status, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """,
                Long.class,
                book.title(),
                book.author(),
                book.isbn(),
                book.description(),
                book.price(),
                book.condition(),
                book.language(),
                book.publisher(),
                book.publishedYear(),
                book.coverImageUrl(),
                sellerId,
                categoryId,
                book.status());
      } else {
        jdbcTemplate.update(
            """
            UPDATE books SET
                title = ?, author = ?, description = ?, price = ?, book_condition = ?,
                language = ?, publisher = ?, published_year = ?, cover_image_url = ?,
                cover_image_public_id = NULL, seller_id = ?, category_id = ?, status = ?
            WHERE id = ?
            """,
            book.title(),
            book.author(),
            book.description(),
            book.price(),
            book.condition(),
            book.language(),
            book.publisher(),
            book.publishedYear(),
            book.coverImageUrl(),
            sellerId,
            categoryId,
            book.status(),
            existingId);
      }
      bookIds.put(book.isbn(), existingId);
    }
    return bookIds;
  }

  private void seedBuyerCart(Long buyerId, Map<String, Long> bookIds) {
    jdbcTemplate.update(
        """
        INSERT INTO carts (user_id, created_at, updated_at)
        VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (user_id) DO NOTHING
        """,
        buyerId);
    Long cartId = requiredId("SELECT id FROM carts WHERE user_id = ?", buyerId);

    addCartItem(cartId, bookIds.get("9780525559474"));
    addCartItem(cartId, bookIds.get("9789734670482"));
  }

  private void addCartItem(Long cartId, Long bookId) {
    jdbcTemplate.update(
        """
        INSERT INTO cart_items (cart_id, book_id, added_at)
        VALUES (?, ?, CURRENT_TIMESTAMP)
        ON CONFLICT (cart_id, book_id) DO NOTHING
        """,
        cartId,
        bookId);
  }

  private void seedBuyerWishlist(Long buyerId, Map<String, Long> bookIds) {
    addWishlistItem(buyerId, bookIds.get("9781847941831"));
    addWishlistItem(buyerId, bookIds.get("9780261103573"));
    addWishlistItem(buyerId, bookIds.get("9780141321073"));
  }

  private void addWishlistItem(Long userId, Long bookId) {
    jdbcTemplate.update(
        """
        INSERT INTO favorites (user_id, book_id, created_at)
        VALUES (?, ?, CURRENT_TIMESTAMP)
        ON CONFLICT (user_id, book_id) DO NOTHING
        """,
        userId,
        bookId);
  }

  private void seedOrderHistory(Map<String, Long> userIds, Map<String, Long> bookIds) {
    Long buyerId = userIds.get(BUYER_EMAIL);

    Long deliveredOrder =
        seedOrder(
            "BN-DEMO-0001",
            buyerId,
            "DELIVERED",
            new BigDecimal("73.40"),
            Instant.parse("2026-06-18T11:30:00Z"));
    seedOrderItem(
        deliveredOrder,
        bookIds.get("9780141439518"),
        userIds.get("mihai.ionescu@booknest.local"),
        "9780141439518");
    seedOrderItem(
        deliveredOrder,
        bookIds.get("9780061120084"),
        userIds.get("elena.marinescu@booknest.local"),
        "9780061120084");
    seedPayment(deliveredOrder, new BigDecimal("73.40"), "SUCCEEDED", "2026-06-20T09:45:00Z");
    seedShipment(
        deliveredOrder,
        userIds.get("mihai.ionescu@booknest.local"),
        "DEMO-EASY-01",
        "Easybox Piața Victoriei, București",
        "BN-DEMO-AWB-1001",
        "DELIVERED");
    seedShipment(
        deliveredOrder,
        userIds.get("elena.marinescu@booknest.local"),
        "DEMO-EASY-01",
        "Easybox Piața Victoriei, București",
        "BN-DEMO-AWB-1002",
        "DELIVERED");

    Long processingOrder =
        seedOrder(
            "BN-DEMO-0002",
            buyerId,
            "PROCESSING",
            new BigDecimal("46.00"),
            Instant.parse("2026-07-12T08:15:00Z"));
    seedOrderItem(
        processingOrder,
        bookIds.get("9780553380163"),
        userIds.get("radu.georgescu@booknest.local"),
        "9780553380163");
    seedPayment(processingOrder, new BigDecimal("46.00"), "PENDING", null);
    seedShipment(
        processingOrder,
        userIds.get("radu.georgescu@booknest.local"),
        "DEMO-EASY-02",
        "Easybox Universitate, București",
        "BN-DEMO-AWB-2001",
        "AWB_CREATED");
  }

  private Long seedOrder(
      String orderNumber, Long buyerId, String status, BigDecimal total, Instant placedAt) {
    Long existingId = findId("SELECT id FROM orders WHERE order_number = ?", orderNumber);
    if (existingId != null) return existingId;

    return jdbcTemplate.queryForObject(
        """
        INSERT INTO orders (
            order_number, buyer_id, shipping_address_id, status, subtotal,
            shipping_cost, total_amount, currency, recipient_name, recipient_email,
            recipient_phone, placed_at, updated_at
        )
        SELECT ?, ?, NULL, ?, ?, 0, ?, ?, CONCAT_WS(' ', first_name, last_name),
               email, '+40700111222', ?, CURRENT_TIMESTAMP
        FROM users WHERE id = ?
        RETURNING id
        """,
        Long.class,
        orderNumber,
        buyerId,
        status,
        total,
        total,
        CURRENCY,
        databaseTimestamp(placedAt),
        buyerId);
  }

  private Long seedOrderItem(Long orderId, Long bookId, Long sellerId, String isbn) {
    Long existingId =
        findId(
            "SELECT id FROM order_items WHERE order_id = ? AND book_id = ? ORDER BY id LIMIT 1",
            orderId,
            bookId);
    if (existingId != null) return existingId;

    BookSeed book = bookByIsbn(isbn);
    BigDecimal commission =
        book.price().multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
    Long sellerOrderId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO seller_orders (
                order_id, seller_id, status, item_subtotal, commission_rate,
                commission_amount, seller_proceeds, shipping_cost, accept_by,
                created_at, updated_at
            )
            VALUES (?, ?, 'AWAITING_SELLER', ?, 5.00, ?, ?, 0,
                    CURRENT_TIMESTAMP + INTERVAL '24 hours',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (order_id, seller_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP
            RETURNING id
            """,
            Long.class,
            orderId,
            sellerId,
            book.price(),
            commission,
            book.price().subtract(commission));
    jdbcTemplate.update(
        """
        INSERT INTO seller_transfers (
            seller_order_id, amount, currency, status, created_at, updated_at
        )
        VALUES (?, ?, 'RON', 'BLOCKED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (seller_order_id) DO UPDATE SET
            amount = EXCLUDED.amount,
            updated_at = CURRENT_TIMESTAMP
        """,
        sellerOrderId,
        book.price().subtract(commission));
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO order_items (
            order_id, book_id, seller_id, seller_order_id, title, author, isbn,
            unit_price, quantity, created_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP)
        RETURNING id
        """,
        Long.class,
        orderId,
        bookId,
        sellerId,
        sellerOrderId,
        book.title(),
        book.author(),
        book.isbn(),
        book.price());
  }

  private void seedPayment(
      Long orderId, BigDecimal amount, String status, String paidAtIsoTimestamp) {
    if (findId("SELECT id FROM payments WHERE order_id = ? ORDER BY id LIMIT 1", orderId) != null) {
      return;
    }
    OffsetDateTime paidAt =
        paidAtIsoTimestamp == null ? null : databaseTimestamp(Instant.parse(paidAtIsoTimestamp));
    jdbcTemplate.update(
        """
        INSERT INTO payments (
            order_id, provider, amount, currency, status, paid_at, created_at, updated_at
        )
        VALUES (?, 'STRIPE', ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        orderId,
        amount,
        CURRENCY,
        status,
        paidAt);
  }

  private void seedShipment(
      Long orderId,
      Long sellerId,
      String easyboxId,
      String easyboxName,
      String trackingNumber,
      String status) {
    Long sellerOrderId =
        requiredId(
            "SELECT id FROM seller_orders WHERE order_id = ? AND seller_id = ?", orderId, sellerId);
    jdbcTemplate.update(
        """
        INSERT INTO shipments (
            seller_order_id, easybox_id, easybox_name, easybox_address,
            easybox_city, easybox_county, easybox_postal_code, tracking_number,
            status, package_size, package_weight_grams, package_length_mm,
            package_width_mm, package_height_mm, provider_status, status_updated_at,
            created_at, updated_at
        )
        VALUES (?, ?, ?, 'Adresă demo', 'București', 'București', '010001', ?,
                ?, 'S', 650, 230, 160, 50, ?, CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (seller_order_id) DO UPDATE SET
            easybox_id = EXCLUDED.easybox_id,
            easybox_name = EXCLUDED.easybox_name,
            tracking_number = EXCLUDED.tracking_number,
            status = EXCLUDED.status,
            package_size = EXCLUDED.package_size,
            package_weight_grams = EXCLUDED.package_weight_grams,
            package_length_mm = EXCLUDED.package_length_mm,
            package_width_mm = EXCLUDED.package_width_mm,
            package_height_mm = EXCLUDED.package_height_mm,
            provider_status = EXCLUDED.provider_status
        """,
        sellerOrderId,
        easyboxId,
        easyboxName,
        trackingNumber,
        status,
        "DEMO_" + status);
    jdbcTemplate.update(
        """
        UPDATE seller_orders
        SET status = ?,
            accepted_at = CASE WHEN ? = 'ACCEPTED' THEN CURRENT_TIMESTAMP ELSE accepted_at END,
            dropoff_by = CASE WHEN ? = 'ACCEPTED' THEN CURRENT_TIMESTAMP + INTERVAL '48 hours' ELSE dropoff_by END,
            fulfilled_at = CASE WHEN ? = 'FULFILLED' THEN CURRENT_TIMESTAMP ELSE fulfilled_at END
        WHERE id = ?
        """,
        "DELIVERED".equals(status) ? "FULFILLED" : "ACCEPTED",
        "DELIVERED".equals(status) ? "FULFILLED" : "ACCEPTED",
        "DELIVERED".equals(status) ? "FULFILLED" : "ACCEPTED",
        "DELIVERED".equals(status) ? "FULFILLED" : "ACCEPTED",
        sellerOrderId);
    if ("DELIVERED".equals(status)) {
      jdbcTemplate.update(
          """
          UPDATE seller_transfers
          SET eligible_at = CURRENT_TIMESTAMP + INTERVAL '24 hours',
              updated_at = CURRENT_TIMESTAMP
          WHERE seller_order_id = ?
          """,
          sellerOrderId);
    }
  }

  private void seedNotifications(Map<String, Long> userIds) {
    seedNotification(
        userIds.get(BUYER_EMAIL),
        "ORDER_PLACED",
        "Comandă pregătită pentru test",
        "Comanda demo BN-DEMO-0002 este în curs de pregătire.");
    seedNotification(
        userIds.get("radu.georgescu@booknest.local"),
        "BOOK_RESERVED",
        "Carte rezervată",
        "Cartea «O scurtă istorie a timpului» apare într-o comandă demo.");
  }

  private void seedNotification(Long userId, String type, String title, String message) {
    jdbcTemplate.update(
        """
        INSERT INTO notifications (user_id, type, title, message, created_at)
        SELECT ?, ?, ?, ?, CURRENT_TIMESTAMP
        WHERE NOT EXISTS (
            SELECT 1 FROM notifications WHERE user_id = ? AND title = ? AND message = ?
        )
        """,
        userId,
        type,
        title,
        message,
        userId,
        title,
        message);
  }

  private BookSeed bookByIsbn(String isbn) {
    return BOOKS.stream()
        .filter(book -> book.isbn().equals(isbn))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Missing development book " + isbn));
  }

  private OffsetDateTime databaseTimestamp(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private Long requiredId(String sql, Object... arguments) {
    Long id = findId(sql, arguments);
    if (id == null) throw new IllegalStateException("Development seed dependency is missing");
    return id;
  }

  private Long findId(String sql, Object... arguments) {
    List<Long> ids =
        jdbcTemplate.query(sql, (resultSet, rowNumber) -> resultSet.getLong(1), arguments);
    return ids.isEmpty() ? null : ids.getFirst();
  }
}
