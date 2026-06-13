package by.chaika19.orderservice.service.impl;

import by.chaika19.orderservice.BaseIntegrationTest;
import by.chaika19.orderservice.client.UserServiceClient;
import by.chaika19.orderservice.dto.OrderInputDto;
import by.chaika19.orderservice.dto.OrderItemInputDto;
import by.chaika19.orderservice.dto.OrderOutputDto;
import by.chaika19.orderservice.dto.UserDto;
import by.chaika19.orderservice.exception.ResourceNotFoundException;
import by.chaika19.orderservice.model.Item;
import by.chaika19.orderservice.model.Order;
import by.chaika19.orderservice.model.OrderStatus;
import by.chaika19.orderservice.repository.ItemRepository;
import by.chaika19.orderservice.repository.OrderRepository;
import by.chaika19.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class OrderServiceImplIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private UserServiceClient userServiceClient;

    private Item savedItem1;
    private Item savedItem2;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE order_items, orders, items RESTART IDENTITY CASCADE;");

        Item item1 = Item.builder()
                .name("Test Item 1")
                .price(BigDecimal.valueOf(100.00))
                .build();

        Item item2 = Item.builder()
                .name("Test Item 2")
                .price(BigDecimal.valueOf(50.00))
                .build();

        savedItem1 = itemRepository.save(item1);
        savedItem2 = itemRepository.save(item2);

        UserDto mockUser = new UserDto(userId, "John Doe", "john.doe@example.com");
        when(userServiceClient.getUserById(userId)).thenReturn(mockUser);
    }

    @Test
    void createOrder_ShouldSaveOrderAndCalculateTotalPrice() {
        OrderInputDto inputDto = new OrderInputDto(
                userId,
                List.of(
                        new OrderItemInputDto(savedItem1.getId(), 2),
                        new OrderItemInputDto(savedItem2.getId(), 3)
                )
        );

        OrderOutputDto result = orderService.createOrder(inputDto);

        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
        assertThat(result.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(350.00));
        assertThat(result.user()).isNotNull();
        assertThat(result.user().id()).isEqualTo(userId);
        assertThat(result.user().name()).isEqualTo("John Doe");
        assertThat(result.orderItems()).hasSize(2);
        assertThat(orderRepository.existsById(result.id())).isTrue();
    }

    @Test
    void createOrder_ShouldThrowException_WhenItemNotFound() {
        Long nonExistingItemId = 999L;
        OrderInputDto inputDto = new OrderInputDto(
                userId,
                List.of(new OrderItemInputDto(nonExistingItemId, 1))
        );

        assertThatThrownBy(() -> orderService.createOrder(inputDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Item not found");
    }

    @Test
    void getOrderById_ShouldReturnOrder_WhenExists() {
        OrderInputDto inputDto = new OrderInputDto(userId, List.of(new OrderItemInputDto(savedItem1.getId(), 1)));
        OrderOutputDto savedOrder = orderService.createOrder(inputDto);

        OrderOutputDto foundOrder = orderService.getOrderById(savedOrder.id());

        assertThat(foundOrder).isNotNull();
        assertThat(foundOrder.id()).isEqualTo(savedOrder.id());
        assertThat(foundOrder.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    void getOrderById_ShouldThrowException_WhenOrderDoesNotExist() {
        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 999");
    }

    @Test
    void getOrdersFiltered_ShouldReturnPagedAndFilteredOrders() {
        OrderInputDto inputDto = new OrderInputDto(userId, List.of(new OrderItemInputDto(savedItem1.getId(), 1)));
        orderService.createOrder(inputDto);

        Page<OrderOutputDto> filteredOrders = orderService.getOrdersFiltered(
                List.of(OrderStatus.CREATED),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                PageRequest.of(0, 10)
        );

        assertThat(filteredOrders).isNotEmpty();
        assertThat(filteredOrders.getContent().get(0).status()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void getOrdersByUserId_ShouldReturnUserOrders() {
        OrderInputDto inputDto = new OrderInputDto(userId, List.of(new OrderItemInputDto(savedItem1.getId(), 1)));
        orderService.createOrder(inputDto);

        List<OrderOutputDto> orders = orderService.getOrdersByUserId(userId);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).user().id()).isEqualTo(userId);
    }

    @Test
    @Transactional
    void updateOrder_ShouldModifyExistingOrderAndRecalculatePrice() {
        OrderInputDto initialInput = new OrderInputDto(userId, List.of(new OrderItemInputDto(savedItem1.getId(), 1)));
        OrderOutputDto initialOrder = orderService.createOrder(initialInput);

        OrderInputDto updateInput = new OrderInputDto(userId, List.of(new OrderItemInputDto(savedItem2.getId(), 2)));

        OrderOutputDto updatedOrder = orderService.updateOrder(initialOrder.id(), updateInput);

        assertThat(updatedOrder.id()).isEqualTo(initialOrder.id());
        assertThat(updatedOrder.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(100.00));

        Order entityInDb = orderRepository.findById(initialOrder.id()).orElseThrow();
        assertThat(entityInDb.getOrderItems()).hasSize(1);
        assertThat(entityInDb.getOrderItems())
                .extracting(orderItem -> orderItem.getItem().getId())
                .containsExactly(savedItem2.getId());
    }

    @Test
    void deleteOrderById_ShouldRemoveOrderFromDatabase() {
        OrderInputDto inputDto = new OrderInputDto(userId, List.of(new OrderItemInputDto(savedItem1.getId(), 1)));
        OrderOutputDto created = orderService.createOrder(inputDto);

        orderService.deleteOrderById(created.id());

        assertThat(orderRepository.existsById(created.id())).isFalse();
    }

    @Test
    void deleteOrderById_ShouldThrowException_WhenOrderNotFound() {
        assertThatThrownBy(() -> orderService.deleteOrderById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 999");
    }

    @Test
    void enrichUser_Fallback_ShouldTriggerWhenUserServiceThrowsException() {
        when(userServiceClient.getUserById(anyLong()))
                .thenThrow(new RuntimeException("Remote service failure"));

        UserDto fallbackUser = orderService.enrichUser(userId);

        assertThat(fallbackUser).isNotNull();
        assertThat(fallbackUser.id()).isEqualTo(userId);
        assertThat(fallbackUser.name()).contains("Fallback User Service Down");
        assertThat(fallbackUser.email()).isEqualTo("temporary@unavailable.com");
    }
}