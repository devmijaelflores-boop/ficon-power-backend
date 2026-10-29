package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.OperationsCalendarEntry;
import com.devzonesoft.ficonpower.domain.TuyaPlan;
import com.devzonesoft.ficonpower.ports.output.OperationsCalendarRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OperationsCalendarService {

    static final int MAX_RANGE_DAYS = 370;

    private final OperationsCalendarRepository calendar;
    private final Clock clock;

    public OperationsCalendarService(OperationsCalendarRepository calendar, Clock clock) {
        this.calendar = calendar;
        this.clock = clock;
    }

    public List<OperationsCalendarEntry> entries(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw badRequest("El rango del calendario no es válido");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw badRequest("El calendario permite consultar como máximo 370 días");
        }
        return calendar.findEntries(from, to);
    }

    public List<TuyaPlan> plans() {
        return calendar.findPlans();
    }

    public TuyaPlan create(
            String projectName,
            String planName,
            LocalDate lastRenewedOn,
            LocalDate nextRenewalOn,
            int intervalMonths,
            int reminderDays,
            String notes,
            boolean active
    ) {
        validate(projectName, planName, lastRenewedOn, nextRenewalOn,
                intervalMonths, reminderDays, notes);
        try {
            return calendar.createPlan(trim(projectName), trim(planName), lastRenewedOn,
                    nextRenewalOn, intervalMonths, reminderDays, nullableTrim(notes), active);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Ya existe un proyecto Tuya con ese nombre");
        }
    }

    public TuyaPlan update(
            long id,
            String projectName,
            String planName,
            LocalDate lastRenewedOn,
            LocalDate nextRenewalOn,
            int intervalMonths,
            int reminderDays,
            String notes,
            boolean active
    ) {
        validate(projectName, planName, lastRenewedOn, nextRenewalOn,
                intervalMonths, reminderDays, notes);
        TuyaPlan current = calendar.findPlan(id).orElseThrow(this::notFound);
        if (!Objects.equals(lastRenewedOn, current.lastRenewedOn())
                || !nextRenewalOn.equals(current.nextRenewalOn())) {
            throw badRequest("Las fechas del plan provienen de Tuya y no se pueden editar");
        }
        try {
            return calendar.updatePlan(id, trim(projectName), trim(planName),
                            current.lastRenewedOn(), current.nextRenewalOn(), 6, reminderDays,
                            nullableTrim(notes), active)
                    .orElseThrow(() -> notFound());
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Ya existe un proyecto Tuya con ese nombre");
        }
    }

    public void delete(long id) {
        if (!calendar.deletePlan(id)) throw notFound();
    }

    @Transactional
    public TuyaPlan renew(long id, LocalDate renewedOn, String notes) {
        TuyaPlan plan = calendar.findPlan(id).orElseThrow(this::notFound);
        LocalDate actualRenewal = renewedOn == null ? LocalDate.now(clock) : renewedOn;
        if (actualRenewal.isAfter(LocalDate.now(clock))) {
            throw badRequest("La fecha de renovación no puede estar en el futuro");
        }
        if (notes != null && notes.trim().length() > 500) {
            throw badRequest("Las notas no pueden superar los 500 caracteres");
        }
        LocalDate base = actualRenewal.isAfter(plan.nextRenewalOn())
                ? actualRenewal : plan.nextRenewalOn();
        LocalDate next = base.plusMonths(plan.renewalIntervalMonths());
        return calendar.recordRenewal(plan, actualRenewal, next, nullableTrim(notes));
    }

    private void validate(
            String projectName,
            String planName,
            LocalDate lastRenewedOn,
            LocalDate nextRenewalOn,
            int intervalMonths,
            int reminderDays,
            String notes
    ) {
        if (projectName == null || projectName.isBlank() || trim(projectName).length() > 120) {
            throw badRequest("El nombre del proyecto es obligatorio y admite hasta 120 caracteres");
        }
        if (planName == null || planName.isBlank() || trim(planName).length() > 120) {
            throw badRequest("El nombre del plan es obligatorio y admite hasta 120 caracteres");
        }
        if (nextRenewalOn == null) {
            throw badRequest("La próxima renovación es obligatoria");
        }
        if (lastRenewedOn != null && nextRenewalOn.isBefore(lastRenewedOn)) {
            throw badRequest("La próxima renovación debe ser posterior a la última renovación");
        }
        if (intervalMonths != 6) {
            throw badRequest("El plan gratuito de Tuya se renueva cada 6 meses");
        }
        if (reminderDays < 0 || reminderDays > 365) {
            throw badRequest("La anticipación debe estar entre 0 y 365 días");
        }
        if (notes != null && notes.trim().length() > 500) {
            throw badRequest("Las notas no pueden superar los 500 caracteres");
        }
    }

    private String trim(String value) {
        return value.trim();
    }

    private String nullableTrim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan Tuya no encontrado");
    }
}
