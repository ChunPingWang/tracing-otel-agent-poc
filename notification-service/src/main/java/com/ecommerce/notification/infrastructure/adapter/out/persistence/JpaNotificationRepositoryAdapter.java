package com.ecommerce.notification.infrastructure.adapter.out.persistence;

import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.model.NotificationStatus;
import com.ecommerce.notification.domain.port.NotificationRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaNotificationRepositoryAdapter implements NotificationRepository {

    private final SpringDataNotificationRepository springDataRepo;

    public JpaNotificationRepositoryAdapter(SpringDataNotificationRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = toEntity(notification);
        NotificationJpaEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    private NotificationJpaEntity toEntity(Notification n) {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setId(n.getId());
        entity.setOrderId(n.getOrderId());
        entity.setCustomerId(n.getCustomerId());
        entity.setCustomerEmail(n.getCustomerEmail());
        entity.setStatus(n.getStatus().name());
        entity.setMessage(n.getMessage());
        entity.setCreatedAt(n.getCreatedAt());
        return entity;
    }

    private Notification toDomain(NotificationJpaEntity entity) {
        return Notification.reconstitute(
                entity.getId(),
                entity.getOrderId(),
                entity.getCustomerId(),
                entity.getCustomerEmail(),
                NotificationStatus.valueOf(entity.getStatus()),
                entity.getMessage(),
                entity.getCreatedAt()
        );
    }
}
