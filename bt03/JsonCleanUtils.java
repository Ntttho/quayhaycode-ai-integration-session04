package com.example.etl.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonCleanUtils {

    private static final Pattern MARKDOWN_JSON_PATTERN = Pattern.compile("(?s)```(?:json)?\\s*(.*?)\\s*```");

    /**
     * Trích xuất và làm sạch dữ liệu chuỗi JSON trả về từ LLM.
     * Loại bỏ các thẻ bao bọc markdown ```json ... ``` hoặc ``` ... ```.
     */
    public static String cleanJson(String rawInput) {
        if (rawInput == null) {
            return "";
        }
        String trimmed = rawInput.trim();
        Matcher matcher = MARKDOWN_JSON_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return trimmed;
    }
}