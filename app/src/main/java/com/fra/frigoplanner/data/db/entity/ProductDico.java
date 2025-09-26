package com.fra.frigoplanner.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(
        primaryKeys = {"productName"}
)
public class ProductDico {
    @NonNull
    public String productName;
    @ColumnInfo(defaultValue = "0")
    public double averagePrice;
    @ColumnInfo(defaultValue = "0")
    public int portions;
    public String aisle;

    public ProductDico(@NonNull String productName) {
        this.productName = productName;
        this.averagePrice = 0;
        this.portions = 0;
        this.aisle = null;
    }
}
