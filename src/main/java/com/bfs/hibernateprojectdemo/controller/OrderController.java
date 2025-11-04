package com.bfs.hibernateprojectdemo.controller;

import com.bfs.hibernateprojectdemo.domain.Order;
import com.bfs.hibernateprojectdemo.service.HomePageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private HomePageService homePageService;

    @PostMapping
    public String placeOrder() {
        // user place order (auto stock decrement)
        return "Order placed";
    }

    @GetMapping("/all")
    public String getAllOrders() {
        // user/admin list orders (5/page)
        return "All orders";
    }

    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Long id) {
        // user/admin order detail
        return homePageService.getOrderDetail(id);
    }

    @PatchMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id) {
        // cancel processing order; restock
        return "Order canceled";
    }

    @PatchMapping("/{id}/complete")
    public String completeOrder(@PathVariable Long id) {
        // complete processing order
        return "Order completed";
    }
}