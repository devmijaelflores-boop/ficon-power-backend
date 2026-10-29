package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.MonitoringViews.SiteView;
import com.devzonesoft.ficonpower.ports.output.SiteQueryRepository;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresSiteQueryRepository implements SiteQueryRepository {

    private final JdbcTemplate jdbc;
    private final MonitoringViewRowMapper rows;

    public PostgresSiteQueryRepository(JdbcTemplate jdbc, MonitoringViewRowMapper rows) {
        this.jdbc = jdbc;
        this.rows = rows;
    }

    @Override
    public List<SiteView> findSites() {
        return jdbc.query("""
                SELECT s.id, s.name, s.address, s.district, s.province, s.department,
                       s.latitude, s.longitude, s.time_zone, s.is_active,
                       count(d.id) AS device_count, s.created_at, s.updated_at
                FROM sites s
                LEFT JOIN devices d ON d.site_id = s.id AND d.is_active = true
                GROUP BY s.id
                ORDER BY s.name
                """, rows::site);
    }
}
