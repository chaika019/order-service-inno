package by.chaika19.orderservice.mapper;

import by.chaika19.orderservice.dto.OrderOutputDto;
import by.chaika19.orderservice.model.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {

    @Mapping(target = "user", ignore = true)
    OrderOutputDto toDto(Order order);

}
