package com.bfs.hibernateprojectdemo.service;

import com.bfs.hibernateprojectdemo.domain.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    @Autowired
    private SessionFactory sessionFactory;

    public boolean authenticate(User user) {
        if (user == null || user.getUserName() == null || user.getPassword() == null) return false;
        String un = user.getUserName().trim();
        String pw = user.getPassword();

        try (Session session = sessionFactory.openSession()) {
            // fetch by username only
            User dbUser = session.createQuery(
                            "from User u where u.username = :username", User.class)
                    .setParameter("username", un)
                    .uniqueResult();
            if (dbUser == null) return false;

            // verify hash (assumes RegisterService stored BCrypt hash)
            return BCrypt.checkpw(pw, dbUser.getPassword());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
