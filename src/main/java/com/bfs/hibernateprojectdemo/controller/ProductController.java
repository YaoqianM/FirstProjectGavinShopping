package com.bfs.hibernateprojectdemo.controller;


import com.bfs.hibernateprojectdemo.domain.Product;
import com.bfs.hibernateprojectdemo.service.HomePageService;
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
