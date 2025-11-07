package com.bfs.hibernateprojectdemo.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "users")
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private Long id;
    @JsonProperty("username")
    @Column(name = "username", unique = true, nullable=false)
    private String username;

    @Setter
    @Getter
    @JsonProperty("email")
    @Column(name = "Email", unique = true, nullable=false)
    private String Email;

    @Setter
    @Getter
    private String password;

    @Setter
    @Getter
    @Column(nullable=false)
    private String role = "USER";

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Order> orders;

    // Note: Watchlist uses userId (Long) instead of User entity, so we don't use @OneToMany mapping here
    // If you need to access watchlist items, query by userId directly


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    // Explicit getters (Lombok may not be generating them properly)
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
}
