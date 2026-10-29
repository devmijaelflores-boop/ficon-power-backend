package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.OperationsCalendarEntry;
import com.devzonesoft.ficonpower.domain.TuyaPlan;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OperationsCalendarRepository {

    List<OperationsCalendarEntry> findEntries(LocalDate from, LocalDate to);

    List<TuyaPlan> findPlans();

    Optional<TuyaPlan> findPlan(long id);

    TuyaPlan createPlan(
            String projectName,
            String planName,
            LocalDate lastRenewedOn,
            LocalDate nextRenewalOn,
            int renewalIntervalMonths,
            int reminderDays,
            String notes,
            boolean active);

    Optional<TuyaPlan> updatePlan(
            long id,
            String projectName,
            String planName,
            LocalDate lastRenewedOn,
            LocalDate nextRenewalOn,
            int renewalIntervalMonths,
            int reminderDays,
            String notes,
            boolean active);

    boolean deletePlan(long id);

    TuyaPlan recordRenewal(
            TuyaPlan plan,
            LocalDate renewedOn,
            LocalDate nextRenewalOn,
            String notes);
}
