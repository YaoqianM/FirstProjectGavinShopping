package com.bfs.hibernateprojectdemo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Getters and setters
    @Setter
    @Getter
    @Column(name = "user_id")
    private Long userId;
    @Setter
    @Getter
    private Long productId;
    @Getter
    @Setter
    private int quantity;
    @Setter
    @Getter
    private String status;
    @Setter
    @Getter
    private LocalDateTime orderTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore // Don't serialize the full complex User object if you already have userId above
    private User user;
}