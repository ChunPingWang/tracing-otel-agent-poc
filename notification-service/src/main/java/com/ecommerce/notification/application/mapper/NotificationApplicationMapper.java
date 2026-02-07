package com.ecommerce.notification.application.mapper;

public final class NotificationApplicationMapper {

    private NotificationApplicationMapper() {
    }

    public static String buildNotificationMessage(String orderId, java.math.BigDecimal totalAmount) {
        return String.format("訂單 %s 已確認，金額 %s", orderId, totalAmount);
    }
}
