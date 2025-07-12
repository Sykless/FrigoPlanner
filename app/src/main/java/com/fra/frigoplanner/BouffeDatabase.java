package com.fra.frigoplanner;

import androidx.room.*;
import android.content.Context;
import java.util.List;

@Entity(primaryKeys = {"year", "month", "row"})
class Bouffe {
    public int year;
    public int month;
    public int row;
    public String name;
    public String type;
    public String expirationDate;
    public boolean eaten;

    public Bouffe(int year, int month, int row, String name, String type) {
        this.year = year;
        this.month = month;
        this.row = row;
        this.name = name;
        this.type = type;
        this.expirationDate = null;
        this.eaten = false;
    }
}

@Dao
interface BouffeDao {
    @Insert
    void insert(Bouffe product);

    @Query("SELECT * FROM Bouffe WHERE type = :type")
    List<Bouffe> getBouffeByType(String type);

    @Query("DELETE FROM Bouffe")
    void clearAll();
}

@Database(entities = {Bouffe.class}, version = 2)
public abstract class BouffeDatabase extends RoomDatabase {
    public abstract BouffeDao productDao();

    private static volatile BouffeDatabase INSTANCE;

    public static BouffeDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BouffeDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            BouffeDatabase.class,
                            "bouffe.db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
