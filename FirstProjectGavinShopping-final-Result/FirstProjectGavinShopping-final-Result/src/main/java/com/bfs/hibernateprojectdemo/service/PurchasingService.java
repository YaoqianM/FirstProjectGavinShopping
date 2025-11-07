package com.bfs.hibernateprojectdemo.service;

import com.bfs.hibernateprojectdemo.domain.Order;
import com.bfs.hibernateprojectdemo.domain.Product;
import com.bfs.hibernateprojectdemo.exception.NotEnoughInventoryException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PurchasingService {
    @Autowired
    private SessionFactory sessionFactory;

    public boolean purchaseProduct(Long userId, Long productId, int quantity) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();

            Product product = session.get(Product.class, productId);
            if (product == null) {
                tx.rollback();
                throw new IllegalArgumentException("Product not found: " + productId);
            }

            if (quantity > product.getQuantity()) {
                tx.rollback();
                throw new NotEnoughInventoryException("Not enough stock for product: " + product.getName());
            }

            // Deduct stock and delete if non-positive
            int newQty = product.getQuantity() - quantity;
            if (newQty <= 0) {
                session.delete(product);
            } else {
                product.setQuantity(newQty);
                session.update(product);
            }

            // Create order
            Order order = new Order();
            order.setUserId(userId);
            order.setProductId(productId);
            order.setQuantity(quantity);
            order.setStatus("Processing");
            order.setOrderTime(LocalDateTime.now());
            // Snapshot product name and description at purchase time
            order.setProductNameAtPurchase(product.getName());
            order.setProductDescriptionAtPurchase(product.getDescription());

            session.persist(order);
            tx.commit();
            return true;
        } catch (NotEnoughInventoryException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Cancel order (restock items)
    public boolean cancelOrder(Long orderId) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();

            Order order = session.get(Order.class, orderId);
            if (order == null || "Completed".equals(order.getStatus())) {
                tx.rollback();
                return false;
            }

            if ("Processing".equals(order.getStatus())) {
                order.setStatus("Canceled");

                Product product = session.get(Product.class, order.getProductId());
                product.setQuantity(product.getQuantity() + order.getQuantity());
                session.update(product);

                session.update(order);
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

}
