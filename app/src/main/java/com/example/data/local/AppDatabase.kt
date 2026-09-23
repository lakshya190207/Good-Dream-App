package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        CartItemEntity::class,
        WishlistItemEntity::class,
        InquiryEntity::class,
        AppConfigEntity::class,
        OrderEntity::class,
        UserEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun inquiryDao(): InquiryDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun orderDao(): OrderDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN videoUrl TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `orders` (
                        `id` TEXT NOT NULL,
                        `customerName` TEXT NOT NULL,
                        `customerPhone` TEXT NOT NULL,
                        `customerEmail` TEXT NOT NULL,
                        `deliveryAddress` TEXT NOT NULL,
                        `city` TEXT NOT NULL,
                        `state` TEXT NOT NULL,
                        `pincode` TEXT NOT NULL,
                        `deliverySlot` TEXT NOT NULL,
                        `floorElevator` TEXT NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `paymentStatus` TEXT NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `itemsSummary` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `users` (
                        `email` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phone` TEXT NOT NULL DEFAULT '',
                        `userType` TEXT NOT NULL DEFAULT 'REGISTERED_VIP',
                        `hashedPasscode` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `addressesJson` TEXT NOT NULL DEFAULT '[]',
                        `isCurrentSession` INTEGER NOT NULL DEFAULT 0,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`email`)
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "good_dream_catalog.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                 .fallbackToDestructiveMigrationOnDowngrade(true)
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
