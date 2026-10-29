package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.MonitoringViews.SiteView;
import java.util.List;

public interface SiteQueryRepository {

    List<SiteView> findSites();
}
