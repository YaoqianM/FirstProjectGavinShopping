package com.bfs.hibernateprojectdemo.service;

import com.bfs.hibernateprojectdemo.domain.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.hibernate.query.Query;

import javax.transaction.Transactional;


@Service
public class RegisterService {
    @Autowired
    private SessionFactory sessionFactory;
    @Transactional
    public User registerUser(User user) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            String hql =
                    "from User u where u.userName = :userName or u.Email = :Email";
            Query<User> query = session.createQuery(hql, User.class);
            query.setParameter("userName", user.getUserName());
            query.setParameter("Email", user.getEmail());

            User existingUser = query.uniqueResult();

            if (existingUser != null) {
                tx.rollback();
                return null;
            }

            session.persist(user);
            tx.commit();
            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
