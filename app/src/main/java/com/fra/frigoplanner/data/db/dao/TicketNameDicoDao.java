package com.fra.frigoplanner.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.fra.frigoplanner.data.db.entity.ProductDico;
import com.fra.frigoplanner.data.db.entity.ProductTypeDico;
import com.fra.frigoplanner.data.db.entity.TicketNameDico;

import java.util.List;

@Dao
public interface TicketNameDicoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(TicketNameDico entry);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<TicketNameDico> entries);

    @Query("DELETE FROM TicketNameDico")
    void clearAll();

    @Query("SELECT * FROM TicketNameDico ORDER BY productName")
    List<TicketNameDico> getAll();

    @Query("SELECT COUNT(*) FROM TicketNameDico")
    int getTicketNamesCount();

    @Query("SELECT * FROM TicketNameDico WHERE productName = :productName")
    TicketNameDico getByName(String productName);

    @Query("SELECT * FROM TicketNameDico WHERE ticketName = :ticketName")
    TicketNameDico getByTicketName(String ticketName);

    @Query("SELECT ticketName FROM TicketNameDico WHERE ticketName IS NOT NULL ORDER BY ticketName")
    List<String> getAllTicketNames();
}
