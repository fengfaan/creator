package com.aiwriter.service;

import com.aiwriter.model.ConfigItem;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final JdbcTemplate jdbc;

    public ConfigItem get(String key) {
        List<ConfigItem> results = jdbc.query(
                "SELECT key, value, updated_at FROM settings WHERE key = ?",
                (rs, i) -> new ConfigItem(rs.getString("key"), rs.getString("value"), rs.getString("updated_at")),
                key
        );
        return results.isEmpty() ? null : results.get(0);
    }

    public List<ConfigItem> list() {
        return jdbc.query(
                "SELECT key, value, updated_at FROM settings ORDER BY key",
                (rs, i) -> new ConfigItem(rs.getString("key"), rs.getString("value"), rs.getString("updated_at"))
        );
    }

    public void set(String key, String value) {
        jdbc.update("""
            INSERT INTO settings (key, value, updated_at) VALUES (?, ?, datetime('now'))
            ON CONFLICT(key) DO UPDATE SET value = ?, updated_at = datetime('now')
        """, key, value, value);
    }
}
