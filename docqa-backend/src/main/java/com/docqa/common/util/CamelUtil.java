package com.docqa.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Map key 蛇形转驼峰：统一 SQL 直查结果与前端（驼峰）契约
 */
public final class CamelUtil {

    private CamelUtil() { }

    public static Map<String, Object> camelRow(Map<String, Object> row) {
        Map<String, Object> m = new HashMap<>();
        for (Map.Entry<String, Object> e : row.entrySet()) {
            String[] parts = e.getKey().split("_");
            StringBuilder key = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                if (parts[i].isEmpty()) continue;
                key.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            }
            m.put(key.toString(), e.getValue());
        }
        return m;
    }

    public static List<Map<String, Object>> camel(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) out.add(camelRow(row));
        return out;
    }
}
