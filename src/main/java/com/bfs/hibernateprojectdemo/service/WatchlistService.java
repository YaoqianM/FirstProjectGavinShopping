package com.bfs.hibernateprojectdemo.service;

import com.bfs.hibernateprojectdemo.domain.Product;
import com.bfs.hibernateprojectdemo.domain.WatchList;
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

    public boolean addToWatchList(Long userId, Long productId) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();

            Product product = session.get(Product.class, productId);
            if (product == null) {
                tx.rollback();
                throw new IllegalArgumentException("Product not found: " + productId);
            }

            WatchList entry = new WatchList();
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

    // Remove product from user's watchlist
    public boolean removeFromWatchList(Long userId, Long productId) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();

            String hql = "FROM WatchList w WHERE w.userId = :userId AND w.productId = :productId";
            Query<WatchList> query = session.createQuery(hql, WatchList.class);
            query.setParameter("userId", userId);
            query.setParameter("productId", productId);

            WatchList entry = query.uniqueResult();
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

    // View all in-stock products in user's watchlist
    public List<Product> getInStockWatchList(Long userId) {
        try (Session session = sessionFactory.openSession()) {
            String hql = """
                SELECT p FROM Product p
                WHERE p.id IN (
                    SELECT w.productId FROM WatchList w WHERE w.userId = :userId
                )
                AND p.quantity > 0
                """;
            Query<Product> query = session.createQuery(hql, Product.class);
            query.setParameter("userId", userId);
            return query.list();
        }
    }
}
