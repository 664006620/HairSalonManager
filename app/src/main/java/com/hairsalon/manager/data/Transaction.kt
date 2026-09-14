package com.hairsalon.manager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val memberName: String,
    val type: Int,          // 0=充值, 1=消费
    val amount: Double,
    val balanceAfter: Double,
    val note: String = "",
    val time: Long = System.currentTimeMillis()
)
