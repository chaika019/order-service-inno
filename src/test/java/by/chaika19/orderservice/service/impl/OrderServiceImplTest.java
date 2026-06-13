package by.chaika19.orderservice.service.impl;

import by.chaika19.orderservice.client.UserServiceClient;
import by.chaika19.orderservice.dto.*;
import by.chaika19.orderservice.exception.ResourceNotFoundException;
import by.chaika19.orderservice.mapper.OrderMapper;
import by.chaika19.orderservice.model.*;
import by.chaika19.orderservice.repository.ItemRepository;
import by.chaika19.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderRepository orderRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private UserServiceClient userServiceClient;

    @InjectMocks private OrderServiceImpl orderService;

    private final Long orderId = 1L;
    private final Long userId = 100L;
    private final Long itemId = 5L;
    private UserDto mockUser;
    private Item mockItem;

    @BeforeEach
    void setUp() {
        orderService.setSelf(orderService);

        mockUser = new UserDto(userId, "Ivan", "ivan@gmail.com");
        mockItem = Item.builder()
                .id(itemId)
                .price(BigDecimal.valueOf(150))
                .name("Test Item")
                .build();
    }

    @Test
    @DisplayName("Should successfully update order when order and items exist")
    void updateOrder_Success() {
        OrderInputDto inputDto = new OrderInputDto(userId, List.of(new OrderItemInputDto(itemId, 2)));

        Order existingOrder = Order.builder()
                .id(orderId)
                .userId(userId)
                .orderItems(new LinkedHashSet<>())
                .build();

        OrderOutputDto intermediateDto = new OrderOutputDto(orderId, null, OrderStatus.CREATED, BigDecimal.valueOf(300), false, List.of(), null, null);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(mockItem));
        when(userServiceClient.getUserById(userId)).thenReturn(mockUser);
        when(orderRepository.save(any(Order.class))).thenReturn(existingOrder);
        when(orderMapper.toDto(any(Order.class))).thenReturn(intermediateDto);

        OrderOutputDto result = orderService.updateOrder(orderId, inputDto);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(300), existingOrder.getTotalPrice());
        assertEquals(mockUser, result.user());
        verify(orderRepository).save(existingOrder);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existing order")
    void updateOrder_OrderNotFound_ThrowsException() {
        OrderInputDto inputDto = new OrderInputDto(userId, List.of());
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.updateOrder(orderId, inputDto));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully create order and calculate correct total price")
    void createOrder_Success() {
        OrderInputDto inputDto = new OrderInputDto(userId, List.of(new OrderItemInputDto(itemId, 3)));
        OrderOutputDto intermediateDto = new OrderOutputDto(orderId, null, OrderStatus.CREATED, BigDecimal.valueOf(450), false, List.of(), null, null);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(mockItem));
        when(userServiceClient.getUserById(userId)).thenReturn(mockUser);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toDto(any(Order.class))).thenReturn(intermediateDto);

        OrderOutputDto result = orderService.createOrder(inputDto);

        assertNotNull(result);
        assertEquals(mockUser, result.user());
        verify(orderRepository).save(argThat(order ->
                order.getTotalPrice().compareTo(BigDecimal.valueOf(450)) == 0 &&
                        order.getOrderItems().size() == 1
        ));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException during creation if item doesn't exist")
    void createOrder_ItemNotFound_ThrowsException() {
        OrderInputDto inputDto = new OrderInputDto(userId, List.of(new OrderItemInputDto(999L, 1)));
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(inputDto));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return order output DTO when order exists by ID")
    void getOrderById_Success() {
        Order order = Order.builder().id(orderId).userId(userId).build();
        OrderOutputDto intermediateDto = new OrderOutputDto(orderId, null, OrderStatus.CREATED, BigDecimal.ZERO, false, List.of(), null, null);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userServiceClient.getUserById(userId)).thenReturn(mockUser);
        when(orderMapper.toDto(order)).thenReturn(intermediateDto);

        OrderOutputDto result = orderService.getOrderById(orderId);

        assertNotNull(result);
        assertEquals(orderId, result.id());
        assertEquals(mockUser, result.user());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when fetching non-existing order by ID")
    void getOrderById_NotFound_ThrowsException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(orderId));
    }

    @Test
    @DisplayName("Should return list of orders for a specific user ID")
    void getOrdersByUserId_Success() {
        Order order = Order.builder().id(orderId).userId(userId).build();
        OrderOutputDto intermediateDto = new OrderOutputDto(orderId, null, OrderStatus.CREATED, BigDecimal.ZERO, false, List.of(), null, null);

        when(orderRepository.findByUserId(userId)).thenReturn(List.of(order));
        when(userServiceClient.getUserById(userId)).thenReturn(mockUser);
        when(orderMapper.toDto(order)).thenReturn(intermediateDto);

        List<OrderOutputDto> result = orderService.getOrdersByUserId(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(mockUser, result.get(0).user());
        verify(userServiceClient, times(1)).getUserById(userId);
    }

    @Test
    @DisplayName("Should return paged orders based on specification filters")
    @SuppressWarnings("unchecked")
    void getOrdersFiltered_Success() {
        Order order = Order.builder().id(orderId).userId(userId).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        OrderOutputDto intermediateDto = new OrderOutputDto(orderId, null, OrderStatus.CREATED, BigDecimal.ZERO, false, List.of(), null, null);
        Pageable pageable = PageRequest.of(0, 10);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(userServiceClient.getUserById(userId)).thenReturn(mockUser);
        when(orderMapper.toDto(order)).thenReturn(intermediateDto);

        Page<OrderOutputDto> result = orderService.getOrdersFiltered(
                List.of(OrderStatus.CREATED), LocalDateTime.now().minusDays(1), LocalDateTime.now(), pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(mockUser, result.getContent().get(0).user());
    }

    @Test
    @DisplayName("Should successfully delete order when it exists")
    void deleteOrderById_Success() {
        when(orderRepository.existsById(orderId)).thenReturn(true);

        assertDoesNotThrow(() -> orderService.deleteOrderById(orderId));
        verify(orderRepository).deleteById(orderId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existing order")
    void deleteOrderById_NotFound_ThrowsException() {
        when(orderRepository.existsById(orderId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> orderService.deleteOrderById(orderId));
        verify(orderRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should return enriched user details from UserServiceClient directly")
    void enrichUser_DirectCall_Success() {
        when(userServiceClient.getUserById(userId)).thenReturn(mockUser);

        UserDto result = orderService.enrichUser(userId);

        assertNotNull(result);
        assertEquals("Ivan", result.name());
        verify(userServiceClient).getUserById(userId);
    }

    @Test
    @DisplayName("Should return fallback user when exception occurs in Circuit Breaker fallback path")
    void fallbackGetUser_ReturnsTemporaryUserDto() {
        RuntimeException exception = new RuntimeException("Connection timeout");

        UserDto fallbackUser = orderService.fallbackGetUser(userId, exception);

        assertNotNull(fallbackUser);
        assertEquals(userId, fallbackUser.id());
        assertTrue(fallbackUser.name().contains("Fallback User Service Down"));
        assertEquals("temporary@unavailable.com", fallbackUser.email());
    }
}