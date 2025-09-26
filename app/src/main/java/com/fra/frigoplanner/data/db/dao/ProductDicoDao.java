package com.fra.frigoplanner.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.fra.frigoplanner.data.db.entity.ProductDico;

import java.util.List;

@Dao
public interface ProductDicoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ProductDico entry);

    @Query("SELECT EXISTS(SELECT 1 FROM ProductDico WHERE productName = :productName)")
    boolean exists(String productName);

    @Query("DELETE FROM ProductDico")
    void clearAll();

    @Query("SELECT * FROM ProductDico ORDER BY productName")
    List<ProductDico> getAll();

    @Query("SELECT * FROM ProductDico WHERE productName = :productName")
    ProductDico getByName(String productName);

    @Query("UPDATE ProductDico SET averagePrice = :averagePrice WHERE productName = :productName")
    void updateAveragePrice(String productName, double averagePrice);

    @Query("SELECT productName FROM ProductDico ORDER BY productName")
    List<String> getAllProductNames();
}
