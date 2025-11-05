package com.bfs.hibernateprojectdemo.domain;

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

}