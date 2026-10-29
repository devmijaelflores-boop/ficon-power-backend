package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.domain.MonitoringViews.SiteView;
import com.devzonesoft.ficonpower.ports.input.CreateSite;
import com.devzonesoft.ficonpower.ports.output.SiteQueryRepository;
import com.devzonesoft.ficonpower.usecase.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteQueryRepository queries;
    private final CreateSite createSite;
    private final AuditLogService audit;

    public SiteController(SiteQueryRepository queries, CreateSite createSite, AuditLogService audit) {
        this.queries = queries;
        this.createSite = createSite;
        this.audit = audit;
    }

    @GetMapping
    public List<SiteView> sites() {
        return queries.findSites();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SiteView create(@Valid @RequestBody SiteRequest request, HttpServletRequest servletRequest) {
        try {
            SiteView site = createSite.create(request.name(), request.district(), request.province());
            audit.record("site.create", "site", site.id().toString(),
                    "{\"name\":\"" + request.name() + "\"}",
                    HttpRequestMetadata.from(servletRequest));
            return site;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Ya existe una sede con ese nombre: " + request.name());
        }
    }

    public record SiteRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 100) String district,
            @Size(max = 100) String province
    ) {
    }
}
