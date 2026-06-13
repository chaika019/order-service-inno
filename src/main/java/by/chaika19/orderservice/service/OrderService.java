package by.chaika19.orderservice.service;

import by.chaika19.orderservice.dto.OrderInputDto;
import by.chaika19.orderservice.dto.OrderOutputDto;
import by.chaika19.orderservice.dto.UserDto;
import by.chaika19.orderservice.model.OrderStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {

    @Transactional
    OrderOutputDto createOrder(OrderInputDto inputDto);

    OrderOutputDto getOrderById(Long id);

    Page<OrderOutputDto> getOrdersFiltered(
            List<OrderStatus> statuses,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    List<OrderOutputDto> getOrdersByUserId(Long userId);

    @Transactional
    OrderOutputDto updateOrder(Long id, OrderInputDto inputDto);

    @Transactional
    void deleteOrderById(Long id);

    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "fallbackGetUser")
    UserDto enrichUser(Long userID);
}
