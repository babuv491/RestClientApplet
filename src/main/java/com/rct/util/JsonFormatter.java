package com.rct.util;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonFormatter{

    public static String formatJson(String json) {
        if (json== null || json.trim().isEmpty()) return json;

        try {
            String trimmed = json.trim();
            if (trimmed.startsWith("{")) {
                return new JSONObject(trimmed).toString(2);
            } else if (trimmed.startsWith("[")) {
                return new JSONArray(trimmed).toString(2);
            }
            return json;
        } catch (Exception e) {
            return json;
        }
    }

    public static String minifyJson(String json) {
        if (json== null || json.trim().isEmpty()) return json;

        try {
            String trimmed = json.trim();
            if (trimmed.startsWith("{")) {
                return new JSONObject(trimmed).toString();
            } else if (trimmed.startsWith("[")) {
                return new JSONArray(trimmed).toString();
            }
            return json;
        } catch (Exception e) {
            return json;
        }
    }

    public static boolean isValidJson(String json) {
        if (json== null || json.trim().isEmpty()) return false;

        try {
            String trimmed = json.trim();
            if (trimmed.startsWith("{")) {
                new JSONObject(trimmed);
            } else if (trimmed.startsWith("[")) {
                new JSONArray(trimmed);
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}