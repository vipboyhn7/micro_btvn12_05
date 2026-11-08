package com.example.productservice.shipping;

import com.example.productservice.order.Order;
import com.example.productservice.order.OrderRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
public class ShippingEventConsumers {
    private final OrderRepository orderRepository;

    public ShippingEventConsumers(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaListener(
            topics = ShippingService.SHIPPING_STATUS_TOPIC,
            groupId = "order-service")
    public void updateOrderStatus(ShippingStatusEvent event) {
        if (!"DELIVERED".equalsIgnoreCase(event.status())) {
            return;
        }

        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND, "Order not found: " + event.orderId()));
        order.markCompleted();
        orderRepository.save(order);
    }

    @KafkaListener(
            topics = ShippingService.SHIPPING_STATUS_TOPIC,
            groupId = "notification-service")
    public void sendDeliveryNotification(ShippingStatusEvent event) {
        if ("DELIVERED".equalsIgnoreCase(event.status())) {
            System.out.printf(
                    "Notification-Service: order %d delivered, sending congratulations%n",
                    event.orderId());
        }
    }
}
