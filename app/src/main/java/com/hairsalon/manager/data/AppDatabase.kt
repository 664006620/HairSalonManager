package com.hairsalon.manager.data

import android.content.Context
import androidx.room.*

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY pinyin ASC")
    suspend fun getAllMembers(): List<Member>

    @Query("SELECT * FROM members WHERE pinyin LIKE :letter || '%' ORDER BY pinyin ASC")
    suspend fun getMembersByLetter(letter: String): List<Member>

    @Query("SELECT * FROM members WHERE name LIKE '%' || :keyword || '%' OR phone LIKE '%' || :keyword || '%'")
    suspend fun searchMembers(keyword: String): List<Member>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: Long): Member?

    @Insert
    suspend fun insertMember(member: Member): Long

    @Update
    suspend fun updateMember(member: Member)

    @Delete
    suspend fun deleteMember(member: Member)

    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    @Query("SELECT * FROM transactions WHERE memberId = :memberId ORDER BY time DESC")
    suspend fun getTransactionsByMember(memberId: Long): List<Transaction>

    @Query("SELECT * FROM transactions ORDER BY time DESC LIMIT 50")
    suspend fun getRecentTransactions(): List<Transaction>

    @Query("DELETE FROM members")
    suspend fun deleteAllMembers()

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}

@Database(entities = [Member::class, Transaction::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hairsalon.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
