package com.bfs.hibernateprojectdemo.service;

import com.bfs.hibernateprojectdemo.domain.Product;
import com.bfs.hibernateprojectdemo.domain.Watchlist;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WatchlistService {
    @Autowired
    private SessionFactory sessionFactory;

    public boolean addToWatchlist(Long userId, Long productId) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();

            Product product = session.get(Product.class, productId);
            if (product == null) {
                tx.rollback();
                throw new IllegalArgumentException("Product not found: " + productId);
            }

            Watchlist entry = new Watchlist();
            entry.setUserId(userId);
            entry.setProductId(productId);

            session.persist(entry);
            tx.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Remove product from user's Watchlist
    public boolean removeFromWatchlist(Long userId, Long productId) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();

            String hql = "FROM Watchlist w WHERE w.userId = :userId AND w.productId = :productId";
            Query<Watchlist> query = session.createQuery(hql, Watchlist.class);
            query.setParameter("userId", userId);
            query.setParameter("productId", productId);

            Watchlist entry = query.uniqueResult();
            if (entry != null) {
                session.remove(entry);
                tx.commit();
                return true;
            }

            tx.rollback();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // View all in-stock products in user's Watchlist
    public List<Product> getInStockWatchlist(Long userId) {
        try (Session session = sessionFactory.openSession()) {
            String hql = """
                SELECT p FROM Product p
                WHERE p.id IN (
                    SELECT w.productId FROM Watchlist w WHERE w.userId = :userId
                )
                AND p.quantity > 0
                """;
            Query<Product> query = session.createQuery(hql, Product.class);
            query.setParameter("userId", userId);
            return query.list();
        }
    }
}
