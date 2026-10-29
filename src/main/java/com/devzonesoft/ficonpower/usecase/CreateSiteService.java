package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.MonitoringViews.SiteView;
import com.devzonesoft.ficonpower.ports.input.CreateSite;
import com.devzonesoft.ficonpower.ports.output.SiteRepository;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class CreateSiteService implements CreateSite {

    static final int MAX_NAME_LENGTH = 150;
    static final int MAX_DISTRICT_LENGTH = 100;
    private static final Pattern LOCATION_SEPARATOR = Pattern.compile("\\s+-\\s+");

    private final SiteRepository siteRepository;

    public CreateSiteService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Override
    public SiteView create(String name, String district, String province) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
        String normalizedName = name.trim();
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "El nombre no puede superar los " + MAX_NAME_LENGTH + " caracteres");
        }

        String normalizedDistrict = normalize(district);
        if (normalizedDistrict == null) {
            normalizedDistrict = inferDistrict(normalizedName);
        }
        if (normalizedDistrict != null && normalizedDistrict.length() > MAX_DISTRICT_LENGTH) {
            throw new IllegalArgumentException(
                    "El distrito no puede superar los " + MAX_DISTRICT_LENGTH + " caracteres");
        }

        return siteRepository.create(
                normalizedName,
                normalizedDistrict,
                normalize(province));
    }

    static String inferDistrict(String siteName) {
        Matcher separators = LOCATION_SEPARATOR.matcher(siteName);
        int districtStart = -1;
        while (separators.find()) {
            districtStart = separators.end();
        }
        if (districtStart < 0 || districtStart >= siteName.length()) {
            return null;
        }
        return normalize(siteName.substring(districtStart));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
