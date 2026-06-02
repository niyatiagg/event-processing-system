package com.example.eps.order.web;

import com.example.eps.order.domain.OrderEntity;
import com.example.eps.order.service.OrderService;
import com.example.eps.order.web.dto.CreateOrderRequest;
import com.example.eps.order.web.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
                                                UriComponentsBuilder uriBuilder) {
        OrderEntity order = orderService.createOrder(request);
        URI location = uriBuilder.path("/api/v1/orders/{id}").buildAndExpand(order.getId()).toUri();
        return ResponseEntity.created(location).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable String id) {
        return OrderResponse.from(orderService.getOrder(id));
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam(required = false) String customerId) {
        return orderService.listOrders(customerId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.status(HttpStatus.OK).body("OK");
    }
}
