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

    // Top-N most frequently ordered product IDs (all statuses, global)
    public List<Long> getTopFrequent(int n) {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> idQuery = session.createQuery(
                    "SELECT o.productId FROM Order o " +
                    "GROUP BY o.productId " +
                    "ORDER BY COUNT(o.productId) DESC, o.productId ASC", Long.class);
            idQuery.setMaxResults(n);
            List<Long> productIds = idQuery.list();
            return productIds != null ? productIds : new java.util.ArrayList<>();
        }
    }

    // Top-N most recent product IDs by latest order time (all statuses, global)
    public List<Long> getTopRecent(int n) {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> idQuery = session.createQuery(
                    "SELECT o.productId FROM Order o " +
                    "GROUP BY o.productId " +
                    "ORDER BY MAX(o.orderTime) DESC, o.productId ASC", Long.class);
            idQuery.setMaxResults(n);
            List<Long> productIds = idQuery.list();
            return productIds != null ? productIds : new java.util.ArrayList<>();
        }
    }

    // Top-N most profitable based purely on current product table
    // Profit = (retailPrice - wholesalePrice) * quantity
    public List<Product> getTopProfit(int n) {
        try (Session session = sessionFactory.openSession()) {
            Query<Product> q = session.createQuery("FROM Product", Product.class);
            List<Product> all = q.list();
            if (all == null || all.isEmpty()) return new java.util.ArrayList<>();
            all.sort((a, b) -> {
                double pa = (a.getRetailPrice() - a.getWholesalePrice()) * a.getQuantity();
                double pb = (b.getRetailPrice() - b.getWholesalePrice()) * b.getQuantity();
                int cmp = Double.compare(pb, pa);
                if (cmp != 0) return cmp;
                return Long.compare(a.getProductId(), b.getProductId());
            });
            if (n < all.size()) {
                return all.subList(0, n);
            }
            return all;
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    // Top-N by units ordered (all statuses, global)
    public List<Product> getTopPopular(int n) {
        try (Session session = sessionFactory.openSession()) {
            // Get product IDs ordered by total quantity
            Query<Long> idQuery = session.createQuery(
                    "SELECT o.productId FROM Order o " +
                    "GROUP BY o.productId " +
                    "ORDER BY SUM(o.quantity) DESC, o.productId ASC", Long.class);
            idQuery.setMaxResults(n);
            List<Long> productIds = idQuery.list();
            
            if (productIds == null || productIds.isEmpty()) {
                return new java.util.ArrayList<>();
            }
            
            // Fetch products by IDs
            Query<Product> productQuery = session.createQuery(
                    "FROM Product p WHERE p.productId IN :ids ORDER BY p.productId ASC", Product.class);
            productQuery.setParameterList("ids", productIds);
            List<Product> products = productQuery.list();
            return products != null ? products : new java.util.ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
}