package com.kingsman.vault.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Business::class, Category::class, Txn::class,
        Goal::class, GoalEntry::class,
        Investment::class, InvestmentEntry::class,
        Debt::class, DebtPayment::class,
        Recurring::class,
        Budget::class, SmsSuggestion::class
    ],
    version = 2,
    exportSchema = true
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun dao(): VaultDao

    companion object {
        /** v1 -> v2: receipt photos, budgets, bank-alert suggestions. Keeps all existing data. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `receiptPath` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budgets` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`categoryId` INTEGER, " +
                        "`categoryName` TEXT NOT NULL, " +
                        "`businessId` INTEGER, " +
                        "`currency` TEXT NOT NULL, " +
                        "`amount` INTEGER NOT NULL, " +
                        "`alertPercent` INTEGER NOT NULL, " +
                        "`alertedMonth` TEXT NOT NULL, " +
                        "`alertedLevel` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sms_suggestions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`hash` TEXT NOT NULL, " +
                        "`sender` TEXT NOT NULL, " +
                        "`body` TEXT NOT NULL, " +
                        "`kind` TEXT NOT NULL, " +
                        "`amount` INTEGER NOT NULL, " +
                        "`currency` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, " +
                        "`date` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_sms_suggestions_hash` ON `sms_suggestions` (`hash`)"
                )
            }
        }
    }
}
