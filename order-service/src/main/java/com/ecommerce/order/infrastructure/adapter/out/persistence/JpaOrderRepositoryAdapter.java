package com.ecommerce.order.infrastructure.adapter.out.persistence;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderItem;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.domain.port.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private final SpringDataOrderRepository springDataRepo;

    public JpaOrderRepositoryAdapter(SpringDataOrderRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = toEntity(order);
        OrderJpaEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Order> findByOrderId(String orderId) {
        return springDataRepo.findByOrderId(orderId).map(this::toDomain);
    }

    private OrderJpaEntity toEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(order.getId());
        entity.setOrderId(order.getOrderId());
        entity.setCustomerId(order.getCustomerId());
        entity.setStatus(order.getStatus().name());
        entity.setTotalAmount(order.getTotalAmount());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());
        entity.setItems(toItemEntities(order.getItems(), entity));
        return entity;
    }

    private List<OrderItemJpaEntity> toItemEntities(List<OrderItem> items,
                                                     OrderJpaEntity parent) {
        return items.stream().map(item -> {
            OrderItemJpaEntity itemEntity = new OrderItemJpaEntity();
            itemEntity.setProductId(item.getProductId());
            itemEntity.setQuantity(item.getQuantity());
            itemEntity.setUnitPrice(item.getUnitPrice());
            itemEntity.setOrder(parent);
            return itemEntity;
        }).collect(Collectors.toList());
    }

    private Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(e -> new OrderItem(e.getProductId(), e.getQuantity(), e.getUnitPrice()))
                .collect(Collectors.toList());

        return Order.reconstitute(
                entity.getId(),
                entity.getOrderId(),
                entity.getCustomerId(),
                OrderStatus.valueOf(entity.getStatus()),
                entity.getTotalAmount(),
                items,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
