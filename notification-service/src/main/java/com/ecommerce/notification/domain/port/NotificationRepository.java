package com.ecommerce.notification.domain.port;

import com.ecommerce.notification.domain.model.Notification;

/**
 * Domain port for persisting Notification entities.
 */
public interface NotificationRepository {

    /** Saves a notification record and returns the persisted instance. */
    Notification save(Notification notification);
}
