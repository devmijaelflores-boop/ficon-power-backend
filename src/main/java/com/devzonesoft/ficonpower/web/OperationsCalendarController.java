package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.domain.OperationsCalendarEntry;
import com.devzonesoft.ficonpower.domain.TuyaPlan;
import com.devzonesoft.ficonpower.usecase.AuditLogService;
import com.devzonesoft.ficonpower.usecase.OperationsCalendarService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
public class OperationsCalendarController {

    private final OperationsCalendarService calendar;
    private final AuditLogService audit;

    public OperationsCalendarController(
            OperationsCalendarService calendar,
            AuditLogService audit
    ) {
        this.calendar = calendar;
        this.audit = audit;
    }

    @GetMapping
    public List<OperationsCalendarEntry> entries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return calendar.entries(from, to);
    }

    @GetMapping("/tuya-plans")
    public List<TuyaPlan> plans() {
        return calendar.plans();
    }

    @PostMapping("/tuya-plans")
    @ResponseStatus(HttpStatus.CREATED)
    public TuyaPlan create(
            @Valid @RequestBody PlanRequest request,
            HttpServletRequest servletRequest
    ) {
        TuyaPlan plan = calendar.create(
                request.projectName(), request.planName(), request.lastRenewedOn(),
                request.nextRenewalOn(), request.renewalIntervalMonths(),
                request.reminderDays(), request.notes(), request.active());
        record("tuya_plan.create", plan.id(), servletRequest);
        return plan;
    }

    @PutMapping("/tuya-plans/{id}")
    public TuyaPlan update(
            @PathVariable long id,
            @Valid @RequestBody PlanRequest request,
            HttpServletRequest servletRequest
    ) {
        TuyaPlan plan = calendar.update(
                id, request.projectName(), request.planName(), request.lastRenewedOn(),
                request.nextRenewalOn(), request.renewalIntervalMonths(),
                request.reminderDays(), request.notes(), request.active());
        record("tuya_plan.update", id, servletRequest);
        return plan;
    }

    @PostMapping("/tuya-plans/{id}/renew")
    public TuyaPlan renew(
            @PathVariable long id,
            @Valid @RequestBody RenewalRequest request,
            HttpServletRequest servletRequest
    ) {
        TuyaPlan plan = calendar.renew(id, request.renewedOn(), request.notes());
        record("tuya_plan.renew", id, servletRequest);
        return plan;
    }

    @DeleteMapping("/tuya-plans/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, HttpServletRequest servletRequest) {
        calendar.delete(id);
        record("tuya_plan.delete", id, servletRequest);
    }

    private void record(String action, long id, HttpServletRequest request) {
        audit.record(action, "tuya_plan", Long.toString(id), "{}",
                HttpRequestMetadata.from(request));
    }

    public record PlanRequest(
            @NotBlank @Size(max = 120) String projectName,
            @NotBlank @Size(max = 120) String planName,
            LocalDate lastRenewedOn,
            @NotNull LocalDate nextRenewalOn,
            @Min(1) @Max(60) int renewalIntervalMonths,
            @Min(0) @Max(365) int reminderDays,
            @Size(max = 500) String notes,
            boolean active
    ) {
    }

    public record RenewalRequest(
            LocalDate renewedOn,
            @Size(max = 500) String notes
    ) {
    }
}
