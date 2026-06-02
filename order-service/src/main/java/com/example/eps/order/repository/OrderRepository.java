package com.example.eps.order.repository;

import com.example.eps.order.domain.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {

    List<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
