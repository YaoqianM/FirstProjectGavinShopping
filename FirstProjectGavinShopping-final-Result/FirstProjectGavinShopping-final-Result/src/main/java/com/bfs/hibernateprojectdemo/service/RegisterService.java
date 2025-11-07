package com.bfs.hibernateprojectdemo.service;

import com.bfs.hibernateprojectdemo.domain.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;


@Service
public class RegisterService {
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(User user) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            // Use JPA Criteria API to prevent SQL injection and avoid string HQL
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<User> cq = cb.createQuery(User.class);
            Root<User> root = cq.from(User.class);
            Predicate usernameEquals = cb.equal(root.get("username"), user.getUsername());
            Predicate emailEquals = cb.equal(root.get("Email"), user.getEmail());
            cq.select(root).where(cb.or(usernameEquals, emailEquals));
            Query<User> dupQuery = session.createQuery(cq);
            User existing = dupQuery.uniqueResult();

            if (existing != null) {
                tx.rollback();
                return null;
            }

            user.setPassword(passwordEncoder.encode(user.getPassword()));
            if (user.getRole() == null || user.getRole().isBlank()) user.setRole("USER");
            session.persist(user);
            tx.commit();
            return user;
        }
    }

    /** Seeds the admin account with a hashed password and unique email. */
    @Transactional
    public void initAdminUser() {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();

            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<User> cq = cb.createQuery(User.class);
            Root<User> root = cq.from(User.class);
            cq.select(root).where(cb.equal(root.get("username"), "admin"));
            User existingAdmin = session.createQuery(cq).uniqueResult();

            if (existingAdmin == null) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@shop.com");
                admin.setPassword(passwordEncoder.encode("123"));
                admin.setRole("ADMIN");
                session.persist(admin);
            }
            tx.commit();
        }
    }
}
