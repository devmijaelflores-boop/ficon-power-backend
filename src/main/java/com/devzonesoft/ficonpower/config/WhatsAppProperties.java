package com.devzonesoft.ficonpower.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications.whatsapp")
public class WhatsAppProperties {

    private String apiUrl = "";
    private String phone = "";
    private String token = "";
    private String templateName = "";
    private String templateLanguage = "en";
    private String outageTemplateName = "";
    private String outageTemplateParams = "";
    private String restoredTemplateName = "";
    private String restoredTemplateParams = "";

    public boolean isConfigured() {
        return hasText(apiUrl) && hasText(token);
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateLanguage() {
        return templateLanguage;
    }

    public void setTemplateLanguage(String templateLanguage) {
        this.templateLanguage = templateLanguage;
    }

    public String getOutageTemplateName() {
        return outageTemplateName;
    }

    public void setOutageTemplateName(String outageTemplateName) {
        this.outageTemplateName = outageTemplateName;
    }

    public String getOutageTemplateParams() {
        return outageTemplateParams;
    }

    public void setOutageTemplateParams(String outageTemplateParams) {
        this.outageTemplateParams = outageTemplateParams;
    }

    public String getRestoredTemplateName() {
        return restoredTemplateName;
    }

    public void setRestoredTemplateName(String restoredTemplateName) {
        this.restoredTemplateName = restoredTemplateName;
    }

    public String getRestoredTemplateParams() {
        return restoredTemplateParams;
    }

    public void setRestoredTemplateParams(String restoredTemplateParams) {
        this.restoredTemplateParams = restoredTemplateParams;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
