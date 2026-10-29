package com.devzonesoft.ficonpower.ports.input;

import com.devzonesoft.ficonpower.domain.MonitoringViews.SiteView;

public interface CreateSite {

    SiteView create(String name, String district, String province);
}
