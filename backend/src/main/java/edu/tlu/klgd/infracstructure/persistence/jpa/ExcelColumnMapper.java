package edu.tlu.klgd.infracstructure.persistence.jpa;

import edu.tlu.klgd.domain.common.ImportConstant;
import edu.tlu.klgd.domain.common.util.TextNormalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ExcelColumnMapper {
    Map<String, Integer> mapColumns(List<String> headers) {
        return mapColumns(headers, List.of());
    }

    Map<String, Integer> mapColumns(List<String> headers, List<List<String>> sampleRows) {
        HashMap<String, Integer> mapped = new HashMap<>();
        ImportConstant.REQUIRED_COLUMN_ALIASES.forEach((field, aliases) -> mapped.put(field, find(field, headers, aliases, sampleRows)));
        ImportConstant.OPTIONAL_COLUMN_ALIASES.forEach((field, aliases) -> mapped.put(field, find(field, headers, aliases, sampleRows)));
        return mapped;
    }

    List<String> missingRequired(Map<String, Integer> mapped) {
        return ImportConstant.REQUIRED_COLUMN_ALIASES.keySet().stream()
            .filter(key -> mapped.getOrDefault(key, ImportConstant.INVALID_COLUMN_INDEX) < ImportConstant.MIN_VALID_COLUMN_INDEX)
            .sorted()
            .toList();
    }

    private int find(String field, List<String> headers, List<String> aliases, List<List<String>> sampleRows) {
        Candidate best = new Candidate(ImportConstant.INVALID_COLUMN_INDEX, 0);
        for (String alias : aliases) {
            String aliasKey = TextNormalizer.normalize(alias);
            for (int i = 0; i < headers.size(); i++) {
                String headerKey = TextNormalizer.normalize(headers.get(i));
                int score = matchScore(headerKey, aliasKey);
                if (score > 0 && !looksLikeExpectedData(field, i, sampleRows)) {
                    score -= 40;
                }
                if (score > best.score()) {
                    best = new Candidate(i, score);
                }
            }
        }
        return best.score() > 0 ? best.index() : ImportConstant.INVALID_COLUMN_INDEX;
    }

    private int matchScore(String headerKey, String aliasKey) {
        if (headerKey.equals(aliasKey)) {
            return aliasKey.length() <= 3 ? 90 : 120;
        }
        if (aliasKey.length() >= 5 && headerKey.contains(aliasKey)) {
            return 70;
        }
        return 0;
    }

    private boolean looksLikeExpectedData(String field, int columnIndex, List<List<String>> sampleRows) {
        if (sampleRows.isEmpty()) {
            return true;
        }
        if (ImportConstant.STUDENT_COUNT_FIELD.equals(field) || ImportConstant.CREDITS_FIELD.equals(field)) {
            return numericSampleCount(columnIndex, sampleRows) > 0;
        }
        if (ImportConstant.CLASS_NAME_FIELD.equals(field)
            || ImportConstant.SUBJECT_NAME_FIELD.equals(field)
            || ImportConstant.TEACHER_ORIGINAL_NAME_FIELD.equals(field)) {
            return textSampleCount(columnIndex, sampleRows) > 0;
        }
        return true;
    }

    private int numericSampleCount(int columnIndex, List<List<String>> sampleRows) {
        int count = 0;
        for (List<String> row : sampleRows) {
            if (columnIndex >= row.size()) {
                continue;
            }
            String value = row.get(columnIndex);
            if (value != null && !value.isBlank() && parseDouble(value) != null) {
                count++;
            }
        }
        return count;
    }

    private int textSampleCount(int columnIndex, List<List<String>> sampleRows) {
        int count = 0;
        for (List<String> row : sampleRows) {
            if (columnIndex >= row.size()) {
                continue;
            }
            String value = row.get(columnIndex);
            if (value != null && !value.isBlank() && parseDouble(value) == null) {
                count++;
            }
        }
        return count;
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value.replace(ImportConstant.DECIMAL_COMMA, ImportConstant.DECIMAL_DOT));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record Candidate(int index, int score) {
    }
}
