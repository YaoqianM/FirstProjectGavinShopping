package com.bfs.hibernateprojectdemo.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

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

    public String getUserName() {
        return username;
    }

    public void setUserName(String username) {
        this.username = username;
    }
}
