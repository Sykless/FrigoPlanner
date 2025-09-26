package com.fra.frigoplanner.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(
        primaryKeys = {"ticketFileName", "productId"},
        foreignKeys = @ForeignKey(
                entity = ProductDico.class,
                parentColumns = "productName",
                childColumns = "productName",
                onDelete = ForeignKey.NO_ACTION,
                onUpdate = ForeignKey.CASCADE
        )
)
public class TempProduct {
    @NonNull
    public String ticketFileName;
    public int productId;
    public String productName;
    public String productType;
    public double price;
    public String expirationDate;

    public TempProduct(@NonNull String ticketFileName, int productId, String productName, String productType, double price, String expirationDate) {
        this.ticketFileName = ticketFileName;
        this.productId = productId;
        this.productName = productName;
        this.productType = productType;
        this.price = price;
        this.expirationDate = expirationDate;}
}
