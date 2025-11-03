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
    public boolean registerUser(String userName, String password, String Email) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            String hql = "from User u where u.userName = :userName or u.Email = :Email";
            Query<User> query = session.createQuery(hql, User.class);
            query.setParameter("userName", userName);
            query.setParameter("Email", Email);

            User existingUser = query.uniqueResult();

            if (existingUser != null) {
                tx.rollback();
                return false;
            }
            User newUser = new User();
            newUser.setUserName(userName);
            newUser.setEmail(Email);
            newUser.setPassword(password);

            session.persist(newUser);
            tx.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
