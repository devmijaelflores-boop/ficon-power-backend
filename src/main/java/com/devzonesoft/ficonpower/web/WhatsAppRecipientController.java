package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.domain.WhatsAppRecipient;
import com.devzonesoft.ficonpower.domain.WhatsAppTestResult;
import com.devzonesoft.ficonpower.usecase.AuditLogService;
import com.devzonesoft.ficonpower.usecase.WhatsAppRecipientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/whatsapp-recipients")
public class WhatsAppRecipientController {

    private final WhatsAppRecipientService recipients;
    private final AuditLogService audit;

    public WhatsAppRecipientController(WhatsAppRecipientService recipients, AuditLogService audit) {
        this.recipients = recipients;
        this.audit = audit;
    }

    @GetMapping
    public List<WhatsAppRecipient> findAll() {
        return recipients.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WhatsAppRecipient create(
            @Valid @RequestBody RecipientRequest request,
            HttpServletRequest servletRequest
    ) {
        WhatsAppRecipient created = recipients.create(
                request.label(), request.phone(), request.siteId(),
                request.notifyOutage(), request.notifyRestored(), request.active());
        record("whatsapp_recipient.create", created, servletRequest);
        return created;
    }

    @PutMapping("/{id}")
    public WhatsAppRecipient update(
            @PathVariable UUID id,
            @Valid @RequestBody RecipientRequest request,
            HttpServletRequest servletRequest
    ) {
        WhatsAppRecipient updated = recipients.update(
                id, request.label(), request.phone(), request.siteId(),
                request.notifyOutage(), request.notifyRestored(), request.active());
        record("whatsapp_recipient.update", updated, servletRequest);
        return updated;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, HttpServletRequest servletRequest) {
        recipients.delete(id);
        audit.record("whatsapp_recipient.delete", "whatsapp_recipient", id.toString(),
                "{}", HttpRequestMetadata.from(servletRequest));
    }

    @PostMapping("/{id}/test")
    public WhatsAppTestResult test(@PathVariable UUID id, HttpServletRequest servletRequest) {
        WhatsAppTestResult result = recipients.test(id);
        audit.record("whatsapp_recipient.test", "whatsapp_recipient", id.toString(),
                "{\"accepted\":" + result.accepted() + "}",
                HttpRequestMetadata.from(servletRequest));
        return result;
    }

    private void record(String action, WhatsAppRecipient recipient, HttpServletRequest request) {
        String scope = recipient.siteId() == null ? "global" : recipient.siteId().toString();
        audit.record(action, "whatsapp_recipient", recipient.id().toString(),
                "{\"scope\":\"" + scope + "\"}", HttpRequestMetadata.from(request));
    }

    public record RecipientRequest(
            @NotBlank @Size(max = 120) String label,
            @NotBlank @Size(max = 30) String phone,
            UUID siteId,
            boolean notifyOutage,
            boolean notifyRestored,
            boolean active
    ) {
    }
}
