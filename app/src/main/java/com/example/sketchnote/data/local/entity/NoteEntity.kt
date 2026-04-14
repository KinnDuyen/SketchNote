package com.example.sketchnote.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String = "",
    val content: String = "",           // Nội dung văn bản (để tìm kiếm nhanh)

    // Loại ghi chú: "TEXT" hoặc "CHECKLIST"
    val type: String = "TEXT",

    // Màu nhãn: "RED", "ORANGE", "YELLOW", "GREEN", "BLUE", "PURPLE", "NONE"
    val colorTag: String = "NONE",

    // Đường dẫn ảnh đính kèm (ngăn cách bằng "||")
    val imagePaths: String = "",

    // Đường dẫn file sketch cũ (.png) - Giữ lại để không mất dữ liệu cũ
    val sketchPath: String = "",

    // MỚI: Lưu toàn bộ block content (text + sketch) dưới dạng JSON
    val sketchData: String? = null,

    // Thời gian nhắc nhở (timestamp, 0L hoặc -1L tùy cưng quy định, ở đây dùng 0L cho đồng bộ)
    val reminderTime: Long = 0L,

    // Trạng thái ghim ghi chú
    val isPinned: Boolean = false,

    // Xóa tạm thời (Recycle Bin)
    val isDeleted: Boolean = false,

    // Thời gian tạo & cập nhật
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)