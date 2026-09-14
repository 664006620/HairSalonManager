package com.hairsalon.manager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val balance: Double = 0.0,
    val level: String = "普通会员",
    val remark: String = "",
    val createTime: Long = System.currentTimeMillis(),
    val pinyin: String = ""  // 姓名拼音首字母，用于快速查找
)
