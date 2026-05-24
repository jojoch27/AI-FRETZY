package com.rplbo.app;

public class Product {
    private final int productId;
    private final String brand;
    private final String title;
    private final String category;
    private final String name;
    private final double price;
    private final String description;
    private final String imageUrl;

    public Product(int productId, String brand, String title, String category, double price, String description, String imageUrl) {
        this.productId = productId;
        this.brand = brand;
        this.title = title;
        this.category = category;
        this.name = (brand + " " + title).trim();
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public Product(int productId, String brand, String title, String category, double price, String description) {
        this(productId, brand, title, category, price, description, "");
    }

    public Product(int productId, String name, double price, String description) {
        this(productId, "", name, description, price, description, "");
    }

    public int getProductId() {
        return productId;
    }

    public String getBrand() {
        return brand;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public String toString() {
        return getName();
    }
}
