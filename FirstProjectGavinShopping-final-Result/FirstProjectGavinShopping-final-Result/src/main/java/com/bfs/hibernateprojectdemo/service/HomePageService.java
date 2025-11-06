package com.bfs.hibernateprojectdemo.service;

import com.bfs.hibernateprojectdemo.domain.Order;
import com.bfs.hibernateprojectdemo.domain.Product;
import com.bfs.hibernateprojectdemo.domain.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HomePageService {
    @Autowired
    private SessionFactory sessionFactory;
    public List<Product> getAvailableProducts(boolean isAdmin) {
        try (Session session = sessionFactory.openSession()) {
            String hql = isAdmin
                    ? "FROM Product" // admin sees all
                    : "FROM Product p WHERE p.quantity > 0"; // user hides OOS
            Query<Product> query = session.createQuery(hql, Product.class);
            return query.list();
        }
    }

    public Product getProductDetail(Long productId, boolean isAdmin) {
        try (Session session = sessionFactory.openSession()) {
            Product product = session.get(Product.class, productId);
            if (product != null && !isAdmin) {
                product.setQuantity(0); // hide quantity for user
            }
            return product;
        }
    }

    public Order getOrderDetail(Long orderId) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Order.class, orderId);
        }
    }
}