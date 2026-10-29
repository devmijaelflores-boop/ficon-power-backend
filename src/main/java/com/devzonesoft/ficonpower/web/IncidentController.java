package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.domain.MonitoringViews.IncidentView;
import com.devzonesoft.ficonpower.domain.PagedResult;
import com.devzonesoft.ficonpower.ports.output.IncidentQueryRepository;
import com.devzonesoft.ficonpower.usecase.IncidentAttendanceService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentQueryRepository queries;
    private final IncidentAttendanceService attendance;

    public IncidentController(IncidentQueryRepository queries, IncidentAttendanceService attendance) {
        this.queries = queries;
        this.attendance = attendance;
    }

    @GetMapping
    public Object incidents(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit,
            @RequestParam Optional<@Min(0) Integer> page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String deviceName,
            @RequestParam(defaultValue = "") String siteName,
            @RequestParam Optional<UUID> incidentId,
            @RequestParam(defaultValue = "") String startDate,
            @RequestParam(defaultValue = "") String endDate
    ) {
        if (page.isPresent()) {
            return pagedIncidents(page.get(), size, search, status, deviceName, siteName,
                    incidentId.map(UUID::toString).orElse(""), startDate, endDate);
        }
        return queries.findIncidents(limit);
    }

    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<IncidentMutationView> acknowledge(@PathVariable UUID id) {
        if (!attendance.acknowledge(id)) {
            return ResponseEntity.status(409).body(new IncidentMutationView(
                    false,
                    "El incidente ya fue atendido, resuelto o no existe"));
        }
        return ResponseEntity.ok(new IncidentMutationView(true, "Incidente atendido"));
    }

    private PagedResult<IncidentView> pagedIncidents(
            int page,
            int size,
            String search,
            String status,
            String deviceName,
            String siteName,
            String incidentId,
            String startDate,
            String endDate
    ) {
        return queries.findIncidentsPage(
                page, size, search, status, deviceName, siteName, incidentId, startDate, endDate);
    }

    public record IncidentMutationView(boolean updated, String message) {
    }
}
