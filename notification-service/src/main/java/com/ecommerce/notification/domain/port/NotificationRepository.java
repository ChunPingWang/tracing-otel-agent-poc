package com.ecommerce.notification.domain.port;

import com.ecommerce.notification.domain.model.Notification;

public interface NotificationRepository {
    Notification save(Notification notification);
}
