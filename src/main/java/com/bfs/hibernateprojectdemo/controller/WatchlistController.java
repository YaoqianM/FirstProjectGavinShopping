//package com.bfs.hibernateprojectdemo.controller;
//
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/watchlist")
//public class WatchlistController {
//
//    @GetMapping("/products/all")
//    public String getWatchlist() {
//        // user list watchlist
//        return "Watchlist products";
//    }
//
//    @PostMapping("/product/{id}")
//    public String addToWatchlist(@PathVariable Long id) {
//        // user add to watchlist
//        return "Added to watchlist";
//    }
//
//    @DeleteMapping("/product/{id}")
//    public String removeFromWatchlist(@PathVariable Long id) {
//        // user remove from watchlist
//        return "Removed from watchlist";
//    }
//
//}


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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/watchlist")
public class WatchlistController {
    private final SessionFactory sessionFactory;
    @Autowired
    public WatchlistController(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    @GetMapping("/products/all")
    public ResponseEntity<List<Product>> getWatchlist(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {

        if (userDetails == null) return ResponseEntity.status(401).build();
        String uname = userDetails.getUsername();
        try (Session s = sessionFactory.openSession()) {
            User dbUser = s.createQuery("from User u where u.username = :un", User.class)
                    .setParameter("un", userDetails.getUsername())
                    .uniqueResult();
            if (dbUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            Query<Product> q = s.createQuery(
                    "select p from Product p where p.productId in (" +
                            " select w.productId from Watchlist w where w.userId = :uid" +
                            " ) and p.quantity > 0",
                    Product.class);
            q.setParameter("uid", dbUser.getId());
            return ResponseEntity.ok(q.list());
        }
    }

    // Adds a product to the user’s watchlist
    @PostMapping("/product/{id}")
    public ResponseEntity<Void> addToWatchlist(@PathVariable("id") Long productId,
                                               @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {

        if (userDetails == null) return ResponseEntity.status(401).build();
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();

            User dbUser = s.createQuery("from User u where u.username = :un", User.class)
                    .setParameter("un", userDetails.getUsername())
                    .uniqueResult();
            if (dbUser == null) {
                tx.rollback();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Product product = s.get(Product.class, productId);
            if (product == null) {
                tx.rollback();
                return ResponseEntity.notFound().build();
            }

            // Avoid duplicates
            Watchlist existing = s.createQuery(
                            "from Watchlist w where w.userId = :uid and w.productId = :pid",
                            Watchlist.class)
                    .setParameter("uid", dbUser.getId())
                    .setParameter("pid", productId)
                    .uniqueResult();
            if (existing == null) {
                Watchlist w = new Watchlist();
                w.setUserId(dbUser.getId());
                w.setProductId(productId);
                s.persist(w);
            }

            tx.commit();
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
    }

    // Removes a product from the user’s watchlist
    @DeleteMapping("/product/{id}")
    public ResponseEntity<Void> removeFromWatchlist(@PathVariable("id") Long productId,
                                                    @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {

        if (userDetails == null) return ResponseEntity.status(401).build();
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();

            User dbUser = s.createQuery("from User u where u.username = :un", User.class)
                    .setParameter("un", userDetails.getUsername())
                    .uniqueResult();
            if (dbUser == null) {
                tx.rollback();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            int affected = s.createQuery(
                            "delete from Watchlist where userId = :uid and productId = :pid")
                    .setParameter("uid", dbUser.getId())
                    .setParameter("pid", productId)
                    .executeUpdate();

            tx.commit();
            return affected > 0 ? ResponseEntity.noContent().build() :
                    ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
    }
}

