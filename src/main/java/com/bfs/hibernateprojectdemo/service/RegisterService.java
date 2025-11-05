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

            Query<User> query = session.createQuery(
                    "from User u where u.username = :username or u.Email = :Email",
                    User.class);
            query.setParameter("username", user.getUserName());
            query.setParameter("Email", user.getEmail());

            if (query.uniqueResult() != null) {
                tx.rollback();
                return null;
            }

            user.setPassword(passwordEncoder.encode(user.getPassword()));
            if (user.getRole() == null || user.getRole().isBlank()) user.setRole("USER");
            session.persist(user);

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

            User existingAdmin = session.createQuery(
                            "from User u where u.username = :username",
                            User.class)
                    .setParameter("username", "admin")
                    .uniqueResult();

            if (existingAdmin == null) {
                User admin = new User();
                admin.setUserName("admin");
                admin.setEmail("admin@shop.com");
                admin.setPassword(passwordEncoder.encode("123"));
                admin.setRole("ADMIN");
                session.persist(admin);
            }
            tx.commit();
        }
    }
}
