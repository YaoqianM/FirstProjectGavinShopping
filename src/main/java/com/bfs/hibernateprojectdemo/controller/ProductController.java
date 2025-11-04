package com.bfs.hibernateprojectdemo.controller;


import com.bfs.hibernateprojectdemo.domain.Product;
import com.bfs.hibernateprojectdemo.service.HomePageService;
import com.bfs.hibernateprojectdemo.service.ProductAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    @Autowired
    private HomePageService homePageService;

    @GetMapping("/all")
    public List<Product> getAllProducts(@RequestParam(defaultValue = "false") boolean admin) {
        return homePageService.getAvailableProducts(admin);
    }

    @GetMapping("/{id}")
    public Product getProductDetail(@PathVariable Long id,
                                    @RequestParam(defaultValue = "false") boolean admin) {
        return homePageService.getProductDetail(id, admin);
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
    @Autowired
    private ProductAnalyticsService productAnalyticsService;

    @GetMapping("/frequent/{n}")
    public List<Product> getTopFrequent(@PathVariable int n) {
        return productAnalyticsService.getTopFrequent(n);
    }

    @GetMapping("/recent/{n}")
    public List<Product> getTopRecent(@PathVariable int n) {
        return productAnalyticsService.getTopRecent(n);
    }

    @GetMapping("/profit/{n}")
    public List<Product> getTopProfit(@PathVariable int n) {
        return productAnalyticsService.getTopProfit(n);
    }

    @GetMapping("/popular/{n}")
    public List<Product> getTopPopular(@PathVariable int n) {
        return productAnalyticsService.getTopPopular(n);
    }
}
