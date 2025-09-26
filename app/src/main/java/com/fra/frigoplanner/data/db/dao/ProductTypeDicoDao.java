package com.fra.frigoplanner.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.fra.frigoplanner.data.db.entity.ProductTypeDico;

import java.util.List;

@Dao
public interface ProductTypeDicoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ProductTypeDico entry);

    @Query("DELETE FROM ProductTypeDico")
    void clearAll();

    @Query("SELECT * FROM ProductTypeDico ORDER BY productName")
    List<ProductTypeDico> getAll();

    @Query("SELECT * FROM ProductTypeDico WHERE productName = :productName AND productType = :productType")
    ProductTypeDico getProduct(String productName, String productType);

    @Query("SELECT productType FROM ProductTypeDico WHERE productName = :productName ORDER BY occurrences DESC LIMIT 1")
    String getMostFrequentProductType(String productName);

    @Query("UPDATE ProductTypeDico SET occurrences = occurrences + 1 WHERE productName = :productName AND productType = :productType")
    void increaseOccurrence(String productName, String productType);
}
