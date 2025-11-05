package com.bfs.hibernateprojectdemo.service;

import com.bfs.hibernateprojectdemo.domain.Product;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductAnalyticsService {

    @Autowired
    private SessionFactory sessionFactory;

    // Top-N most frequently purchased (exclude canceled)
    public List<Product> getTopFrequent(int n) {
        try (Session session = sessionFactory.openSession()) {
            // Get product IDs ordered by frequency
            Query<Long> idQuery = session.createQuery(
                    "SELECT o.productId FROM Order o " +
                    "WHERE o.status = 'Completed' " +
                    "GROUP BY o.productId " +
                    "ORDER BY COUNT(o.productId) DESC, o.productId ASC", Long.class);
            idQuery.setMaxResults(n);
            List<Long> productIds = idQuery.list();
            
            if (productIds.isEmpty()) {
                return new java.util.ArrayList<>();
            }
            
            // Fetch products by IDs
            Query<Product> productQuery = session.createQuery(
                    "FROM Product p WHERE p.productId IN :ids ORDER BY p.productId ASC", Product.class);
            productQuery.setParameterList("ids", productIds);
            return productQuery.list();
        }
    }

    // Top-N most recent (exclude canceled)
    public List<Product> getTopRecent(int n) {
        try (Session session = sessionFactory.openSession()) {
            // Get product IDs ordered by most recent order time
            Query<Long> idQuery = session.createQuery(
                    "SELECT o.productId FROM Order o " +
                    "WHERE o.status = 'Completed' " +
                    "GROUP BY o.productId " +
                    "ORDER BY MAX(o.orderTime) DESC, o.productId ASC", Long.class);
            idQuery.setMaxResults(n);
            List<Long> productIds = idQuery.list();
            
            if (productIds.isEmpty()) {
                return new java.util.ArrayList<>();
            }
            
            // Fetch products by IDs
            Query<Product> productQuery = session.createQuery(
                    "FROM Product p WHERE p.productId IN :ids ORDER BY p.productId ASC", Product.class);
            productQuery.setParameterList("ids", productIds);
            return productQuery.list();
        }
    }

    // Top-N most profitable (exclude canceled/ongoing)
    public List<Product> getTopProfit(int n) {
        try (Session session = sessionFactory.openSession()) {
            // Use native SQL for complex profit calculation
            String sql = 
                    "SELECT p.product_id FROM products p " +
                    "JOIN orders o ON p.product_id = o.product_id " +
                    "WHERE o.status = 'Completed' " +
                    "GROUP BY p.product_id " +
                    "ORDER BY SUM((p.retail_price - p.wholesale_price) * o.quantity) DESC, p.product_id ASC " +
                    "LIMIT ?";
            
            @SuppressWarnings("unchecked")
            List<Long> productIds = session.createNativeQuery(sql)
                    .setParameter(1, n)
                    .getResultList();
            
            if (productIds.isEmpty()) {
                return new java.util.ArrayList<>();
            }
            
            // Fetch products by IDs
            Query<Product> productQuery = session.createQuery(
                    "FROM Product p WHERE p.productId IN :ids ORDER BY p.productId ASC", Product.class);
            productQuery.setParameterList("ids", productIds);
            return productQuery.list();
        }
    }

    // Top-N by units sold (exclude canceled/ongoing)
    public List<Product> getTopPopular(int n) {
        try (Session session = sessionFactory.openSession()) {
            // Get product IDs ordered by total quantity sold
            Query<Long> idQuery = session.createQuery(
                    "SELECT o.productId FROM Order o " +
                    "WHERE o.status = 'Completed' " +
                    "GROUP BY o.productId " +
                    "ORDER BY SUM(o.quantity) DESC, o.productId ASC", Long.class);
            idQuery.setMaxResults(n);
            List<Long> productIds = idQuery.list();
            
            if (productIds.isEmpty()) {
                return new java.util.ArrayList<>();
            }
            
            // Fetch products by IDs
            Query<Product> productQuery = session.createQuery(
                    "FROM Product p WHERE p.productId IN :ids ORDER BY p.productId ASC", Product.class);
            productQuery.setParameterList("ids", productIds);
            return productQuery.list();
        }
    }
}