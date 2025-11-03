package com.bfs.hibernateprojectdemo.controller;


import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    @GetMapping("/all")
    public String getAllProducts() {
        // user: hide OOS; admin: include OOS
        return "All products";
    }

    @GetMapping("/{id}")
    public String getProductById(@PathVariable Long id) {
        // user detail (hide quantity)
        return "Product detail";
    }

    @PatchMapping("/{id}")
    public String updateProduct(@PathVariable Long id) {
        // admin update name/desc/prices/quantity
        return "Product updated";
    }

    @PostMapping
    public String createProduct() {
        // admin create product
        return "Product created";
    }

    @GetMapping("/frequent/{n}")
    public String topFrequent(@PathVariable int n) {
        // user top-N most frequently purchased
        return "Top frequent";
    }

    @GetMapping("/recent/{n}")
    public String topRecent(@PathVariable int n) {
        // user top-N most recent
        return "Top recent";
    }

    @GetMapping("/profit/{n}")
    public String topProfit(@PathVariable int n) {
        // admin top-N most profitable
        return "Top profit";
    }

    @GetMapping("/popular/{n}")
    public String topPopular(@PathVariable int n) {
        // admin top-N by units sold
        return "Top popular";
    }
}
