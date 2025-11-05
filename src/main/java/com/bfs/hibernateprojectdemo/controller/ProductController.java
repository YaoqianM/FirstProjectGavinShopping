package com.bfs.hibernateprojectdemo.controller;

import com.bfs.hibernateprojectdemo.domain.Product;
import com.bfs.hibernateprojectdemo.service.HomePageService;
import com.bfs.hibernateprojectdemo.service.ProductAnalyticsService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final SessionFactory sessionFactory;
    private final HomePageService homePageService;
    private final ProductAnalyticsService productAnalyticsService;

    public ProductController(SessionFactory sessionFactory,
                             HomePageService homePageService,
                             ProductAnalyticsService productAnalyticsService) {
        this.sessionFactory = sessionFactory;
        this.homePageService = homePageService;
        this.productAnalyticsService = productAnalyticsService;
    }

// Inside ProductController

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/all")
    public ResponseEntity<List<Product>> getAllProducts(@RequestParam(defaultValue = "false") boolean admin) {
        try {
            return ResponseEntity.ok(homePageService.getAvailableProducts(admin));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProductDetail(@PathVariable Long productId,
                                    @RequestParam(defaultValue = "false") boolean admin) {
        try {
            Product product = homePageService.getProductDetail(productId, admin);
            if (product == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/{productId}")
    public ResponseEntity<?> updateProduct(@PathVariable Long productId, @RequestBody Product changes) {
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            Product p = s.get(Product.class, productId);

            if (p == null) {
                return ResponseEntity.notFound().build();
            }

            // Apply partial updates
            if (changes.getName() != null) p.setName(changes.getName());
            if (changes.getDescription() != null) p.setDescription(changes.getDescription());
            if (changes.getRetailPrice() > 0) p.setRetailPrice(changes.getRetailPrice());
            if (changes.getWholesalePrice() > 0) p.setWholesalePrice(changes.getWholesalePrice());
            if (changes.getQuantity() >= 0) p.setQuantity(changes.getQuantity());

            s.update(p);
            tx.commit();
            return ResponseEntity.ok(p);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating product: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product p) {
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            s.save(p);
            tx.commit();
            return ResponseEntity.status(HttpStatus.CREATED).body(p);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating product: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/frequent/{n}")
    public ResponseEntity<List<Product>> getTopFrequent(@PathVariable int n) {
        try {
            return ResponseEntity.ok(productAnalyticsService.getTopFrequent(n));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/recent/{n}")
    public ResponseEntity<List<Product>> getTopRecent(@PathVariable int n) {
        try {
            return ResponseEntity.ok(productAnalyticsService.getTopRecent(n));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/profit/{n}")
    public ResponseEntity<List<Product>> getTopProfit(@PathVariable int n) {
        try {
            return ResponseEntity.ok(productAnalyticsService.getTopProfit(n));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/popular/{n}")
    public ResponseEntity<List<Product>> getTopPopular(@PathVariable int n) {
        try {
            return ResponseEntity.ok(productAnalyticsService.getTopPopular(n));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}