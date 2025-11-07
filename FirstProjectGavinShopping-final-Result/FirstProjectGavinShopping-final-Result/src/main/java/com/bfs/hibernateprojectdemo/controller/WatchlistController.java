package com.bfs.hibernateprojectdemo.controller;

import com.bfs.hibernateprojectdemo.domain.Product;
import com.bfs.hibernateprojectdemo.domain.User;
import com.bfs.hibernateprojectdemo.domain.Watchlist;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

import com.bfs.hibernateprojectdemo.dto.MessageResponse;

@RestController
@RequestMapping("/watchlist")
@PreAuthorize("hasRole('USER')") // Class-level auth is fine
public class WatchlistController {

    private final SessionFactory sessionFactory;

    @Autowired
    public WatchlistController(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @GetMapping("/products/all")
    public ResponseEntity<?> getWatchlist(Principal principal) {
        // 1. Robust Principal Check
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try (Session s = sessionFactory.openSession()) {
            // 2. Get User ID from username
            User dbUser = s.createQuery("from User u where u.username = :un", User.class)
                    .setParameter("un", principal.getName())
                    .uniqueResult();

            if (dbUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            // 3. Fetch Products
            Query<Product> q = s.createQuery(
                    "select p from Product p where p.productId in (" +
                            " select w.productId from Watchlist w where w.userId = :uid" +
                            " ) and p.quantity > 0", Product.class);
            q.setParameter("uid", dbUser.getId());
            List<Product> products = q.list();
            if (products == null || products.isEmpty()) {
                return ResponseEntity.ok(new com.bfs.hibernateprojectdemo.dto.MessageResponse("Your watchlist is empty"));
            }
            // Map to user-safe DTOs
            java.util.List<com.bfs.hibernateprojectdemo.dto.UserProductDto> dtos = new java.util.ArrayList<>();
            for (Product p : products) {
                com.bfs.hibernateprojectdemo.dto.UserProductDto dto = new com.bfs.hibernateprojectdemo.dto.UserProductDto();
                dto.setProductId(p.getProductId());
                dto.setName(p.getName());
                dto.setDescription(p.getDescription());
                dto.setRetailPrice(p.getRetailPrice());
                dtos.add(dto);
            }
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            e.printStackTrace(); // Helpful for debugging 500s in console
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/product/{id}")
    public ResponseEntity<?> addToWatchlist(@PathVariable("id") Long productId,
                                               Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Unauthorized: please log in"));

        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            try {
                User dbUser = s.createQuery("from User u where u.username = :un", User.class)
                        .setParameter("un", principal.getName())
                        .uniqueResult();

                if (dbUser == null) {
                    tx.rollback();
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new MessageResponse("Unauthorized: user not found"));
                }

                Product product = s.get(Product.class, productId);
                if (product == null) {
                    tx.rollback();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new MessageResponse("Product not found"));
                }

                Watchlist existing = s.createQuery(
                                "from Watchlist w where w.userId = :uid and w.productId = :pid", Watchlist.class)
                        .setParameter("uid", dbUser.getId())
                        .setParameter("pid", productId)
                        .uniqueResult();

                if (existing == null) {
                    Watchlist w = new Watchlist();
                    w.setUserId(dbUser.getId());
                    w.setProductId(productId);
                    s.save(w);
                    tx.commit();
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(new MessageResponse("Added to watchlist successfully"));
                }
                tx.commit();
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new MessageResponse("Item already in watchlist"));
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new MessageResponse("Failed to add to watchlist"));
            }
        }
    }

    @DeleteMapping("/product/{id}")
    public ResponseEntity<?> removeFromWatchlist(@PathVariable("id") Long productId,
                                                    Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Unauthorized: please log in"));

        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            try {
                User dbUser = s.createQuery("from User u where u.username = :un", User.class)
                        .setParameter("un", principal.getName())
                        .uniqueResult();

                if (dbUser == null) {
                    tx.rollback();
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new MessageResponse("Unauthorized: user not found"));
                }

                int affected = s.createQuery("delete from Watchlist where userId = :uid and productId = :pid")
                        .setParameter("uid", dbUser.getId())
                        .setParameter("pid", productId)
                        .executeUpdate();

                tx.commit();
                if (affected > 0) {
                    return ResponseEntity.ok(new MessageResponse("Successfully deleted from watchlist"));
                } else {
                    return ResponseEntity.status(HttpStatus.NO_CONTENT)
                            .body(new MessageResponse("Item not in watchlist"));
                }
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new MessageResponse("Failed to remove from watchlist"));
            }
        }
    }
}