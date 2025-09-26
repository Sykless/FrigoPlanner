package com.fra.frigoplanner.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(
        primaryKeys = {"productName", "ticketName"},
        foreignKeys = @ForeignKey(
                entity = ProductDico.class,
                parentColumns = "productName",
                childColumns = "productName",
                onDelete = ForeignKey.NO_ACTION,
                onUpdate = ForeignKey.CASCADE
        )
)
public class TicketNameDico {
    @NonNull
    public String productName;
    @NonNull
    public String ticketName;

    public TicketNameDico(@NonNull String productName, @NonNull String ticketName) {
        this.productName = productName;
        this.ticketName = ticketName;
    }
}
