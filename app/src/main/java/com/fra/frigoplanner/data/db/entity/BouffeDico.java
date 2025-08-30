package com.fra.frigoplanner.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(
        primaryKeys = {"name"}
)
public class BouffeDico {
    @NonNull
    public String name;
    public double averagePrice;
    public int portions;
    public String ticketName;
    public String aisle;

    public BouffeDico(@NonNull String name, double averagePrice, int portions, String ticketName, String aisle) {
        this.name = name;
        this.averagePrice = averagePrice;
        this.portions = portions;
        this.ticketName = ticketName;
        this.aisle = aisle;
    }
}
