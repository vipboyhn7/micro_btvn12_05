package com.example.productservice.shipping;

import java.time.Instant;

public record ShippingStatusEvent(
        Long orderId,
        String status,
        Instant occurredAt
) {
}
