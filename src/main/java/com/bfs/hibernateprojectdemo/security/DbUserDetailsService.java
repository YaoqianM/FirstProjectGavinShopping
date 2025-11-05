// src/main/java/com/bfs/hibernateprojectdemo/security/DbUserDetailsService.java
package com.bfs.hibernateprojectdemo.security;

import com.bfs.hibernateprojectdemo.domain.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class DbUserDetailsService implements UserDetailsService {
    private final SessionFactory sessionFactory;
    public DbUserDetailsService(SessionFactory sf) { this.sessionFactory = sf; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try (Session s = sessionFactory.openSession()) {
            User u = s.createQuery("from User u where u.username = :un", User.class)
                    .setParameter("un", username).uniqueResult();
            if (u == null) throw new UsernameNotFoundException("User not found");
            GrantedAuthority auth = new SimpleGrantedAuthority("ROLE_" + u.getRole());
            return org.springframework.security.core.userdetails.User
                    .withUsername(u.getUserName())
                    .password(u.getPassword())
                    .authorities(Collections.singleton(auth))
                    .build();
        }
    }
}
