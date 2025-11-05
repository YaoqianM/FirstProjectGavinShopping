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
    public ResponseEntity<List<Product>> getWatchlist(@AuthenticationPrincipal User user) {
        try (Session s = sessionFactory.openSession()) {
            Query<Product> q = s.createQuery("""
          select p from Product p
          where p.id in (select w.productId from Watchlist w where w.userId = :uid)
          """, Product.class);
            q.setParameter("uid", user.getId());
            return ResponseEntity.ok(q.list());
        }
    }

//    @PostMapping("/product/{id}")
//    public ResponseEntity<Void> addToWatchlist(@AuthenticationPrincipal User user, @PathVariable Long id) {
//        try (Session s = sessionFactory.openSession()) {
//            Transaction tx = s.beginTransaction();
//            Watchlist w = new Watchlist(); w.setUserId(user.getId()); w.setProductId(id);
//            s.save(w); tx.commit();
//            return ResponseEntity.status(201).build();
//        }
//    }
//    @PostMapping("/product/{id}")
//    public ResponseEntity<Void> addToWatchlist(@PathVariable("id") int id,
//                                               @AuthenticationPrincipal User user) {
//        Session session = sessionFactory.getCurrentSession();
//        Product product = session.get(Product.class, id);
//        if (product == null) return ResponseEntity.notFound().build();
//
//        Watchlist watchlist = new Watchlist();
//        watchlist.setUserId(user.getId());
//        watchlist.setProductId(product.getProductId());
//        session.persist(watchlist);
//        return ResponseEntity.ok().build();
//    }

    @PostMapping("/product/{id}")
    public ResponseEntity<Void> addToWatchlist(@PathVariable("id") int id,
                                               @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {
        Session session = sessionFactory.getCurrentSession();
        Product product = session.get(Product.class, id);
        if (product == null) return ResponseEntity.notFound().build();

        // fetch user entity from DB using username
        Query<User> query = session.createQuery("from User u where u.username = :username", User.class);
        query.setParameter("username", userDetails.getUsername());
        User dbUser = query.uniqueResult();
        if (dbUser == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        Watchlist watchlist = new Watchlist();
        watchlist.setUserId(dbUser.getId());
        watchlist.setProductId(product.getProductId());
        session.persist(watchlist);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/product/{id}")
    public ResponseEntity<Void> removeFromWatchlist(@AuthenticationPrincipal User user, @PathVariable Long id) {
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            s.createQuery("delete from Watchlist where userId=:uid and productId=:pid")
                    .setParameter("uid", user.getId()).setParameter("pid", id).executeUpdate();
            tx.commit();
            return ResponseEntity.noContent().build();
        }
    }

}
