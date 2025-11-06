package com.bfs.hibernateprojectdemo.domain;

import javax.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Version
    private Long version;

    // Fields
    private String name;
    private String description;
    private double retailPrice;
    private int quantity;
    private double wholesalePrice;

    // Getters
    public Long getProductId() {
        return productId;
    }

    public Long getVersion() {
        return version;
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
    
    public double getWholesalePrice() { 
        return wholesalePrice; 
    }
    
    // Explicit setters (Lombok may not be generating them properly)
    public void setRetailPrice(double retailPrice) {
        this.retailPrice = retailPrice;
    }
    
    public void setWholesalePrice(double wholesalePrice) {
        this.wholesalePrice = wholesalePrice;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

}