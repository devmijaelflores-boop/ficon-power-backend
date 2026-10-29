package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.MonitoringViews.EventView;
import com.devzonesoft.ficonpower.domain.PagedResult;
import java.util.List;

public interface EventQueryRepository {

    List<EventView> findEvents(int limit);

    PagedResult<EventView> findEventsPage(
            int page,
            int size,
            String search,
            String eventType,
            String deviceName,
            String siteName,
            String occurredDate
    );
}
