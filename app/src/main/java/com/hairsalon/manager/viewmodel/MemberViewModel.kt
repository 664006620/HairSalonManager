package com.hairsalon.manager.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hairsalon.manager.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class MemberViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).memberDao()

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Member>>(emptyList())
    val searchResults: StateFlow<List<Member>> = _searchResults.asStateFlow()

    private val _currentMember = MutableStateFlow<Member?>(null)
    val currentMember: StateFlow<Member?> = _currentMember.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    init {
        loadMembers()
    }

    fun loadMembers() {
        viewModelScope.launch {
            _members.value = dao.getAllMembers()
        }
    }

    fun searchByLetter(letter: String) {
        viewModelScope.launch {
            _searchResults.value = if (letter.isEmpty()) {
                dao.getAllMembers()
            } else {
                dao.getMembersByLetter(letter.uppercase())
            }
        }
    }

    fun searchByKeyword(keyword: String) {
        viewModelScope.launch {
            _searchResults.value = if (keyword.isEmpty()) {
                dao.getAllMembers()
            } else {
                dao.searchMembers(keyword)
            }
        }
    }

    fun loadMember(id: Long) {
        viewModelScope.launch {
            _currentMember.value = dao.getMemberById(id)
            _transactions.value = dao.getTransactionsByMember(id)
        }
    }

    fun addMember(name: String, phone: String, level: String, remark: String) {
        viewModelScope.launch {
            val member = Member(
                name = name,
                phone = phone,
                level = level,
                remark = remark,
                pinyin = getPinyinInitial(name)
            )
            dao.insertMember(member)
            loadMembers()
            _message.value = "添加成功"
        }
    }

    fun updateMember(member: Member) {
        viewModelScope.launch {
            dao.updateMember(member.copy(pinyin = getPinyinInitial(member.name)))
            loadMembers()
            _currentMember.value = member
            _message.value = "修改成功"
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            dao.deleteMember(member)
            loadMembers()
            _message.value = "已删除"
        }
    }

    fun charge(memberId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            val member = dao.getMemberById(memberId) ?: return@launch
            val newBalance = member.balance + amount
            dao.updateMember(member.copy(balance = newBalance))
            dao.insertTransaction(
                Transaction(
                    memberId = memberId,
                    memberName = member.name,
                    type = 0,
                    amount = amount,
                    balanceAfter = newBalance,
                    note = note
                )
            )
            loadMember(memberId)
            loadMembers()
            _message.value = "充值 ¥$amount 成功"
        }
    }

    fun deduct(memberId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            val member = dao.getMemberById(memberId) ?: return@launch
            if (member.balance < amount) {
                _message.value = "余额不足！当前余额 ¥${member.balance}"
                return@launch
            }
            val newBalance = member.balance - amount
            dao.updateMember(member.copy(balance = newBalance))
            dao.insertTransaction(
                Transaction(
                    memberId = memberId,
                    memberName = member.name,
                    type = 1,
                    amount = amount,
                    balanceAfter = newBalance,
                    note = note
                )
            )
            loadMember(memberId)
            loadMembers()
            _message.value = "扣费 ¥$amount 成功"
        }
    }

    fun importFromTxt(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                var count = 0
                reader.forEachLine { line ->
                    val parts = line.split(",", "\t", "|")
                    if (parts.size >= 2) {
                        val name = parts[0].trim()
                        val phone = parts.getOrNull(1)?.trim() ?: ""
                        val balance = parts.getOrNull(2)?.trim()?.toDoubleOrNull() ?: 0.0
                        val level = parts.getOrNull(3)?.trim() ?: "普通会员"
                        if (name.isNotEmpty()) {
                            dao.insertMember(
                                Member(
                                    name = name,
                                    phone = phone,
                                    balance = balance,
                                    level = level,
                                    pinyin = getPinyinInitial(name)
                                )
                            )
                            count++
                        }
                    }
                }
                reader.close()
                loadMembers()
                _message.value = "成功导入 $count 条会员记录"
            } catch (e: Exception) {
                _message.value = "导入失败: ${e.message}"
            }
        }
    }

    fun exportDatabase(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val dbFile = context.getDatabasePath("hairsalon.db")
                val outputStream = context.contentResolver.openOutputStream(uri)
                val inputStream = FileInputStream(dbFile)
                inputStream.copyTo(outputStream!!)
                inputStream.close()
                outputStream.close()
                _message.value = "数据库导出成功"
            } catch (e: Exception) {
                _message.value = "导出失败: ${e.message}"
            }
        }
    }

    fun exportMembersToTxt(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val outputStream = context.contentResolver.openOutputStream(uri)
                val writer = BufferedWriter(OutputStreamWriter(outputStream, "UTF-8"))
                writer.write("姓名,电话,余额,等级,备注,创建时间")
                writer.newLine()
                _members.value.forEach { m ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    writer.write("${m.name},${m.phone},${m.balance},${m.level},${m.remark},${sdf.format(Date(m.createTime))}")
                    writer.newLine()
                }
                writer.close()
                _message.value = "会员列表导出成功"
            } catch (e: Exception) {
                _message.value = "导出失败: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = ""
    }

    // 简易拼音首字母提取（支持常用汉字）
    private fun getPinyinInitial(name: String): String {
        if (name.isEmpty()) return "#"
        val first = name[0]
        return when {
            first in 'A'..'Z' || first in 'a'..'z' -> first.uppercase()
            first in '0'..'9' -> "#"
            else -> getChineseInitial(first)
        }
    }

    private fun getChineseInitial(c: Char): String {
        // 基于 Unicode 区间的简易拼音首字母映射
        val code = c.code
        return when {
            code in 0x4E00..0x4E8C -> "A"  // 简化处理
            code in 0x5317..0x5317 -> "B"
            code in 0x5F69..0x5F69 -> "C"
            code in 0x5927..0x5927 -> "D"
            else -> {
                // 使用 GBK 编码范围进行更精确的映射
                try {
                    val bytes = c.toString().toByteArray(charset("GBK"))
                    if (bytes.size == 2) {
                        val high = bytes[0].toInt() and 0xFF
                        val low = bytes[1].toInt() and 0xFF
                        val code2 = high * 256 + low
                        when {
                            code2 >= 0xB0A1 && code2 <= 0xB0C4 -> "A"
                            code2 >= 0xB0C5 && code2 <= 0xB2C0 -> "B"
                            code2 >= 0xB2C1 && code2 <= 0xB4ED -> "C"
                            code2 >= 0xB4EE && code2 <= 0xB6E9 -> "D"
                            code2 >= 0xB6EA && code2 <= 0xB7A1 -> "E"
                            code2 >= 0xB7A2 && code2 <= 0xB8C0 -> "F"
                            code2 >= 0xB8C1 && code2 <= 0xB9FD -> "G"
                            code2 >= 0xB9FE && code2 <= 0xBBF6 -> "H"
                            code2 >= 0xBBF7 && code2 <= 0xBFA5 -> "J"
                            code2 >= 0xBFA6 && code2 <= 0xC0AB -> "K"
                            code2 >= 0xC0AC && code2 <= 0xC2E7 -> "L"
                            code2 >= 0xC2E8 && code2 <= 0xC4C2 -> "M"
                            code2 >= 0xC4C3 && code2 <= 0xC5B5 -> "N"
                            code2 >= 0xC5B6 && code2 <= 0xC5BD -> "O"
                            code2 >= 0xC5BE && code2 <= 0xC6D9 -> "P"
                            code2 >= 0xC6DA && code2 <= 0xC8BA -> "Q"
                            code2 >= 0xC8BB && code2 <= 0xC8F5 -> "R"
                            code2 >= 0xC8F6 && code2 <= 0xCBF9 -> "S"
                            code2 >= 0xCBFA && code2 <= 0xCDD9 -> "T"
                            code2 >= 0xCDDA && code2 <= 0xCEF3 -> "W"
                            code2 >= 0xCEF4 && code2 <= 0xD1B8 -> "X"
                            code2 >= 0xD1B9 && code2 <= 0xD4D0 -> "Y"
                            code2 >= 0xD4D1 && code2 <= 0xD7F9 -> "Z"
                            else -> "#"
                        }
                    } else "#"
                } catch (e: Exception) {
                    "#"
                }
            }
        }
    }
}
