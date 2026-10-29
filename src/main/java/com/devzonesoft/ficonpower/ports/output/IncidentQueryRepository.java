package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.MonitoringViews.IncidentView;
import com.devzonesoft.ficonpower.domain.PagedResult;
import java.util.List;

public interface IncidentQueryRepository {

    List<IncidentView> findIncidents(int limit);

    PagedResult<IncidentView> findIncidentsPage(
            int page,
            int size,
            String search,
            String status,
            String deviceName,
            String siteName,
            String incidentId,
            String startDate,
            String endDate
    );
}
