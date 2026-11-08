package com.example.productservice.order;

import java.util.concurrent.TimeUnit;

import com.example.productservice.product.Product;
import com.example.productservice.product.ProductRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    public static final String ORDER_EVENTS_TOPIC = "order-events";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final RedissonClient redissonClient;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate,
            RedissonClient redissonClient) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.redissonClient = redissonClient;
    }

    @Transactional
    @CacheEvict(cacheNames = "products", key = "#request.productId()")
    public Order create(CreateOrderRequest request) {
        String lockName = "lock:product:" + request.productId();
        RLock lock = redissonClient.getLock(lockName);

        try {
            if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Product is being purchased by another request");
            }

            Product product = productRepository.findById(request.productId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Product not found: " + request.productId()));
            if (request.quantity() > product.getStockQuantity()) {
                throw new InsufficientStockException(
                        product.getId(), request.quantity(), product.getStockQuantity());
            }

            product.decreaseStock(request.quantity());
            productRepository.save(product);

            Order order = orderRepository.save(new Order(
                    request.productId(), request.quantity(), request.customerEmail()));

            kafkaTemplate.send(
                    ORDER_EVENTS_TOPIC,
                    order.getId().toString(),
                    OrderCreatedEvent.from(order));
            return order;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Could not acquire product lock", exception);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public Order getById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found: " + orderId));
    }
}
