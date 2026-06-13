package by.chaika19.orderservice.service.impl;

import by.chaika19.orderservice.client.UserServiceClient;
import by.chaika19.orderservice.dto.OrderInputDto;
import by.chaika19.orderservice.dto.OrderItemInputDto;
import by.chaika19.orderservice.dto.OrderOutputDto;
import by.chaika19.orderservice.dto.UserDto;
import by.chaika19.orderservice.exception.ResourceNotFoundException;
import by.chaika19.orderservice.mapper.OrderMapper;
import by.chaika19.orderservice.model.Item;
import by.chaika19.orderservice.model.Order;
import by.chaika19.orderservice.model.OrderItem;
import by.chaika19.orderservice.model.OrderStatus;
import by.chaika19.orderservice.repository.OrderRepository;
import by.chaika19.orderservice.repository.ItemRepository;
import by.chaika19.orderservice.repository.specification.OrderSpecification;
import by.chaika19.orderservice.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final UserServiceClient userServiceClient;

    private OrderServiceImpl self;

    @Autowired
    public void setSelf(@Lazy OrderServiceImpl self) {
        this.self = self;
    }

    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "fallbackGetUser")
    public UserDto enrichUser(Long userID) {
        return userServiceClient.getUserById(userID);
    }

    public UserDto fallbackGetUser(Long userId, Throwable throwable) {
        log.error("User Service is unavailable! Fallback triggered for userId: {}. Error: {}", userId, throwable.getMessage());
        return new UserDto(userId, "Fallback User Service Down)", "temporary@unavailable.com");
    }

    private OrderOutputDto combineOrderAndUser(Order order, UserDto userDto) {
        OrderOutputDto orderOutputDto = orderMapper.toDto(order);
        return new OrderOutputDto(
                orderOutputDto.id(),
                userDto,
                orderOutputDto.status(),
                orderOutputDto.totalPrice(),
                orderOutputDto.deleted(),
                orderOutputDto.orderItems(),
                orderOutputDto.createdAt(),
                orderOutputDto.updatedAt()
        );
    }

    @Override
    @Transactional
    public OrderOutputDto createOrder(OrderInputDto inputDto) {
        Order order = Order.builder()
                .userId(inputDto.userId())
                .status(OrderStatus.CREATED)
                .orderItems(new LinkedHashSet<>())
                .deleted(false)
                .build();

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemInputDto itemInputDto : inputDto.items()) {
            Item item = itemRepository.findById(itemInputDto.itemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found"));

            OrderItem orderItem = OrderItem.builder()
                    .quantity(itemInputDto.quantity())
                    .build();

            orderItem.setOrder(order);
            orderItem.setItem(item);

            order.getOrderItems().add(orderItem);

            BigDecimal itemCost = item.getPrice().multiply(BigDecimal.valueOf(itemInputDto.quantity()));
            totalPrice = totalPrice.add(itemCost);
        }

        order.setTotalPrice(totalPrice);

        UserDto userDto = self.enrichUser(inputDto.userId());
        Order savedOrder = orderRepository.save(order);

        return combineOrderAndUser(savedOrder, userDto);
    }

    @Override
    public OrderOutputDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        UserDto userDto = self.enrichUser(order.getUserId());
        return combineOrderAndUser(order, userDto);
    }

    @Override
    public Page<OrderOutputDto> getOrdersFiltered(
            List<OrderStatus> statuses,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        Specification<Order> specification = Specification.where(OrderSpecification.hasStatus(statuses))
                .and(OrderSpecification.createBetween(startDate, endDate));

        Page<Order> orderPage = orderRepository.findAll(specification, pageable);

        return orderPage.map(order -> {
            UserDto userDto = self.enrichUser(order.getUserId());
            return combineOrderAndUser(order, userDto);
        });
    }

    @Override
    public List<OrderOutputDto> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        UserDto userDto = self.enrichUser(userId);

        return orders.stream()
                .map(order -> combineOrderAndUser(order, userDto))
                .toList();
    }

    @Override
    @Transactional
    public OrderOutputDto updateOrder(Long id, OrderInputDto inputDto) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        existingOrder.setUserId(inputDto.userId());

        if (existingOrder.getOrderItems() == null) {
            existingOrder.setOrderItems(new LinkedHashSet<>());
        } else {
            existingOrder.getOrderItems().clear();
        }

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemInputDto itemInputDto : inputDto.items()) {
            Item item = itemRepository.findById(itemInputDto.itemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + itemInputDto.itemId()));

            OrderItem orderItem = OrderItem.builder()
                    .quantity(itemInputDto.quantity())
                    .build();

            orderItem.setOrder(existingOrder);
            orderItem.setItem(item);
            existingOrder.getOrderItems().add(orderItem);

            BigDecimal itemCost = item.getPrice().multiply(BigDecimal.valueOf(itemInputDto.quantity()));
            totalPrice = totalPrice.add(itemCost);
        }

        existingOrder.setTotalPrice(totalPrice);

        UserDto userDto = self.enrichUser(inputDto.userId());
        Order updatedOrder = orderRepository.save(existingOrder);
        return combineOrderAndUser(updatedOrder, userDto);
    }

    @Override
    @Transactional
    public void deleteOrderById(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order not found with id: " + id);
        }
        orderRepository.deleteById(id);
    }
}