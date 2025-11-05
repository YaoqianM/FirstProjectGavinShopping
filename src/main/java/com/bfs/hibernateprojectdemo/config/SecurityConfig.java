// src/main/java/com/bfs/hibernateprojectdemo/config/SecurityConfig.java
package com.bfs.hibernateprojectdemo.config;

import com.bfs.hibernateprojectdemo.security.DbUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final DbUserDetailsService userDetailsService;
    public SecurityConfig(DbUserDetailsService uds) { this.userDetailsService = uds; }

    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean public DaoAuthenticationProvider daoAuthProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests()
                .antMatchers("/signup","/login").permitAll()
                //.antMatchers(HttpMethod.POST,"/products/**", HttpMethod.PATCH,"/products/**").hasRole("ADMIN")
                .antMatchers("/products/profit/**","/products/popular/**").hasRole("ADMIN")
                .antMatchers("/products/frequent/**","/products/recent/**").hasRole("USER")
                .antMatchers("/watchlist/**").hasRole("USER")
                .antMatchers(HttpMethod.POST,"/orders/**").hasRole("USER")
                .antMatchers(HttpMethod.PATCH,"/orders/**/complete").hasRole("ADMIN")
                .antMatchers("/orders/**").hasAnyRole("ADMIN","USER")
                .anyRequest().denyAll();
        return http.build();
//        http.authorizeHttpRequests()
//                .antMatchers("/signup", "/login").permitAll()
//                .antMatchers("/product/**", "/order/**", "/watchlist/**").authenticated()
//                .anyRequest().denyAll();
//        return http.build();
    }
}
