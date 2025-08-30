package com.fra.frigoplanner.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(
        primaryKeys = {"year", "month", "rowNumber"},
        foreignKeys = @ForeignKey(
                entity = BouffeDico.class,
                parentColumns = "name",
                childColumns = "name",
                onDelete = ForeignKey.NO_ACTION,
                onUpdate = ForeignKey.CASCADE
        )
)
public class Bouffe {
    public int year;
    public int month;
    public int rowNumber;
    public String name;
    public String type;
    public double price;
    public String expirationDate;
    public boolean eaten;

    public Bouffe(int year, int month, int rowNumber, String name, String type, double price) {
        this.year = year;
        this.month = month;
        this.rowNumber = rowNumber;
        this.name = name;
        this.type = type;
        this.price = price;
        this.expirationDate = null;
        this.eaten = false;
    }
}
