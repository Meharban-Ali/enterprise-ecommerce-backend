package com.redis.notification.service;

public interface NotificationQueueService {
    void enqueue(Long notificationId);
    void enqueue(String queueName, Long notificationId);
    Long dequeue();
    Long dequeue(String queueName);
    void acknowledge(Long notificationId);
    void requeue(Long notificationId);
}
