package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.domain.Device;
import com.devzonesoft.ficonpower.domain.DeviceNotFoundException;
import com.devzonesoft.ficonpower.domain.MonitoringViews.DeviceView;
import com.devzonesoft.ficonpower.domain.MonitoringViews.DeviceDetailView;
import com.devzonesoft.ficonpower.ports.input.UpdateDeviceDetails;
import com.devzonesoft.ficonpower.ports.output.DeviceQueryRepository;
import com.devzonesoft.ficonpower.usecase.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceQueryRepository queries;
    private final UpdateDeviceDetails updateDeviceDetails;
    private final AuditLogService audit;

    public DeviceController(
            DeviceQueryRepository queries,
            UpdateDeviceDetails updateDeviceDetails,
            AuditLogService audit
    ) {
        this.queries = queries;
        this.updateDeviceDetails = updateDeviceDetails;
        this.audit = audit;
    }

    @GetMapping
    public List<DeviceView> devices() {
        return queries.findDevices();
    }

    @GetMapping("/{id}/detail")
    public DeviceDetailView detail(@PathVariable UUID id) {
        return queries.findDeviceDetail(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Dispositivo no encontrado: " + id));
    }

    @PutMapping("/{id}")
    public Device update(
            @PathVariable UUID id,
            @Valid @RequestBody DeviceDetailsRequest request,
            HttpServletRequest servletRequest
    ) {
        try {
            Device updated = updateDeviceDetails.update(id, request.name(), request.siteId());
            audit.record("device.update", "device", id.toString(),
                    "{\"name\":\"" + request.name() + "\",\"siteId\":\"" + request.siteId() + "\"}",
                    HttpRequestMetadata.from(servletRequest));
            return updated;
        } catch (DeviceNotFoundException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Dispositivo no encontrado: " + id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La sede indicada no existe: " + request.siteId());
        }
    }

    public record DeviceDetailsRequest(
            @NotBlank @Size(max = 150) String name,
            @NotNull UUID siteId
    ) {
    }
}
