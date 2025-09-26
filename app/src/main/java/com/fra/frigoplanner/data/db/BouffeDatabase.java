package com.fra.frigoplanner.data.db;

import androidx.annotation.NonNull;
import androidx.room.*;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import android.content.Context;

import com.fra.frigoplanner.data.db.dao.ProductDao;
import com.fra.frigoplanner.data.db.dao.ProductDicoDao;
import com.fra.frigoplanner.data.db.dao.TempProductDao;
import com.fra.frigoplanner.data.db.dao.TicketNameDicoDao;
import com.fra.frigoplanner.data.db.dao.ProductTypeDicoDao;
import com.fra.frigoplanner.data.db.entity.Product;
import com.fra.frigoplanner.data.db.entity.ProductDico;
import com.fra.frigoplanner.data.db.entity.ProductTypeDico;
import com.fra.frigoplanner.data.db.entity.TempProduct;
import com.fra.frigoplanner.data.db.entity.TicketNameDico;

@Database(entities = {Product.class, ProductDico.class, ProductTypeDico.class, TicketNameDico.class, TempProduct.class}, version = 17)
public abstract class BouffeDatabase extends RoomDatabase {
    public abstract ProductDao productDao();
    public abstract ProductDicoDao productDicoDao();
    public abstract ProductTypeDicoDao productTypeDicoDao();
    public abstract TicketNameDicoDao ticketNameDicoDao();
    public abstract TempProductDao tempProductDao();

    private static volatile BouffeDatabase INSTANCE;

    public static BouffeDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BouffeDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            BouffeDatabase.class,
                            "bouffe.db"
                    )
                    .fallbackToDestructiveMigration()
                    // .addMigrations(MIGRATION)
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    static final Migration MIGRATION = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("SELECT NULL");
        }
    };
}