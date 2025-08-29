package com.example.shopservice.controller;

import com.example.shopservice.entity.Order;
import com.example.shopservice.entity.OrderItem;
import com.example.shopservice.entity.Product;
import com.example.shopservice.entity.User;
import com.example.shopservice.repository.OrderRepository;
import com.example.shopservice.repository.ProductRepository;
import com.example.shopservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Order order) {
        if (order.getUser() == null || order.getUser().getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<User> userOpt = userRepository.findById(order.getUser().getId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        order.setUser(userOpt.get());

        List<OrderItem> items = order.getOrderItems();
        if (items != null) {
            for (OrderItem item : items) {
                if (item.getProduct() == null || item.getProduct().getId() == null){
                    return ResponseEntity.badRequest().build();
                }
                Optional<Product> prodOpt = productRepository.findById(item.getProduct().getId());
                if (prodOpt.isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                item.setProduct(prodOpt.get());
                item.setOrder(order);
            }
        }
        return ResponseEntity.ok(orderRepository.save(order));
    }

    @GetMapping
    public Page<Order> list(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> get(@PathVariable Long id) {
        return orderRepository.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> update(@PathVariable Long id, @RequestBody Order order) {
        return orderRepository.findById(id)
                .map(existing -> {
                    if (order.getUser() != null && order.getUser().getId() != null) {
                        Optional<User> userOpt = userRepository.findById(order.getUser().getId());
                        userOpt.ifPresent(existing::setUser);
                    }
                    if (order.getOrderDate() != null) {
                        existing.setOrderDate(order.getOrderDate());
                    }
                    return ResponseEntity.ok(orderRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(existing -> {
                    orderRepository.delete(existing);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
