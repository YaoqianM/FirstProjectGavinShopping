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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {
    static class OrderItem { public Long productId; public int quantity; }
    static class OrderRequest { public List<OrderItem> order; }
    private final SessionFactory sessionFactory;

    public OrderController(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    @Autowired
    private HomePageService homePageService;

    @PostMapping
    public ResponseEntity<Void> placeOrder(@RequestBody OrderRequest req,
                                           @AuthenticationPrincipal User current) {
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            for (OrderItem item : req.order) {
                Product p = s.get(Product.class, item.productId);
                if (p == null || p.getQuantity() < item.quantity) {
                    tx.rollback();
                    return ResponseEntity.badRequest().build();
                }
                p.setQuantity(p.getQuantity() - item.quantity);

                Order o = new Order();
                o.setUserId(current.getId());
                o.setProductId(item.productId);
                o.setQuantity(item.quantity);
                o.setStatus("Processing");
                o.setOrderTime(LocalDateTime.now());

                s.save(o);
                s.update(p);
            }
            tx.commit();
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
    }

//    @PostMapping
//    public ResponseEntity<Void> placeOrder(@RequestBody List<Order> orders,
//                                           @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {
//        Transaction tx = null;
//        try (Session session = sessionFactory.openSession()) {
//            tx = session.beginTransaction();
//
//            Query<User> query = session.createQuery("from User u where u.username = :username", User.class);
//            query.setParameter("username", userDetails.getUsername());
//            User dbUser = query.uniqueResult();
//            if (dbUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//
//            for (Order order : orders) {
//                Product product = session.get(Product.class, order.getProductId());
//                if (product == null) continue;
//
//                order.setUserId(dbUser.getId());
//                order.setStatus("Processing");
//                order.setOrderTime(LocalDateTime.now());
//
//                session.persist(order);
//            }
//
//            tx.commit();
//            return ResponseEntity.status(HttpStatus.CREATED).build();
//        } catch (Exception e) {
//            if (tx != null) tx.rollback();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
    @GetMapping("/all")
    public ResponseEntity<List<Order>> getAllOrders(@RequestParam(defaultValue = "0") int page) {
        try (Session s = sessionFactory.openSession()) {
            Query<Order> q = s.createQuery("from Order o order by o.orderTime desc", Order.class);
            q.setFirstResult(page * 5);
            q.setMaxResults(5);
            return ResponseEntity.ok(q.list());
        }
    }

    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Long id) {
        // user/admin order detail
        return homePageService.getOrderDetail(id);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();

            Order o = s.get(Order.class, id);
            if (o == null) {
                tx.rollback();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
            }

            switch (o.getStatus()) {
                case "Completed":
                    tx.rollback();
                    return ResponseEntity.badRequest().body("Completed order cannot be canceled");
                case "Canceled":
                    tx.rollback();
                    return ResponseEntity.badRequest().body("Order already canceled");
                case "Processing":
                    // restock
                    Product p = s.get(Product.class, o.getProductId());
                    if (p != null) {
                        p.setQuantity(p.getQuantity() + o.getQuantity());
                        s.update(p);
                    }
                    o.setStatus("Canceled");
                    s.update(o);
                    tx.commit();
                    return ResponseEntity.ok(o);
                default:
                    tx.rollback();
                    return ResponseEntity.badRequest().body("Invalid order status");
            }
        }
    }

    @PatchMapping("/{id}/complete")
    public String completeOrder(@PathVariable Long id) {
        // complete processing order
        return "Order completed";
    }
}