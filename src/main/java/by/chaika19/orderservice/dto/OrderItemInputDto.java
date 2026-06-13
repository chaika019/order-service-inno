package by.chaika19.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemInputDto(

        @NotNull(message = "Item id is required")
        Long itemId,

        @Min(value = 1, message = "Quantity must be greater than 0")
        int quantity

) { }
