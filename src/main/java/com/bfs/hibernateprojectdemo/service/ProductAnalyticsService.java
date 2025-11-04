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
            String hql = """
                SELECT p FROM Product p
                WHERE p.id IN (
                    SELECT o.productId FROM Order o
                    WHERE o.status != 'Canceled'
                    GROUP BY o.productId
                    ORDER BY COUNT(o.productId) DESC, o.productId ASC
                )
                """;
            Query<Product> query = session.createQuery(hql, Product.class);
            query.setMaxResults(n);
            return query.list();
        }
    }

    // Top-N most recent (exclude canceled)
    public List<Product> getTopRecent(int n) {
        try (Session session = sessionFactory.openSession()) {
            String hql = """
                SELECT p FROM Product p
                WHERE p.id IN (
                    SELECT o.productId FROM Order o
                    WHERE o.status != 'Canceled'
                    ORDER BY o.orderTime DESC, o.productId ASC
                )
                """;
            Query<Product> query = session.createQuery(hql, Product.class);
            query.setMaxResults(n);
            return query.list();
        }
    }

    // Top-N most profitable (exclude canceled/ongoing)
    public List<Product> getTopProfit(int n) {
        try (Session session = sessionFactory.openSession()) {
            String hql = """
                SELECT p FROM Product p
                WHERE p.id IN (
                    SELECT o.productId FROM Order o
                    WHERE o.status = 'Completed'
                    GROUP BY o.productId
                    ORDER BY SUM(p.retailPrice - p.wholesalePrice) DESC, o.productId ASC
                )
                """;
            Query<Product> query = session.createQuery(hql, Product.class);
            query.setMaxResults(n);
            return query.list();
        }
    }

    // Top-N by units sold (exclude canceled/ongoing)
    public List<Product> getTopPopular(int n) {
        try (Session session = sessionFactory.openSession()) {
            String hql = """
                SELECT p FROM Product p
                WHERE p.id IN (
                    SELECT o.productId FROM Order o
                    WHERE o.status = 'Completed'
                    GROUP BY o.productId
                    ORDER BY SUM(o.quantity) DESC, o.productId ASC
                )
                """;
            Query<Product> query = session.createQuery(hql, Product.class);
            query.setMaxResults(n);
            return query.list();
        }
    }
}