package com.bfs.hibernateprojectdemo.service;

import com.bfs.hibernateprojectdemo.domain.User;
import com.bfs.hibernateprojectdemo.exception.DuplicateUserException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;


@Service
public class RegisterService {
    @Autowired
    private SessionFactory sessionFactory;
    @Transactional
    public User registerUser(User user) {
        if (user == null || user.getUsername() == null || user.getEmail() == null || user.getPassword() == null) {
            throw new IllegalArgumentException("username, email, password required");
        }
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            Long dupCount = session.createQuery(
                            "select count(u) from User u where u.username = :username or u.Email = :Email", Long.class)
                    .setParameter("username", user.getUsername().trim())
                    .setParameter("Email", user.getEmail().trim())
                    .uniqueResult();

            if (dupCount != null && dupCount > 0) {
                tx.rollback();
                throw new DuplicateUserException("Username or email already exists");
            }
            user.setUsername(user.getUsername().trim());
            user.setEmail(user.getEmail().trim());
            user.setPassword(org.springframework.security.crypto.bcrypt.BCrypt.hashpw(
                    user.getPassword(), org.springframework.security.crypto.bcrypt.BCrypt.gensalt()));


            session.save(user);
            tx.commit();
            return user;
        }
    }
    @Transactional
    public void initAdminUser() {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();

            String hql = "from User u where u.username = :username";
            Query<User> query = session.createQuery(hql, User.class);
            query.setParameter("username", "admin");

            User existingAdmin = query.uniqueResult();
            if (existingAdmin == null) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword("123");
                session.persist(admin);
            }

            tx.commit();
        }
    }
}
