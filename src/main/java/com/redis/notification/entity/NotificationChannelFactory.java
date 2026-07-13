package com.redis.notification.entity;

import com.redis.notification.service.NotificationChannelService;

import com.redis.notification.entity.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationChannelFactory {

    private final Map<NotificationChannel, NotificationChannelService> strategyMap;

    /**
     * Initializes O(1) strategy mapping registry at startup time.
     * Iterates over discovered beans and maps them to respective NotificationChannels.
     */
    public NotificationChannelFactory(List<NotificationChannelService> services) {
        this.strategyMap = new EnumMap<>(NotificationChannel.class);
        for (NotificationChannelService service : services) {
            for (NotificationChannel channel : NotificationChannel.values()) {
                if (service.supports(channel)) {
                    this.strategyMap.put(channel, service);
                }
            }
        }
    }

    public NotificationChannelService getService(NotificationChannel channel) {
        NotificationChannelService service = strategyMap.get(channel);
        if (service == null) {
            throw new IllegalArgumentException("No strategy registered for channel: " + channel);
        }
        return service;
    }
}
