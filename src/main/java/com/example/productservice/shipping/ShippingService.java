package com.example.productservice.shipping;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ShippingService {
    public static final String SHIPPING_STATUS_TOPIC = "shipping-status-events";

    private final KafkaTemplate<String, ShippingStatusEvent> kafkaTemplate;

    public ShippingService(KafkaTemplate<String, ShippingStatusEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishStatus(Long orderId, String status) {
        kafkaTemplate.send(
                SHIPPING_STATUS_TOPIC,
                orderId.toString(),
                new ShippingStatusEvent(orderId, status, Instant.now()));
    }
}
