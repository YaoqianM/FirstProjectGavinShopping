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
        http
                .csrf().disable()
                .authenticationProvider(daoAuthProvider())
                .authorizeRequests()
                // public
                .antMatchers("/signup", "/login").permitAll()
                .antMatchers(HttpMethod.GET, "/products/**").permitAll()
                .antMatchers("/stats/**").hasRole("ADMIN") // stats for admin only (per your note)
                // user-only features (must be logged in as USER or ADMIN)
                .antMatchers("/watchlist/**").hasAnyRole("USER","ADMIN")
                .antMatchers(HttpMethod.POST, "/orders").hasAnyRole("USER","ADMIN")
                .antMatchers(HttpMethod.GET, "/orders/**").hasAnyRole("USER","ADMIN")
                .antMatchers(HttpMethod.PATCH, "/orders/**").hasRole("ADMIN") // admin manages order status
                // admin mgmt
                .antMatchers(HttpMethod.POST, "/products/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PATCH, "/products/**").hasRole("ADMIN")
                .antMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
                .and()
                .httpBasic(); // Postman → Authorization tab → Basic Auth

        return http.build();
    }
}
