package com.example.sketchnote.utils

import com.example.sketchnote.ui.editor.ContentBlock
import com.example.sketchnote.ui.editor.WrapMode
import org.json.JSONArray
import org.json.JSONObject

object SketchSerializer {

    /**
     * Chuyển đổi blocks thành JSON string
     * Format mới: TEXT:id:text ||| SKETCH:id:imagePath:heightDp
     */
    fun blocksToJson(blocks: List<ContentBlock>): String {
        val arr = JSONArray()
        blocks.forEach { block ->
            val obj = JSONObject()
            when (block) {
                is ContentBlock.TextBlock -> {
                    obj.put("type", "TEXT")
                    obj.put("id", block.id)
                    obj.put("text", block.text)
                }
                is ContentBlock.SketchBlock -> {
                    obj.put("type", "SKETCH")
                    obj.put("id", block.id)
                    obj.put("imagePath", block.imagePath)
                    obj.put("heightDp", block.heightDp)
                }
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    /**
     * Chuyển đổi từ JSON string thành list ContentBlock
     * Hỗ trợ cả format cũ (có actions/wrapMode) và format mới (imagePath)
     */
    fun jsonToBlocks(json: String): List<ContentBlock> {
        return try {
            val arr = JSONArray(json)
            val blocks = mutableListOf<ContentBlock>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                when (obj.getString("type")) {
                    "TEXT" -> blocks.add(
                        ContentBlock.TextBlock(
                            id = obj.getString("id"),
                            text = obj.getString("text")
                        )
                    )
                    "SKETCH" -> {
                        // Kiểm tra xem là format mới (có imagePath) hay format cũ (có actions)
                        if (obj.has("imagePath")) {
                            // Format mới
                            blocks.add(
                                ContentBlock.SketchBlock(
                                    id = obj.getString("id"),
                                    imagePath = obj.optString("imagePath", ""),
                                    heightDp = obj.optInt("heightDp", 200)
                                )
                            )
                        } else {
                            // Format cũ - bỏ qua actions, tạo SketchBlock rỗng
                            // Hoặc có thể chuyển đổi actions thành ảnh nếu cần
                            blocks.add(
                                ContentBlock.SketchBlock(
                                    id = obj.getString("id"),
                                    imagePath = "",  // Không thể chuyển actions thành ảnh ở đây
                                    heightDp = obj.optInt("heightDp", 200)
                                )
                            )
                        }
                    }
                }
            }
            blocks.ifEmpty { listOf(ContentBlock.TextBlock()) }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(ContentBlock.TextBlock())
        }
    }

    /**
     * Chuyển đổi từ format cũ (actions) sang format mới (imagePath)
     * Hàm này có thể được gọi khi cần migrate dữ liệu cũ
     */
    fun migrateOldToNew(oldJson: String): String {
        return try {
            val oldBlocks = jsonToBlocks(oldJson) // Đọc với logic cũ
            blocksToJson(oldBlocks) // Ghi với logic mới
        } catch (e: Exception) {
            oldJson
        }
    }
}