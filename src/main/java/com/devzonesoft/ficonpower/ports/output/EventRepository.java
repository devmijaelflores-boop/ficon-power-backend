package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.Event;

public interface EventRepository {

    void save(Event event);
}
