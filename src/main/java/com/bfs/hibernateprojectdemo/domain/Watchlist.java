package com.bfs.hibernateprojectdemo.domain;

import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "watchlist")
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "user_id")
    private Long userId;

    @Setter
    @Column(name = "product_id")
    private Long productId;

    // getters and setters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getProductId() { return productId; }
}
