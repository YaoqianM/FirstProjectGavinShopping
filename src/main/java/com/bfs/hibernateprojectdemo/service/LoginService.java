package com.bfs.hibernateprojectdemo.service;

import com.bfs.hibernateprojectdemo.domain.User;
import com.bfs.hibernateprojectdemo.exception.InvalidCredentialsException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    @Autowired
    private SessionFactory sessionFactory;

    public User authenticate(User user) {
        if (user == null || user.getUsername() == null || user.getPassword() == null) {
            throw new InvalidCredentialsException();
        }
        try (Session s = sessionFactory.openSession()) {
            User db = s.createQuery("from User u where u.username = :username", User.class)
                    .setParameter("username", user.getUsername().trim())
                    .uniqueResult();
            if (db == null || !BCrypt.checkpw(user.getPassword(), db.getPassword())) {
                throw new InvalidCredentialsException();
            }
            return db; // return user; token/JWT can be added later
        }
    }

    ////
    public User findByUsername(String username) {
        try (var s = sessionFactory.openSession()) {
            return s.createQuery("from User u where u.username=:un", User.class)
                    .setParameter("un", username.trim())
                    .uniqueResult();
        }
    }

    public boolean verifyPassword(User dbUser, String rawPw) {
        return dbUser != null && BCrypt.checkpw(rawPw, dbUser.getPassword());
    }
}
