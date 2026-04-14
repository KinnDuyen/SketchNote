package com.example.sketchnote.utils

object AutoTagUtils {

    // Map từ khóa → màu tag gợi ý
    private val tagRules = mapOf(
        "RED" to listOf(
            "deadline", "gấp", "khẩn", "quan trọng", "urgent",
            "nộp", "hạn", "cảnh báo", "lỗi", "bug"
        ),
        "ORANGE" to listOf(
            "mua", "sắm", "chợ", "siêu thị", "order",
            "đặt hàng", "thanh toán", "tiền", "chi phí"
        ),
        "YELLOW" to listOf(
            "ý tưởng", "idea", "brainstorm", "sáng tạo",
            "kế hoạch", "plan", "dự án", "project"
        ),
        "GREEN" to listOf(
            "xong", "hoàn thành", "done", "ok", "thành công",
            "học", "ôn tập", "lịch", "schedule"
        ),
        "BLUE" to listOf(
            "công việc", "work", "họp", "meeting", "báo cáo",
            "report", "email", "liên hệ", "contact"
        ),
        "PURPLE" to listOf(
            "cá nhân", "personal", "nhật ký", "diary",
            "cảm xúc", "suy nghĩ", "note", "ghi nhớ"
        )
    )

    /**
     * Phân tích nội dung và gợi ý màu tag phù hợp nhất.
     * Trả về null nếu không match được rule nào.
     */
    fun suggestTag(title: String, content: String): String? {
        val text = (title + " " + content).lowercase()

        // Đếm số từ khóa match cho từng màu
        val scores = tagRules.mapValues { (_, keywords) ->
            keywords.count { keyword -> text.contains(keyword) }
        }

        // Lấy màu có điểm cao nhất, bỏ qua nếu điểm = 0
        val best = scores.maxByOrNull { it.value }
        return if ((best?.value ?: 0) > 0) best?.key else null
    }
}