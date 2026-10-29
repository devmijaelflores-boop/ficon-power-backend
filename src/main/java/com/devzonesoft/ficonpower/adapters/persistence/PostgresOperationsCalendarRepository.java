package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.OperationsCalendarEntry;
import com.devzonesoft.ficonpower.domain.TuyaPlan;
import com.devzonesoft.ficonpower.ports.output.OperationsCalendarRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresOperationsCalendarRepository implements OperationsCalendarRepository {

    private static final String PLAN_SELECT = """
            SELECT id, project_name, plan_name, last_renewed_on, next_renewal_on,
                   renewal_interval_months, reminder_days, notes, is_active,
                   created_at, updated_at
            FROM tuya_plans
            """;

    private final JdbcTemplate jdbc;
    private final ZoneId timeZone;

    public PostgresOperationsCalendarRepository(JdbcTemplate jdbc, ZoneId timeZone) {
        this.jdbc = jdbc;
        this.timeZone = timeZone;
    }

    @Override
    public List<OperationsCalendarEntry> findEntries(LocalDate from, LocalDate to) {
        List<OperationsCalendarEntry> entries = new ArrayList<>();
        Timestamp fromTimestamp = Timestamp.from(from.atStartOfDay(timeZone).toInstant());
        Timestamp toExclusive = Timestamp.from(to.plusDays(1).atStartOfDay(timeZone).toInstant());

        entries.addAll(jdbc.query("""
                SELECT i.id, i.status, i.start_time, i.end_time,
                       d.id AS device_id, d.name AS device_name,
                       s.id AS site_id, s.name AS site_name, i.notes
                FROM incidents i
                JOIN devices d ON d.id = i.device_id
                JOIN sites s ON s.id = COALESCE(i.site_id, d.site_id)
                WHERE i.start_time < ?
                  AND COALESCE(i.end_time, now()) >= ?
                ORDER BY i.start_time
                """, this::mapIncident, toExclusive, fromTimestamp));

        entries.addAll(jdbc.query("""
                SELECT id, project_name, plan_name, next_renewal_on,
                       reminder_days, notes
                FROM tuya_plans
                WHERE is_active = true
                  AND next_renewal_on BETWEEN ? AND ?
                ORDER BY next_renewal_on, project_name
                """, this::mapScheduledRenewal, from, to));

        entries.addAll(jdbc.query("""
                SELECT h.id, h.plan_id, p.project_name, p.plan_name,
                       h.renewed_on, h.notes
                FROM tuya_plan_renewal_history h
                JOIN tuya_plans p ON p.id = h.plan_id
                WHERE h.renewed_on BETWEEN ? AND ?
                ORDER BY h.renewed_on, p.project_name
                """, this::mapCompletedRenewal, from, to));

        entries.sort(Comparator
                .comparing(OperationsCalendarEntry::date)
                .thenComparing(OperationsCalendarEntry::title));
        return List.copyOf(entries);
    }

    @Override
    public List<TuyaPlan> findPlans() {
        return jdbc.query(PLAN_SELECT +
                " ORDER BY is_active DESC, next_renewal_on, project_name", this::mapPlan);
    }

    @Override
    public Optional<TuyaPlan> findPlan(long id) {
        return jdbc.query(PLAN_SELECT + " WHERE id = ?", this::mapPlan, id)
                .stream().findFirst();
    }

    @Override
    public TuyaPlan createPlan(
            String projectName,
            String planName,
            LocalDate lastRenewedOn,
            LocalDate nextRenewalOn,
            int renewalIntervalMonths,
            int reminderDays,
            String notes,
            boolean active
    ) {
        Long id = jdbc.queryForObject("""
                INSERT INTO tuya_plans (
                    project_name, plan_name, last_renewed_on, next_renewal_on,
                    renewal_interval_months, reminder_days, notes, is_active
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, projectName, planName, lastRenewedOn, nextRenewalOn,
                renewalIntervalMonths, reminderDays, notes, active);
        return findPlan(id).orElseThrow();
    }

    @Override
    public Optional<TuyaPlan> updatePlan(
            long id,
            String projectName,
            String planName,
            LocalDate lastRenewedOn,
            LocalDate nextRenewalOn,
            int renewalIntervalMonths,
            int reminderDays,
            String notes,
            boolean active
    ) {
        int updated = jdbc.update("""
                UPDATE tuya_plans
                SET project_name = ?, plan_name = ?, last_renewed_on = ?,
                    next_renewal_on = ?, renewal_interval_months = ?,
                    reminder_days = ?, notes = ?, is_active = ?
                WHERE id = ?
                """, projectName, planName, lastRenewedOn, nextRenewalOn,
                renewalIntervalMonths, reminderDays, notes, active, id);
        return updated == 0 ? Optional.empty() : findPlan(id);
    }

    @Override
    public boolean deletePlan(long id) {
        return jdbc.update("DELETE FROM tuya_plans WHERE id = ?", id) > 0;
    }

    @Override
    public TuyaPlan recordRenewal(
            TuyaPlan plan,
            LocalDate renewedOn,
            LocalDate nextRenewalOn,
            String notes
    ) {
        jdbc.update("""
                INSERT INTO tuya_plan_renewal_history (
                    plan_id, previous_date, renewed_on, next_renewal_on, notes
                ) VALUES (?, ?, ?, ?, ?)
                """, plan.id(), plan.nextRenewalOn(), renewedOn, nextRenewalOn, notes);
        jdbc.update("""
                UPDATE tuya_plans
                SET last_renewed_on = ?, next_renewal_on = ?
                WHERE id = ?
                """, renewedOn, nextRenewalOn, plan.id());
        return findPlan(plan.id()).orElseThrow();
    }

    private OperationsCalendarEntry mapIncident(ResultSet rs, int rowNum) throws SQLException {
        OffsetDateTime start = localTime(rs, "start_time");
        return new OperationsCalendarEntry(
                "incident:" + rs.getObject("id", UUID.class),
                "INCIDENT",
                "Corte · " + rs.getString("device_name"),
                start.toLocalDate(),
                start,
                localTime(rs, "end_time"),
                rs.getString("status"),
                rs.getObject("device_id", UUID.class),
                rs.getString("device_name"),
                rs.getObject("site_id", UUID.class),
                rs.getString("site_name"),
                null,
                rs.getString("notes"));
    }

    private OperationsCalendarEntry mapScheduledRenewal(ResultSet rs, int rowNum)
            throws SQLException {
        LocalDate date = rs.getObject("next_renewal_on", LocalDate.class);
        int reminderDays = rs.getInt("reminder_days");
        LocalDate today = LocalDate.now(timeZone);
        String status = date.isBefore(today)
                ? "OVERDUE"
                : !date.isAfter(today.plusDays(reminderDays)) ? "DUE_SOON" : "SCHEDULED";
        return new OperationsCalendarEntry(
                "tuya-renewal:" + rs.getLong("id"),
                "TUYA_RENEWAL",
                "Renovar Tuya · " + rs.getString("project_name"),
                date, null, null, status, null, null, null, null,
                rs.getLong("id"),
                joinDescription(rs.getString("plan_name"), rs.getString("notes")));
    }

    private OperationsCalendarEntry mapCompletedRenewal(ResultSet rs, int rowNum)
            throws SQLException {
        return new OperationsCalendarEntry(
                "tuya-renewed:" + rs.getLong("id"),
                "TUYA_RENEWED",
                "Tuya renovado · " + rs.getString("project_name"),
                rs.getObject("renewed_on", LocalDate.class),
                null, null, "COMPLETED", null, null, null, null,
                rs.getLong("plan_id"),
                joinDescription(rs.getString("plan_name"), rs.getString("notes")));
    }

    private TuyaPlan mapPlan(ResultSet rs, int rowNum) throws SQLException {
        return new TuyaPlan(
                rs.getLong("id"),
                rs.getString("project_name"),
                rs.getString("plan_name"),
                rs.getObject("last_renewed_on", LocalDate.class),
                rs.getObject("next_renewal_on", LocalDate.class),
                rs.getInt("renewal_interval_months"),
                rs.getInt("reminder_days"),
                rs.getString("notes"),
                rs.getBoolean("is_active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private OffsetDateTime localTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null
                ? null
                : timestamp.toInstant().atZone(timeZone).toOffsetDateTime();
    }

    private String joinDescription(String planName, String notes) {
        return notes == null || notes.isBlank() ? planName : planName + " · " + notes;
    }
}
