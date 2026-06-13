package by.chaika19.orderservice.dto;

import by.chaika19.orderservice.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record
OrderOutputDto(

        Long id,

        UserDto user,

        OrderStatus status,

        BigDecimal totalPrice,

        boolean deleted,

        List<OrderItemOutputDto> orderItems,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) { }
