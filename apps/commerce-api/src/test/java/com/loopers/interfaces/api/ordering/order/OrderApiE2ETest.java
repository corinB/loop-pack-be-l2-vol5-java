package com.loopers.interfaces.api.ordering.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.application.common.PageResult;
import com.loopers.application.ordering.order.AdminOrderView;
import com.loopers.application.ordering.order.OrderView;
import com.loopers.domain.mall.brand.Brand;
import com.loopers.domain.mall.brand.BrandRepository;
import com.loopers.domain.mall.product.Product;
import com.loopers.domain.mall.product.ProductRepository;
import com.loopers.domain.ordering.order.OrderStatus;
import com.loopers.domain.shopping.user.User;
import com.loopers.domain.shopping.user.UserRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderApiE2ETest {
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private JdbcClient jdbcClient;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("주문 생성")
    @Nested
    class Create {
        @DisplayName("같은 상품이 중복되면 수량을 합산해 DRAFT로 생성한다")
        @Test
        void createsDraftOrder_withMergedQuantities() {
            userRepository.save(User.create(1L));
            long productId = createProduct("상품", 1_000L, 10);

            ResponseEntity<ApiResponse<OrderView>> response = createOrder(1L, List.of(
                new OrderApiDto.ItemRequest(productId, 2),
                new OrderApiDto.ItemRequest(productId, 3)
            ));

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody().data().status()).isEqualTo(OrderStatus.DRAFT),
                () -> assertThat(response.getBody().data().totalAmount()).isEqualTo(5_000L),
                () -> assertThat(response.getBody().data().items()).hasSize(1),
                () -> assertThat(response.getBody().data().items().get(0).quantity()).isEqualTo(5),
                () -> assertThat(response.getBody().data().paymentAmount()).isNull(),
                () -> assertThat(currentStock(productId)).isEqualTo(10)
            );
        }

        @DisplayName("삭제된 상품이 포함되면 404를 반환하고 주문을 생성하지 않는다")
        @Test
        void returnsNotFound_whenProductIsDeleted() {
            userRepository.save(User.create(1L));
            long productId = createProduct("상품", 1_000L, 10);
            Product product = productRepository.findById(productId).orElseThrow();
            product.delete();
            productRepository.save(product);

            ResponseEntity<ApiResponse<Object>> response = createOrderRaw(1L,
                List.of(new OrderApiDto.ItemRequest(productId, 1)));

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                () -> assertThat(orderCount()).isZero()
            );
        }

        @DisplayName("품목이 비어 있으면 400을 반환한다")
        @Test
        void returnsBadRequest_whenItemsIsEmpty() {
            userRepository.save(User.create(1L));

            ResponseEntity<ApiResponse<Object>> response = createOrderRaw(1L, List.of());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("존재하지 않는 사용자면 404를 반환한다")
        @Test
        void returnsNotFound_whenUserDoesNotExist() {
            long productId = createProduct("상품", 1_000L, 10);

            ResponseEntity<ApiResponse<Object>> response = createOrderRaw(999L,
                List.of(new OrderApiDto.ItemRequest(productId, 1)));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("내 주문 목록·상세")
    @Nested
    class CustomerQuery {
        @DisplayName("헤더로 지정한 사용자의 주문만 조회한다")
        @Test
        void returnsOnlyOwnOrders() {
            userRepository.save(User.create(1L));
            userRepository.save(User.create(2L));
            long productId = createProduct("상품", 1_000L, 10);
            createOrder(1L, List.of(new OrderApiDto.ItemRequest(productId, 1)));
            createOrder(2L, List.of(new OrderApiDto.ItemRequest(productId, 1)));

            ResponseEntity<ApiResponse<PageResult<OrderView>>> response = findOrders(1L);

            assertThat(response.getBody().data().items()).hasSize(1);
        }

        @DisplayName("orderId로 상세를 조회한다")
        @Test
        void returnsOrderDetail() {
            userRepository.save(User.create(1L));
            long productId = createProduct("상품", 1_000L, 10);
            long orderId = createOrder(1L, List.of(new OrderApiDto.ItemRequest(productId, 2)))
                .getBody().data().orderId();

            ResponseEntity<ApiResponse<OrderView>> response = findOrder(orderId);

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().totalAmount()).isEqualTo(2_000L)
            );
        }

        @DisplayName("없는 주문은 404를 반환한다")
        @Test
        void returnsNotFound_whenOrderDoesNotExist() {
            ResponseEntity<ApiResponse<Object>> response = restTemplate.exchange(
                "/api/v1/orders/999",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("관리자 주문 목록·상세")
    @Nested
    class AdminQuery {
        @DisplayName("모든 구매자의 주문을 구매자 ID와 함께 조회한다")
        @Test
        void returnsAllOrders_withUserId() {
            userRepository.save(User.create(1L));
            long productId = createProduct("상품", 1_000L, 10);
            createOrder(1L, List.of(new OrderApiDto.ItemRequest(productId, 1)));

            ResponseEntity<ApiResponse<PageResult<AdminOrderView>>> response = restTemplate.exchange(
                "/api-admin/v1/orders",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {}
            );

            assertAll(
                () -> assertThat(response.getBody().data().items()).hasSize(1),
                () -> assertThat(response.getBody().data().items().get(0).userId()).isEqualTo(1L)
            );
        }

        @DisplayName("없는 주문은 404를 반환한다")
        @Test
        void returnsNotFound_whenOrderDoesNotExist() {
            ResponseEntity<ApiResponse<Object>> response = restTemplate.exchange(
                "/api-admin/v1/orders/999",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    private long createProduct(String name, long price, int stock) {
        Brand brand = brandRepository.save(Brand.create("브랜드", null));
        Product product = productRepository.save(Product.create(brand.getId(), name, null, price, stock));
        return product.getId();
    }

    private int currentStock(long productId) {
        return productRepository.findById(productId).orElseThrow().getStock();
    }

    private long orderCount() {
        return jdbcClient.sql("SELECT COUNT(*) FROM orders").query(Long.class).single();
    }

    private ResponseEntity<ApiResponse<OrderView>> createOrder(long userId, List<OrderApiDto.ItemRequest> items) {
        return restTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.POST,
            new HttpEntity<>(new OrderApiDto.CreateRequest(items), headers(String.valueOf(userId))),
            new ParameterizedTypeReference<>() {}
        );
    }

    private ResponseEntity<ApiResponse<Object>> createOrderRaw(long userId, List<OrderApiDto.ItemRequest> items) {
        return restTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.POST,
            new HttpEntity<>(new OrderApiDto.CreateRequest(items), headers(String.valueOf(userId))),
            new ParameterizedTypeReference<>() {}
        );
    }

    private ResponseEntity<ApiResponse<PageResult<OrderView>>> findOrders(long userId) {
        return restTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.GET,
            new HttpEntity<>(null, headers(String.valueOf(userId))),
            new ParameterizedTypeReference<>() {}
        );
    }

    private ResponseEntity<ApiResponse<OrderView>> findOrder(long orderId) {
        return restTemplate.exchange(
            "/api/v1/orders/" + orderId,
            HttpMethod.GET,
            HttpEntity.EMPTY,
            new ParameterizedTypeReference<>() {}
        );
    }

    private HttpHeaders headers(String userIdHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", userIdHeader);
        return headers;
    }
}
