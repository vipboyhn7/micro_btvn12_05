package com.example.productservice.order;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long productId;
    private Integer quantity;
    private String customerEmail;
    private Instant createdAt;
    private String status;

    protected Order() {
    }

    public Order(Long productId, Integer quantity, String customerEmail) {
        this.productId = productId;
        this.quantity = quantity;
        this.customerEmail = customerEmail;
        this.createdAt = Instant.now();
        this.status = "CREATED";
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void markCompleted() {
        this.status = "COMPLETED";
    }
}
