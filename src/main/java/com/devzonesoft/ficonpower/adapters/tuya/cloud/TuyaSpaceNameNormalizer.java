package com.devzonesoft.ficonpower.adapters.tuya.cloud;

import com.devzonesoft.ficonpower.domain.DevicePlacement;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TuyaSpaceNameNormalizer {

    // Tuya Spatial names are typed by operators, so accept the common ASCII
    // separators as well as the dash characters produced by mobile keyboards.
    private static final Pattern SEPARATOR = Pattern.compile(
            "\\s*[_\\-\\u2010-\\u2015\\u2212]+\\s*");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Map<String, String> CANONICAL_WORDS = Map.of(
            "timax", "TIMAX");
    private static final Map<String, String> DISTRICTS = Map.of(
            "puno", "Puno",
            "juliaca", "Juliaca",
            "putina", "Putina",
            "lampa", "Lampa");

    public Optional<DevicePlacement> normalize(String sourceName) {
        if (sourceName == null || sourceName.isBlank()) {
            return Optional.empty();
        }
        String compact = WHITESPACE.matcher(sourceName.trim()).replaceAll(" ");
        String[] segments = SEPARATOR.split(compact);
        if (segments.length < 2) {
            return Optional.empty();
        }
        String district = DISTRICTS.get(key(segments[segments.length - 1]));
        if (district == null) {
            return Optional.empty();
        }
        StringBuilder organization = new StringBuilder();
        for (int index = 0; index < segments.length - 1; index++) {
            String segment = segments[index].trim();
            if (!segment.isEmpty()) {
                if (!organization.isEmpty()) {
                    organization.append(" - ");
                }
                organization.append(titleCase(segment));
            }
        }
        if (organization.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DevicePlacement(
                organization + " - " + district,
                district,
                compact));
    }

    private static String titleCase(String value) {
        return Arrays.stream(WHITESPACE.split(value.trim().toLowerCase(Locale.ROOT)))
                .filter(word -> !word.isEmpty())
                .map(TuyaSpaceNameNormalizer::canonicalWord)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private static String canonicalWord(String word) {
        String canonical = CANONICAL_WORDS.get(key(word));
        if (canonical != null) {
            return canonical;
        }
        int first = word.codePointAt(0);
        int firstLength = Character.charCount(first);
        return new String(Character.toChars(Character.toTitleCase(first)))
                + word.substring(firstLength);
    }

    private static String key(String value) {
        String decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
