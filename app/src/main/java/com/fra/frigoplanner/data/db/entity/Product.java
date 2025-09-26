package com.fra.frigoplanner.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(
        primaryKeys = {"year", "month", "rowNumber"},
        foreignKeys = @ForeignKey(
                entity = ProductDico.class,
                parentColumns = "productName",
                childColumns = "productName",
                onDelete = ForeignKey.NO_ACTION,
                onUpdate = ForeignKey.CASCADE
        )
)
public class Product {
    public int year;
    public int month;
    public int rowNumber;
    public String productName;
    public String type;
    public double price;
    public String expirationDate;
    public boolean eaten;

    public Product(int year, int month, int rowNumber, String productName, String type, double price) {
        this.year = year;
        this.month = month;
        this.rowNumber = rowNumber;
        this.productName = productName;
        this.type = type;
        this.price = price;
        this.expirationDate = null;
        this.eaten = false;
    }
}
