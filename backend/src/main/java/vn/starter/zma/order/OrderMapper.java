package vn.starter.zma.order;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.starter.zma.order.OrderDtos.OrderDto;
import vn.starter.zma.order.OrderDtos.OrderItemDto;

@Mapper
public interface OrderMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.name")
    OrderDto toDto(Order o);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "items", ignore = true)
    OrderDto toSummary(Order o);

    @Mapping(target = "productId", source = "product.id")
    OrderItemDto toDto(OrderItem i);
}
