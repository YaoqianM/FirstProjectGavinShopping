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
import java.util.Collections;
import java.util.List;

import com.bfs.hibernateprojectdemo.dto.MessageResponse;
import com.bfs.hibernateprojectdemo.dto.OrderViewDto;
import com.bfs.hibernateprojectdemo.exception.NotEnoughInventoryException;

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
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Unauthorized"));

        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            try {
                User user = s.createQuery("from User where username = :un", User.class)
                        .setParameter("un", principal.getName())
                        .uniqueResult();

                if (user == null) {
                    tx.rollback();
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new MessageResponse("Unauthorized"));
                }

                if (req.order == null || req.order.isEmpty()) {
                    tx.rollback();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new MessageResponse("Order items cannot be empty"));
                }
                // Validate each item has a positive quantity
                for (OrderItem item : req.order) {
                    if (item == null || item.quantity <= 0) {
                        tx.rollback();
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(new MessageResponse("Each order item quantity must be >= 1"));
                    }
                }

                // Collect all reasons if any items are invalid (not found or insufficient stock)
                java.util.List<String> reasons = new java.util.ArrayList<>();
                for (OrderItem item : req.order) {
                    Product p = s.get(Product.class, item.productId);
                    if (p == null) {
                        reasons.add("Product not found: " + item.productId);
                        continue;
                    }
                    if (p.getQuantity() < item.quantity) {
                        reasons.add("Insufficient stock for product: " + p.getName());
                    }
                }

                if (!reasons.isEmpty()) {
                    tx.rollback();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new MessageResponse("Failed to process order: " + String.join("; ", reasons)));
                }

                // All items valid, proceed with stock deduction and order creation
                for (OrderItem item : req.order) {
                    Product p = s.get(Product.class, item.productId);
                    int newQty = p.getQuantity() - item.quantity;
                    // Snapshot prices before potential deletion
                    double retailSnapshot = p.getRetailPrice();
                    double wholesaleSnapshot = p.getWholesalePrice();
                    if (newQty <= 0) {
                        // Remove product completely when stock reaches <= 0
                        s.delete(p);
                    } else {
                        p.setQuantity(newQty);
                        s.update(p);
                    }

                    Order o = new Order();
                    o.setUserId(user.getId());
                    o.setProductId(item.productId);
                    o.setQuantity(item.quantity);
                    o.setStatus("Processing");
                    o.setOrderTime(LocalDateTime.now());
                    // Snapshot prices at purchase time
                    o.setRetailPriceAtPurchase(retailSnapshot);
                    o.setWholesalePriceAtPurchase(wholesaleSnapshot);
                    // Snapshot product name and description at purchase time
                    o.setProductNameAtPurchase(p.getName());
                    o.setProductDescriptionAtPurchase(p.getDescription());
                    s.save(o);
                }
                tx.commit();
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(new MessageResponse("Order created successfully"));
            } catch (NotEnoughInventoryException ie) {
                if (tx.isActive()) tx.rollback();
                throw ie; // Let global handler shape the response
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new MessageResponse("Error placing order"));
            }
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/all")
    public ResponseEntity<?> getAllOrders(@RequestParam(defaultValue = "0") int page, Principal principal) {
        try (Session s = sessionFactory.openSession()) {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("Unauthorized"));
            }

            User currentUser = s.createQuery("from User where username = :un", User.class)
                    .setParameter("un", principal.getName())
                    .uniqueResult();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("Unauthorized"));
            }

            boolean isAdmin = "ADMIN".equals(currentUser.getRole());
            if (isAdmin) {
                Query<Order> q = s.createQuery("from Order o order by o.orderTime desc", Order.class);
                q.setFirstResult(page * 5);
                q.setMaxResults(5);
                List<Order> orders = q.list();
                if (orders == null || orders.isEmpty()) {
                    return ResponseEntity.ok(new MessageResponse("No orders found"));
                }
                return ResponseEntity.ok(orders);
            } else {
                Query<Order> q = s.createQuery("from Order o where o.userId = :uid order by o.orderTime desc", Order.class);
                q.setParameter("uid", currentUser.getId());
                q.setFirstResult(page * 5);
                q.setMaxResults(5);
                List<Order> orders = q.list();
                // Map to user view DTO (hide wholesale price at purchase)
                List<OrderViewDto> dtos = new java.util.ArrayList<>();
                for (Order o : orders) {
                    if ("Canceled".equals(o.getStatus())) continue;
                    OrderViewDto dto = new OrderViewDto();
                    dto.setId(o.getId());
                    dto.setProductId(o.getProductId());
                    dto.setQuantity(o.getQuantity());
                    dto.setStatus(o.getStatus());
                    dto.setOrderTime(o.getOrderTime());
                    dto.setProductName(o.getProductNameAtPurchase());
                    dto.setProductDescription(o.getProductDescriptionAtPurchase());
                    dtos.add(dto);
                }
                if (dtos.isEmpty()) {
                    return ResponseEntity.ok(new MessageResponse("No orders found"));
                }
                return ResponseEntity.ok(dtos);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to fetch orders"));
        }
    }

    // User-specific: view all orders of the current user (paginated 5 per page)
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/mine")
    public ResponseEntity<?> getMyOrders(@RequestParam(defaultValue = "0") int page, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Unauthorized: please log in"));
        try (Session s = sessionFactory.openSession()) {
            User user = s.createQuery("from User where username = :un", User.class)
                    .setParameter("un", principal.getName())
                    .uniqueResult();
            if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Unauthorized: user not found"));

            Query<Order> q = s.createQuery("from Order o where o.userId = :uid order by o.orderTime desc", Order.class);
            q.setParameter("uid", user.getId());
            q.setFirstResult(page * 5);
            q.setMaxResults(5);
            List<Order> orders = q.list();
            // Auto-cancel any zero-quantity orders to keep data consistent
            Transaction tx = s.beginTransaction();
            for (Order o : orders) {
                if (o.getQuantity() <= 0 && !"Canceled".equals(o.getStatus())) {
                    o.setStatus("Canceled");
                    s.update(o);
                }
            }
            tx.commit();
            // Map to view DTO for user: omit snapshot prices and hide canceled orders
            List<OrderViewDto> dtos = new java.util.ArrayList<>();
            for (Order o : orders) {
                if ("Canceled".equals(o.getStatus())) continue;
                OrderViewDto dto = new OrderViewDto();
                dto.setId(o.getId());
                dto.setProductId(o.getProductId());
                dto.setQuantity(o.getQuantity());
                dto.setStatus(o.getStatus());
                dto.setOrderTime(o.getOrderTime());
                dto.setProductName(o.getProductNameAtPurchase());
                dto.setProductDescription(o.getProductDescriptionAtPurchase());
                dtos.add(dto);
            }
            if (dtos.isEmpty()) {
                return ResponseEntity.ok(new MessageResponse("No orders found"));
            }
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to fetch orders"));
        }
    }

    // User-specific: top-N most recently ordered products (include all statuses), tiebreaker by productId
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/mine/recent/{n}")
    public ResponseEntity<?> getMyRecentProducts(@PathVariable int n, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Unauthorized: please log in"));
        if (n <= 0) return ResponseEntity.badRequest().body(new MessageResponse("n must be >= 1"));
        try (Session s = sessionFactory.openSession()) {
            User user = s.createQuery("from User where username = :un", User.class)
                    .setParameter("un", principal.getName())
                    .uniqueResult();
            if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Unauthorized: user not found"));

            Query<Long> idQuery = s.createQuery(
                    "SELECT o.productId FROM Order o WHERE o.userId = :uid " +
                            "GROUP BY o.productId ORDER BY MAX(o.orderTime) DESC, o.productId ASC", Long.class);
            idQuery.setParameter("uid", user.getId());
            idQuery.setMaxResults(n);
            List<Long> productIds = idQuery.list();
            if (productIds == null || productIds.isEmpty()) {
                return ResponseEntity.ok(new MessageResponse("No recent products"));
            }

            Query<Product> productQuery = s.createQuery(
                    "FROM Product p WHERE p.productId IN :ids ORDER BY p.productId ASC", Product.class);
            productQuery.setParameterList("ids", productIds);
            List<Product> products = productQuery.list();
            if (products == null || products.isEmpty()) {
                return ResponseEntity.ok(new MessageResponse("No recent products"));
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to fetch recent products"));
        }
    }

    // User-specific: top-N most frequently ordered products (include all statuses)
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/mine/frequent/{n}")
    public ResponseEntity<?> getMyFrequentProducts(@PathVariable int n, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Unauthorized: please log in"));
        if (n <= 0) return ResponseEntity.badRequest().body(new MessageResponse("n must be >= 1"));
        try (Session s = sessionFactory.openSession()) {
            User user = s.createQuery("from User where username = :un", User.class)
                    .setParameter("un", principal.getName())
                    .uniqueResult();
            if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Unauthorized: user not found"));

            Query<Long> idQuery = s.createQuery(
                    "SELECT o.productId FROM Order o WHERE o.userId = :uid " +
                            "GROUP BY o.productId ORDER BY COUNT(o.productId) DESC, o.productId ASC", Long.class);
            idQuery.setParameter("uid", user.getId());
            idQuery.setMaxResults(n);
            List<Long> productIds = idQuery.list();
            if (productIds == null || productIds.isEmpty()) {
                return ResponseEntity.ok(new MessageResponse("No frequent products"));
            }

            Query<Product> productQuery = s.createQuery(
                    "FROM Product p WHERE p.productId IN :ids ORDER BY p.productId ASC", Product.class);
            productQuery.setParameterList("ids", productIds);
            List<Product> products = productQuery.list();
            if (products == null || products.isEmpty()) {
                return ResponseEntity.ok(new MessageResponse("No frequent products"));
            }
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to fetch frequent products"));
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
            List<Order> orders = q.list();
            if (orders == null || orders.isEmpty()) {
                return ResponseEntity.ok(new MessageResponse("No orders found"));
            }
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to fetch orders"));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id, Principal principal) {
        try (Session s = sessionFactory.openSession()) {
            Order order = homePageService.getOrderDetail(id);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("Order not found"));
            }
            // Enforce user segregation: USER can only view own order; ADMIN can view any
            if (principal != null) {
                User currentUser = s.createQuery("from User where username = :un", User.class)
                        .setParameter("un", principal.getName())
                        .uniqueResult();
                if (currentUser == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new MessageResponse("Unauthorized"));
                }
                boolean isAdmin = "ADMIN".equals(currentUser.getRole());
                boolean isOwnOrder = order.getUserId().equals(currentUser.getId());
                if (!isAdmin && !isOwnOrder) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new MessageResponse("You cannot view other users' orders"));
                }
                // If zero quantity, cancel to ensure consistent state
                if (order.getQuantity() <= 0 && !"Canceled".equals(order.getStatus())) {
                    Transaction tx = s.beginTransaction();
                    order.setStatus("Canceled");
                    s.update(order);
                    tx.commit();
                }
                if (!isAdmin) {
                    if ("Canceled".equals(order.getStatus())) {
                        return ResponseEntity.ok(new MessageResponse("Order canceled due to zero quantity"));
                    }
                    OrderViewDto dto = new OrderViewDto();
                    dto.setId(order.getId());
                    dto.setProductId(order.getProductId());
                    dto.setQuantity(order.getQuantity());
                    dto.setStatus(order.getStatus());
                    dto.setOrderTime(order.getOrderTime());
                    dto.setProductName(order.getProductNameAtPurchase());
                    dto.setProductDescription(order.getProductDescriptionAtPurchase());
                    return ResponseEntity.ok(dto);
                }
            }
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to fetch order"));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @PatchMapping("/{id}/cancel")
    //cancel complete order: case 1: the order has been completed, but we want to cancel it;
    //cancel processing order: case2: the order has been processing, but we want to cancel it;
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Unauthorized"));
        }
        
        Session s = null;
        Transaction tx = null;
        try {
            s = sessionFactory.openSession();
            tx = s.beginTransaction();
            
            Order o = s.get(Order.class, id);
            if (o == null) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("Order not found"));
            }

            // Check user ownership first: USER can only cancel their own orders, ADMIN can cancel any
            User currentUser = s.createQuery("from User where username = :un", User.class)
                    .setParameter("un", principal.getName())
                    .uniqueResult();
            
            if (currentUser == null) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("Unauthorized"));
            }
            
            // Check if user has ADMIN role or if it's their own order
            boolean isAdmin = "ADMIN".equals(currentUser.getRole());
            boolean isOwnOrder = o.getUserId().equals(currentUser.getId());
            
            if (!isAdmin && !isOwnOrder) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("You can only cancel your own orders"));
            }

            // Check if order is already in a state that cannot be canceled
            if ("Completed".equals(o.getStatus())) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new MessageResponse("Cannot cancel a completed order"));
            }
            
            if (!"Processing".equals(o.getStatus())) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse("Cannot cancel order in state: " + o.getStatus()));
            }

            // Restore product quantity; if product was deleted, recreate it using order snapshots
            Product p = s.get(Product.class, o.getProductId());
            if (p != null) {
                p.setQuantity(p.getQuantity() + o.getQuantity());
                s.update(p);
            } else {
                Product restored = new Product();
                restored.setName(o.getProductNameAtPurchase());
                restored.setDescription(o.getProductDescriptionAtPurchase());
                restored.setRetailPrice(o.getRetailPriceAtPurchase());
                restored.setWholesalePrice(o.getWholesalePriceAtPurchase());
                restored.setQuantity(o.getQuantity());
                s.save(restored);
            }
            
            o.setStatus("Canceled");
            s.update(o);
            tx.commit();
            if (isAdmin) {
                return ResponseEntity.ok(o);
            } else {
                OrderViewDto dto = new OrderViewDto();
                dto.setId(o.getId());
                dto.setProductId(o.getProductId());
                dto.setQuantity(o.getQuantity());
                dto.setStatus(o.getStatus());
                dto.setOrderTime(o.getOrderTime());
                dto.setProductName(o.getProductNameAtPurchase());
                dto.setProductDescription(o.getProductDescriptionAtPurchase());
                return ResponseEntity.ok(dto);
            }
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
                    .body(new MessageResponse("Error canceling order"));
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

    // Admin: total number of successfully sold items (sum of quantities of Completed orders)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/sold/total")
    public ResponseEntity<?> getTotalSold() {
        try (Session s = sessionFactory.openSession()) {
            Long total = s.createQuery(
                    "SELECT COALESCE(SUM(o.quantity), 0) FROM Order o WHERE o.status = 'Completed'",
                    Long.class).uniqueResult();
            return ResponseEntity.ok(total == null ? 0 : total);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to fetch total sold"));
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
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("Order not found"));
            }

            // Check if order is already in a state that cannot be completed
            if ("Canceled".equals(o.getStatus())) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse("Cannot complete a canceled order"));
            }
            if ("Completed".equals(o.getStatus())) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new MessageResponse("Order already completed"));
            }
            if (!"Processing".equals(o.getStatus())) {
                if (tx != null && tx.isActive()) tx.rollback();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse("Cannot complete order in state: " + o.getStatus()));
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
                    .body(new MessageResponse("Error completing order"));
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