package com.bfs.hibernateprojectdemo.service;

import com.bfs.hibernateprojectdemo.domain.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.hibernate.query.Query;


@Service
public class LoginService {
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean authenticate(String userName, String rawPassword) {
        try (Session session = sessionFactory.openSession()) {
            Query<User> query = session.createQuery(
                    "from User u where u.username = :userName",
                    User.class);
            query.setParameter("userName", userName);
            User dbUser = query.uniqueResult();
            return dbUser != null && passwordEncoder.matches(rawPassword, dbUser.getPassword());
        }
    }
}