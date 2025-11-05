package com.bfs.hibernateprojectdemo.service;

import com.bfs.hibernateprojectdemo.domain.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    @Autowired
    private SessionFactory sessionFactory;

    public boolean authenticate(User user) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "from User u where u.userName = :userName and u.password = :password";
            Query<User> query = session.createQuery(hql, User.class);
            query.setParameter("userName", user.getUserName());
            query.setParameter("password", user.getPassword());
            User foundUser = query.uniqueResult();
            return foundUser != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
