package com.bfs.hibernateprojectdemo.controller;


import com.bfs.hibernateprojectdemo.domain.Product;
import com.bfs.hibernateprojectdemo.service.HomePageService;
import com.bfs.hibernateprojectdemo.service.ProductAnalyticsService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")

public class ProductController {
    private final SessionFactory sessionFactory;

    @Autowired
    private HomePageService homePageService;
    public ProductController(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/all")
    public List<Product> getAllProducts(@RequestParam(defaultValue = "false") boolean admin) {
        return homePageService.getAvailableProducts(admin);
    }
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/{productId}")
    public Product getProductDetail(@PathVariable Long productId,
                                    @RequestParam(defaultValue = "false") boolean admin) {
        return homePageService.getProductDetail(productId, admin);
    }
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/{productId}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long productId, @RequestBody Product changes) {
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            Product p = s.get(Product.class, productId);
            if (p == null) return ResponseEntity.notFound().build();
            if (changes.getName() != null) p.setName(changes.getName());
            if (changes.getDescription() != null) p.setDescription(changes.getDescription());
            if (changes.getRetailPrice() > 0) p.setRetailPrice(changes.getRetailPrice());
            if (changes.getWholesalePrice() > 0) p.setWholesalePrice(changes.getWholesalePrice());
            if (changes.getQuantity() >= 0) p.setQuantity(changes.getQuantity());
            s.update(p); tx.commit();
            return ResponseEntity.ok(p);
        }
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product p) {
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            s.save(p);
            tx.commit();
            return ResponseEntity.status(HttpStatus.CREATED).body(p);
        }
    }
    @Autowired
    private ProductAnalyticsService productAnalyticsService;
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/frequent/{n}")
    public List<Product> getTopFrequent(@PathVariable int n) {
        return productAnalyticsService.getTopFrequent(n);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/recent/{n}")
    public List<Product> getTopRecent(@PathVariable int n) {
        return productAnalyticsService.getTopRecent(n);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/profit/{n}")
    public List<Product> getTopProfit(@PathVariable int n) {
        return productAnalyticsService.getTopProfit(n);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/popular/{n}")
    public List<Product> getTopPopular(@PathVariable int n) {
        return productAnalyticsService.getTopPopular(n);
    }
}
