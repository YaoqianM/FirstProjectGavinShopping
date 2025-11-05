package com.bfs.hibernateprojectdemo.controller;

import com.bfs.hibernateprojectdemo.domain.Order;
import com.bfs.hibernateprojectdemo.domain.Product;
import com.bfs.hibernateprojectdemo.domain.User;
import com.bfs.hibernateprojectdemo.service.HomePageService;
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
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    static class OrderItem { public Long productId; public int quantity; }
    static class OrderRequest { public List<OrderItem> order; }

    private final SessionFactory sessionFactory;
    private final HomePageService homePageService;

    @Autowired
    public OrderController(SessionFactory sessionFactory, HomePageService homePageService) {
        this.sessionFactory = sessionFactory;
        this.homePageService = homePageService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest req, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            try {
                User user = s.createQuery("from User where username = :un", User.class)
                        .setParameter("un", principal.getName())
                        .uniqueResult();

                if (user == null) {
                    tx.rollback();
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                if (req.order == null || req.order.isEmpty()) {
                    tx.rollback();
                    return ResponseEntity.badRequest().body("Order items cannot be empty");
                }

                for (OrderItem item : req.order) {
                    Product p = s.get(Product.class, item.productId);
                    if (p == null || p.getQuantity() < item.quantity) {
                        tx.rollback();
                        return ResponseEntity.badRequest().body("Not enough stock for product: " + item.productId);
                    }
                    p.setQuantity(p.getQuantity() - item.quantity);
                    s.update(p);

                    Order o = new Order();
                    o.setUserId(user.getId());
                    o.setProductId(item.productId);
                    o.setQuantity(item.quantity);
                    o.setStatus("Processing");
                    o.setOrderTime(LocalDateTime.now());
                    s.save(o);
                }
                tx.commit();
                return ResponseEntity.status(HttpStatus.CREATED).build();
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error placing order: " + e.getMessage());
            }
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/all")
    public ResponseEntity<List<Order>> getAllOrders(@RequestParam(defaultValue = "0") int page) {
        try (Session s = sessionFactory.openSession()) {
            Query<Order> q = s.createQuery("from Order o order by o.orderTime desc", Order.class);
            q.setFirstResult(page * 5);
            q.setMaxResults(5);
            return ResponseEntity.ok(q.list());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        try {
            Order order = homePageService.getOrderDetail(id);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            try {
                Order o = s.get(Order.class, id);
                if (o == null) {
                    tx.rollback();
                    return ResponseEntity.notFound().build();
                }

                if (!"Processing".equals(o.getStatus())) {
                    tx.rollback();
                    return ResponseEntity.badRequest().body("Cannot cancel order in state: " + o.getStatus());
                }

                Product p = s.get(Product.class, o.getProductId());
                if (p != null) {
                    p.setQuantity(p.getQuantity() + o.getQuantity());
                    s.update(p);
                }
                o.setStatus("Canceled");
                s.update(o);
                tx.commit();
                return ResponseEntity.ok(o);
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error canceling order: " + e.getMessage());
            }
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<?> completeOrder(@PathVariable Long id) {
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            try {
                Order o = s.get(Order.class, id);
                if (o == null) {
                    tx.rollback();
                    return ResponseEntity.notFound().build();
                }

                if (!"Processing".equals(o.getStatus())) {
                    tx.rollback();
                    return ResponseEntity.badRequest().body("Cannot complete order in state: " + o.getStatus());
                }

                o.setStatus("Completed");
                s.update(o);
                tx.commit();
                return ResponseEntity.ok(o);
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error completing order: " + e.getMessage());
            }
        }
    }
}