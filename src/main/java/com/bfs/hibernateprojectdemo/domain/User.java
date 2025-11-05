package com.bfs.hibernateprojectdemo.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    @Id
    private Long id;
    @JsonProperty("username")
    @Setter
    @Getter
    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Setter
    @Getter
    @JsonProperty("email")
    @Column(name = "Email", unique = true)
    private String Email;

    @Setter
    @Getter
    @Column(nullable = false)
    private String password;

    @Setter
    @Getter
    @Column(name = "role", nullable = false)
    private String role = "USER"; // or "ADMIN"
}
