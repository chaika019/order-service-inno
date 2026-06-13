package by.chaika19.orderservice.mapper;

import by.chaika19.orderservice.dto.OrderItemOutputDto;
import by.chaika19.orderservice.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(target = "itemId", source = "item.id")
    OrderItemOutputDto toDto(OrderItem orderItem);

}
