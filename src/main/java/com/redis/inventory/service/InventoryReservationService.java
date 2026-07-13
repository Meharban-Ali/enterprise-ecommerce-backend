package com.redis.inventory.service;

import com.redis.order.entity.Order;

public interface InventoryReservationService {
    void reserveInventory(Order order);
    void releaseReservation(Order order);
    void commitReservation(Order order);
}
