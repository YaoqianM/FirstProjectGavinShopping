package com.bfs.hibernateprojectdemo.controller;

import com.bfs.hibernateprojectdemo.domain.Order;
import com.bfs.hibernateprojectdemo.domain.Product;
import com.bfs.hibernateprojectdemo.domain.User;
import com.bfs.hibernateprojectdemo.service.HomePageService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
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
                    // Snapshot prices at purchase time
                    o.setRetailPriceAtPurchase(p.getRetailPrice());
                    o.setWholesalePriceAtPurchase(p.getWholesalePrice());
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

    // User-specific: view all orders of the current user (paginated 5 per page)
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/mine")
    public ResponseEntity<?> getMyOrders(@RequestParam(defaultValue = "0") int page, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try (Session s = sessionFactory.openSession()) {
            User user = s.createQuery("from User where username = :un", User.class)
                    .setParameter("un", principal.getName())
                    .uniqueResult();
            if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            Query<Order> q = s.createQuery("from Order o where o.userId = :uid order by o.orderTime desc", Order.class);
            q.setParameter("uid", user.getId());
            q.setFirstResult(page * 5);
            q.setMaxResults(5);
            return ResponseEntity.ok(q.list());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // User-specific: top-N most recently purchased products (exclude canceled), tiebreaker by productId
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/mine/recent/{n}")
    public ResponseEntity<?> getMyRecentProducts(@PathVariable int n, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (n <= 0) return ResponseEntity.badRequest().body("n must be >= 1");
        try (Session s = sessionFactory.openSession()) {
            User user = s.createQuery("from User where username = :un", User.class)
                    .setParameter("un", principal.getName())
                    .uniqueResult();
            if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            Query<Long> idQuery = s.createQuery(
                    "SELECT o.productId FROM Order o WHERE o.userId = :uid AND o.status = 'Completed' " +
                            "GROUP BY o.productId ORDER BY MAX(o.orderTime) DESC, o.productId ASC", Long.class);
            idQuery.setParameter("uid", user.getId());
            idQuery.setMaxResults(n);
            List<Long> productIds = idQuery.list();
            if (productIds == null || productIds.isEmpty()) return ResponseEntity.ok(java.util.Collections.emptyList());

            Query<Product> productQuery = s.createQuery(
                    "FROM Product p WHERE p.productId IN :ids ORDER BY p.productId ASC", Product.class);
            productQuery.setParameterList("ids", productIds);
            return ResponseEntity.ok(productQuery.list());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Admin dashboard endpoint with pagination (5 per page)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<?> getOrdersAdmin(@RequestParam(defaultValue = "0") int page) {
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
    //cancel complete order: case 1: the order has been completed, but we want to cancel it;
    //cancel processing order: case2: the order has been processing, but we want to cancel it;
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Session s = null;
        Transaction tx = null;
        try {
            s = sessionFactory.openSession();
            tx = s.beginTransaction();
            
            Order o = s.get(Order.class, id);
            if (o == null) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.notFound().build();
            }

            // Check user ownership first: USER can only cancel their own orders, ADMIN can cancel any
            User currentUser = s.createQuery("from User where username = :un", User.class)
                    .setParameter("un", principal.getName())
                    .uniqueResult();
            
            if (currentUser == null) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Check if user has ADMIN role or if it's their own order
            boolean isAdmin = "ADMIN".equals(currentUser.getRole());
            boolean isOwnOrder = o.getUserId().equals(currentUser.getId());
            
            if (!isAdmin && !isOwnOrder) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You can only cancel your own orders");
            }

            // Check if order is already in a state that cannot be canceled
            if ("Completed".equals(o.getStatus())) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.badRequest().body("Cannot cancel order in state: " + o.getStatus());
            }
            
            if (!"Processing".equals(o.getStatus())) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.badRequest().body("Cannot cancel order in state: " + o.getStatus());
            }

            // Restore product quantity
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
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception rollbackEx) {
                    e.printStackTrace();
                }
            }
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error canceling order: " + e.getMessage());
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    // case 3: the order has been calcelled, we we want to complete it from admin
    // case 4: the order has been processing, we want to complete it from admin;
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<?> completeOrder(@PathVariable Long id) {
        Session s = null;
        Transaction tx = null;
        try {
            s = sessionFactory.openSession();
            tx = s.beginTransaction();
            
            Order o = s.get(Order.class, id);
            if (o == null) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.notFound().build();
            }

            // Check if order is already in a state that cannot be completed
            if ("Canceled".equals(o.getStatus())) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.badRequest().body("Cannot complete order in state: " + o.getStatus());
            }
            
            if (!"Processing".equals(o.getStatus())) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.badRequest().body("Cannot complete order in state: " + o.getStatus());
            }

            o.setStatus("Completed");
            s.update(o);
            tx.commit();
            return ResponseEntity.ok(o);
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception rollbackEx) {
                    e.printStackTrace();
                }
            }
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error completing order: " + e.getMessage());
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}