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
    @Column(name = "username", unique = true)
    private String username;

    @Setter
    @Getter
    @JsonProperty("email")
    @Column(name = "Email", unique = true)
    private String Email;

    @Setter
    @Getter
    private String password;

    public String getUserName() {
        return username;
    }

    public void setUserName(String username) {
        this.username = username;
    }
}
