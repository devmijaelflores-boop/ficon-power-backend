package com.devzonesoft.ficonpower.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.TuyaPlan;
import com.devzonesoft.ficonpower.ports.output.OperationsCalendarRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class OperationsCalendarServiceTest {

    private final OperationsCalendarRepository calendar =
            mock(OperationsCalendarRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-09-14T17:00:00Z"),
            ZoneId.of("America/Lima"));
    private final OperationsCalendarService service =
            new OperationsCalendarService(calendar, clock);

    @Test
    void rejectsRangesLongerThanMaximum() {
        assertThrows(ResponseStatusException.class, () -> service.entries(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 8)));
    }

    @Test
    void trimsPlanFieldsBeforeCreatingIt() {
        TuyaPlan stored = plan(LocalDate.of(2026, 10, 1));
        when(calendar.createPlan(
                "Ficon Puno", "Plan gratuito", null,
                LocalDate.of(2026, 10, 1), 6, 30, "Referencia", true))
                .thenReturn(stored);

        assertEquals(stored, service.create(
                "  Ficon Puno  ", " Plan gratuito ", null,
                LocalDate.of(2026, 10, 1), 6, 30, " Referencia ", true));
    }

    @Test
    void recordsRenewalAndAdvancesFromExistingDueDateWhenRenewedEarly() {
        TuyaPlan current = plan(LocalDate.of(2026, 9, 20));
        TuyaPlan renewed = plan(LocalDate.of(2027, 3, 20));
        when(calendar.findPlan(7L)).thenReturn(Optional.of(current));
        when(calendar.recordRenewal(
                current,
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2027, 3, 20),
                "Factura 123"))
                .thenReturn(renewed);

        TuyaPlan result = service.renew(
                7L, LocalDate.of(2026, 9, 14), " Factura 123 ");

        assertEquals(7L, result.id());
        assertEquals(LocalDate.of(2027, 3, 20), result.nextRenewalOn());
        verify(calendar).recordRenewal(
                current,
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2027, 3, 20),
                "Factura 123");
    }

    @Test
    void rejectsFutureRenewal() {
        when(calendar.findPlan(7L)).thenReturn(Optional.of(
                plan(LocalDate.of(2026, 9, 20))));

        assertThrows(ResponseStatusException.class, () -> service.renew(
                7L, LocalDate.of(2026, 9, 15), null));
    }

    @Test
    void keepsTuyaDatesImmutableWhenEditingPlanMetadata() {
        TuyaPlan current = plan(LocalDate.of(2026, 9, 20));
        when(calendar.findPlan(7L)).thenReturn(Optional.of(current));

        assertThrows(ResponseStatusException.class, () -> service.update(
                7L,
                "Ficon Puno",
                "Plan gratuito",
                LocalDate.of(2026, 3, 21),
                LocalDate.of(2026, 9, 20),
                6,
                30,
                null,
                true));
    }

    @Test
    void rejectsRenewalIntervalsOtherThanSixMonths() {
        assertThrows(ResponseStatusException.class, () -> service.create(
                "Ficon Puno",
                "Plan gratuito",
                LocalDate.of(2026, 3, 20),
                LocalDate.of(2026, 9, 20),
                12,
                30,
                null,
                true));
    }

    private TuyaPlan plan(LocalDate nextRenewalOn) {
        return new TuyaPlan(
                7L,
                "Ficon Puno",
                "Plan gratuito",
                LocalDate.of(2026, 3, 20),
                nextRenewalOn,
                6,
                30,
                null,
                true,
                Instant.parse("2026-03-20T12:00:00Z"),
                Instant.parse("2026-03-20T12:00:00Z"));
    }
}
