package com.fra.frigoplanner.data.db;

import androidx.annotation.NonNull;
import androidx.room.*;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import android.content.Context;

import com.fra.frigoplanner.data.db.dao.BouffeDao;
import com.fra.frigoplanner.data.db.dao.BouffeDicoDao;
import com.fra.frigoplanner.data.db.entity.Bouffe;
import com.fra.frigoplanner.data.db.entity.BouffeDico;

@Database(entities = {Bouffe.class, BouffeDico.class}, version = 9)
public abstract class BouffeDatabase extends RoomDatabase {
    public abstract BouffeDao productDao();
    public abstract BouffeDicoDao dicoDao();

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
                    .addMigrations(MIGRATION)
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    static final Migration MIGRATION = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE BouffeDico ADD COLUMN ticketName TEXT");
        }
    };
}