package com.fra.frigoplanner.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(
        primaryKeys = {"productName", "productType"},
        foreignKeys = @ForeignKey(
                entity = ProductDico.class,
                parentColumns = "productName",
                childColumns = "productName",
                onDelete = ForeignKey.NO_ACTION,
                onUpdate = ForeignKey.CASCADE
        )
)
public class ProductTypeDico {
    @NonNull
    public String productName;
    @NonNull
    public String productType;
    @ColumnInfo(defaultValue = "1")
    public int occurrences;

    public ProductTypeDico(@NonNull String productName, @NonNull String productType) {
        this.productName = productName;
        this.productType = productType;
        this.occurrences = 1;
    }
}
