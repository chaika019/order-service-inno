package by.chaika19.orderservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderInputDto (

        @NotNull(message = "User id is required")
        Long userId,

        @NotEmpty(message = "Order must contain at least one item")
        List<OrderItemInputDto> items

) { }
