package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.MonitoringViews.SiteView;

public interface SiteRepository {

    SiteView create(String name, String district, String province);
}
