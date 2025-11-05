package com.bfs.hibernateprojectdemo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.*;

@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Map to DB column userName (capital N)
    @JsonProperty("username")
    @Column(name = "userName", unique = true, nullable = false)
    private String username;

    @JsonProperty("email")
    @Column(name = "Email", unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password; // will store BCrypt

    @Column(nullable = false)
    private String role = "USER"; // USER or ADMIN

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
