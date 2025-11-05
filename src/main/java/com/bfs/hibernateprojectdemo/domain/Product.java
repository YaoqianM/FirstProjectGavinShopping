package com.bfs.hibernateprojectdemo.domain;

import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Setters
    @Setter
    private String name;
    @Setter
    private String description;
    @Setter
    private double retailPrice;
    @Setter
    private int quantity;
    @Setter
    private double wholesalePrice;

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getRetailPrice() {
        return retailPrice;
    }

    public int getQuantity() {
        return quantity;
    }
    public double getWholesalePrice() { return wholesalePrice; }

}