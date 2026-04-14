package com.example.sketchnote.utils

object MathCalculator {

    // Evaluate một expression string thuần
    fun evaluate(expression: String): Double? {
        return try {
            val cleaned = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace(" ", "")
                .trim()

            if (cleaned.isEmpty()) return null
            // Chỉ cho phép số, toán tử, dấu chấm, ngoặc
            if (!cleaned.matches(Regex("""^[\d\+\-\*\/\(\)\.\^]+$"""))) return null

            parseExpr(cleaned, 0).first
        } catch (_: Exception) {
            null
        }
    }

    // Parser đơn giản dùng index
    private fun parseExpr(s: String, pos: Int): Pair<Double, Int> {
        var (left, idx) = parseTerm(s, pos)
        var i = idx
        while (i < s.length && (s[i] == '+' || s[i] == '-')) {
            val op = s[i]
            val (right, nextIdx) = parseTerm(s, i + 1)
            left = if (op == '+') left + right else left - right
            i = nextIdx
        }
        return Pair(left, i)
    }

    private fun parseTerm(s: String, pos: Int): Pair<Double, Int> {
        var (left, idx) = parseFactor(s, pos)
        var i = idx
        while (i < s.length && (s[i] == '*' || s[i] == '/')) {
            val op = s[i]
            val (right, nextIdx) = parseFactor(s, i + 1)
            left = if (op == '*') left * right else left / right
            i = nextIdx
        }
        return Pair(left, i)
    }

    private fun parseFactor(s: String, pos: Int): Pair<Double, Int> {
        var i = pos
        if (i < s.length && s[i] == '(') {
            val (v, nextIdx) = parseExpr(s, i + 1)
            // skip ')'
            return Pair(v, nextIdx + 1)
        }
        if (i < s.length && s[i] == '-') {
            val (v, nextIdx) = parseFactor(s, i + 1)
            return Pair(-v, nextIdx)
        }
        val start = i
        while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
        val numStr = s.substring(start, i)
        return Pair(numStr.toDoubleOrNull() ?: 0.0, i)
    }

    /**
     * Phát hiện biểu thức toán trong text và tính kết quả.
     * Khi user nhập "2+3=" cuối dòng → trả về "= 5"
     * Khi text đã có "= 5" rồi → không tính lại.
     */
    fun detectAndCalculate(text: String): String? {
        // Nếu text đã có kết quả rồi thì bỏ qua
        if (text.contains(Regex("""=\s*-?\d"""))) return null

        // Tìm biểu thức kết thúc bằng =
        val match = Regex("""([\d\s\+\-\*\/\(\)\.]+)=\s*$""").find(text.trimEnd())
            ?: return null

        val expr = match.groupValues[1].trim()
        if (expr.isBlank()) return null

        val result = evaluate(expr) ?: return null

        val formatted = if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            "%.2f".format(result)
        }
        return formatted
    }
}