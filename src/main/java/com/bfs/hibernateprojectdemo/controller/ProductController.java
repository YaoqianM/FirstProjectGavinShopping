package com.bfs.hibernateprojectdemo.controller;


import com.bfs.hibernateprojectdemo.domain.Product;
import com.bfs.hibernateprojectdemo.service.HomePageService;
import com.bfs.hibernateprojectdemo.service.ProductAnalyticsService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;   // add this import

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
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product changes) {
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            Product p = s.get(Product.class, id);
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
