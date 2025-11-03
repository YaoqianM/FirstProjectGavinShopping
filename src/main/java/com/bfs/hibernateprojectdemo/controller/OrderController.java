package com.bfs.hibernateprojectdemo.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

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
    public String getOrderById(@PathVariable Long id) {
        // user/admin order detail
        return "Order detail";
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