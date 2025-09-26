package com.fra.frigoplanner.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.fra.frigoplanner.data.db.entity.Product;

import java.util.List;

@Dao
public interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Product product);

    @Query("SELECT EXISTS(SELECT 1 FROM Product WHERE year = :year AND month = :month AND rowNumber = :rowNumber)")
    boolean exists(int year, int month, int rowNumber);

    @Query("DELETE FROM Product")
    void clearAll();

    @Query("SELECT * FROM Product ORDER BY year desc, month desc, rowNumber desc limit 1")
    Product getLatestProduct();

    @Query("SELECT price FROM Product WHERE productName = :productName ORDER BY year desc, month desc, rowNumber desc limit 10")
    List<Double> getLatestPrices(String productName);
}
