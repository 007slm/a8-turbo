package com.example.shopservice.controller;

import com.example.shopservice.entity.Order;
import com.example.shopservice.entity.OrderItem;
import com.example.shopservice.entity.Product;
import com.example.shopservice.entity.User;
import com.example.shopservice.repository.OrderItemRepository;
import com.example.shopservice.repository.OrderRepository;
import com.example.shopservice.repository.ProductRepository;
import com.example.shopservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OrderItemControllerIT {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private User user;
    private Product product;
    private Order order;

    @BeforeEach
    void setup() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        
        User userObj = new User();
        userObj.setUsername("testuser");
        userObj.setEmail("testuser@example.com");
        user = userRepository.save(userObj);

        Product prodObj = new Product();
        prodObj.setName("TestProduct");
        prodObj.setPrice(new BigDecimal("25.99"));
        product = productRepository.save(prodObj);
        
        Order orderObj = new Order();
        orderObj.setUser(user);
        order = orderRepository.save(orderObj);
    }

    @Test
    void testCreateOrderItem() throws Exception {
        String itemJson = """
        {
          "product":{"id":%d},
          "quantity":3
        }
        """.formatted(product.getId());

        mockMvc.perform(post("/orders/" + order.getId() + "/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.product.id").value(product.getId()));
    }

    @Test
    void testGetOrderItems() throws Exception {
        // First create an order item
        String itemJson = """
        {
          "product":{"id":%d},
          "quantity":2
        }
        """.formatted(product.getId());

        mockMvc.perform(post("/orders/" + order.getId() + "/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemJson))
                .andExpect(status().isOk());

        // Then get the list of items
        mockMvc.perform(get("/orders/" + order.getId() + "/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].quantity").value(2));
    }

    @Test
    void testGetOrderItem() throws Exception {
        // First create an order item
        String itemJson = """
        {
          "product":{"id":%d},
          "quantity":4
        }
        """.formatted(product.getId());

        String response = mockMvc.perform(post("/orders/" + order.getId() + "/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        OrderItem orderItem = objectMapper.readValue(response, OrderItem.class);

        // Then get the specific item
        mockMvc.perform(get("/orders/" + order.getId() + "/items/" + orderItem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderItem.getId()))
                .andExpect(jsonPath("$.quantity").value(4));
    }

    @Test
    void testUpdateOrderItem() throws Exception {
        // First create an order item
        String itemJson = """
        {
          "product":{"id":%d},
          "quantity":1
        }
        """.formatted(product.getId());

        String response = mockMvc.perform(post("/orders/" + order.getId() + "/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        OrderItem orderItem = objectMapper.readValue(response, OrderItem.class);

        // Then update the item
        String updateJson = """
        {
          "product":{"id":%d},
          "quantity":5
        }
        """.formatted(product.getId());

        mockMvc.perform(put("/orders/" + order.getId() + "/items/" + orderItem.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void testDeleteOrderItem() throws Exception {
        // First create an order item
        String itemJson = """
        {
          "product":{"id":%d},
          "quantity":1
        }
        """.formatted(product.getId());

        String response = mockMvc.perform(post("/orders/" + order.getId() + "/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        OrderItem orderItem = objectMapper.readValue(response, OrderItem.class);

        // Then delete the item
        mockMvc.perform(delete("/orders/" + order.getId() + "/items/" + orderItem.getId()))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/orders/" + order.getId() + "/items/" + orderItem.getId()))
                .andExpect(status().isNotFound());
    }
}