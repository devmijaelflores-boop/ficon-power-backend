package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.domain.MonitoringViews.EventView;
import com.devzonesoft.ficonpower.domain.PagedResult;
import com.devzonesoft.ficonpower.ports.output.EventQueryRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Optional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventQueryRepository queries;

    public EventController(EventQueryRepository queries) {
        this.queries = queries;
    }

    @GetMapping
    public Object events(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit,
            @RequestParam Optional<@Min(0) Integer> page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String eventType,
            @RequestParam(defaultValue = "") String deviceName,
            @RequestParam(defaultValue = "") String siteName,
            @RequestParam(defaultValue = "") String occurredDate
    ) {
        if (page.isPresent()) {
            return pagedEvents(page.get(), size, search, eventType, deviceName, siteName, occurredDate);
        }
        return queries.findEvents(limit);
    }

    private PagedResult<EventView> pagedEvents(
            int page,
            int size,
            String search,
            String eventType,
            String deviceName,
            String siteName,
            String occurredDate
    ) {
        return queries.findEventsPage(page, size, search, eventType, deviceName, siteName, occurredDate);
    }
}
