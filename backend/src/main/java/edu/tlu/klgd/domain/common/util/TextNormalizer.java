package edu.tlu.klgd.domain.common.util;

import edu.tlu.klgd.domain.common.TextConstant;
import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

public final class TextNormalizer {

    private static final int REGEX_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS;

    // --- Patterns for Lecturer Name Cleaning ---
    private static final Pattern NOTE_PATTERN = Pattern.compile("\\s*\\([^)]*\\)");
    private static final Pattern ACADEMIC_PREFIX_PATTERN = Pattern.compile("^\\s*(?:(?:GS|PGS|TS|ThS|CN|KS)\\.?\\s*)+", REGEX_FLAGS);
    private static final Pattern DEPARTMENT_TAIL_PATTERN = Pattern.compile("\\s+(?:b[oộ\\p{L}\\p{M}]+|p[oô\\p{L}\\p{M}]+|2[°o]?|b0|p0)?\\s*m[oô\\p{L}\\p{M}]+n\\b.*$", REGEX_FLAGS);
    private static final Pattern WATER_RESOURCES_TAIL_PATTERN = Pattern.compile("\\s+(?:t[aà\\p{L}\\p{M}]*i\\s+nguy[eêéèẻẽẹ\\p{L}\\p{M}]+n|nguy[eêéèẻẽẹ\\p{L}\\p{M}]+n\\s+n[uư][\\p{L}\\p{M}]*).*$", REGEX_FLAGS);
    private static final Pattern DEPARTMENT_KEYWORDS_TAIL_PATTERN = Pattern.compile("\\s+(?:k[yỹ]\\s+thu[aậ]t|c[oô]ng\\s+tr[iì]nh|th[uủ]y\\s+v[aă]n|m[oô]i\\s+tr[uư][oơ]ng|đ[iị]a\\s+ch[aấ]t|c[oơ]\\s+đ[iị][eê]n|kinh\\s+t[eế]|c[oô]ng\\s+ngh[eệ]\\s+th[oô]ng\\s+tin|gi[aá]o\\s+d[uụ]c\\s+th[eể]\\s+ch[aấ]t|ng[oô]n\\s+ng[uữ]\\s+anh|ng[oô]n\\s+ng[uữ]\\s+trung)\\b.*$", REGEX_FLAGS);
    private static final Pattern DASH_NOTE_TAIL_PATTERN = Pattern.compile("\\s*[-–—/]\\s*(?:bộ\\s+môn|bm|kỹ\\s+thực|kỹ\\s+thuật|công\\s+trình|tài\\s+nguyên|nguyên\\s+nước|nguyén\\s+nước|đồng\\s+tháp|cs2|phmn|dhtl|hn|ph|môi\\s+trường|thủy\\s+văn|kinh\\s+tế|cntt).*$", REGEX_FLAGS);
    private static final Pattern CLASS_CODE_TAIL_PATTERN = Pattern.compile("\\s+S\\d{2}[-–\\s]*.*$", REGEX_FLAGS);

    private static final List<Pattern> TEACHER_TAIL_PATTERNS = List.of(
        DEPARTMENT_TAIL_PATTERN,
        WATER_RESOURCES_TAIL_PATTERN,
        DEPARTMENT_KEYWORDS_TAIL_PATTERN,
        DASH_NOTE_TAIL_PATTERN,
        CLASS_CODE_TAIL_PATTERN
    );

    // --- Canonical Department Registry ---
    private record DepartmentRule(List<String> keywords, String canonicalName) {
        boolean matches(String normalizedText) {
            return keywords.stream().anyMatch(normalizedText::contains);
        }
    }

    private static final List<DepartmentRule> DEPARTMENT_RULES = List.of(
        new DepartmentRule(List.of("ky thuat cong trinh", "ktct"), "Bộ môn Kỹ thuật công trình"),
        new DepartmentRule(List.of("ky thuat tai nguyen nuoc", "tai nguyen nuoc", "kttnn"), "Bộ môn Kỹ thuật tài nguyên nước"),
        new DepartmentRule(List.of("giao duc the chat", "gdtc"), "Bộ môn Giáo dục thể chất"),
        new DepartmentRule(List.of("ngon ngu anh", "tieng anh"), "Bộ môn Ngôn ngữ Anh"),
        new DepartmentRule(List.of("ngon ngu trung", "tieng trung"), "Bộ môn Ngôn ngữ Trung Quốc"),
        new DepartmentRule(List.of("thuy van"), "Bộ môn Thủy văn và Tài nguyên nước"),
        new DepartmentRule(List.of("co dien"), "Bộ môn Cơ điện"),
        new DepartmentRule(List.of("cong nghe thong tin", "cntt"), "Bộ môn Công nghệ thông tin"),
        new DepartmentRule(List.of("kinh te"), "Bộ môn Kinh tế"),
        new DepartmentRule(List.of("xay dung"), "Bộ môn Xây dựng")
    );

    private TextNormalizer() {
    }

    /**
     * Normalizes accents and special characters into a plain lowercase ASCII key.
     */
    public static String normalize(String value) {
        if (value == null) {
            return TextConstant.EMPTY;
        }
        return Normalizer.normalize(
                value.replace(TextConstant.VIETNAMESE_LOWER_D_WITH_STROKE, TextConstant.LATIN_LOWER_D)
                    .replace(TextConstant.VIETNAMESE_UPPER_D_WITH_STROKE, TextConstant.LATIN_UPPER_D),
                Normalizer.Form.NFD
            )
            .replaceAll(TextConstant.MARK_REGEX, TextConstant.EMPTY)
            .toLowerCase()
            .replaceAll(TextConstant.NON_ALPHANUMERIC_REGEX, TextConstant.SPACE)
            .trim()
            .replaceAll(TextConstant.MULTIPLE_WHITESPACE_REGEX, TextConstant.SPACE);
    }

    /**
     * Cleans lecturer names by stripping academic titles, parenthetical notes, department tails, and class codes.
     */
    public static String cleanTeacherName(String value) {
        if (value == null || value.isBlank()) {
            return TextConstant.EMPTY;
        }

        String cleaned = NOTE_PATTERN.matcher(value).replaceAll(TextConstant.EMPTY);
        cleaned = ACADEMIC_PREFIX_PATTERN.matcher(cleaned).replaceAll(TextConstant.EMPTY);

        for (Pattern tailPattern : TEACHER_TAIL_PATTERNS) {
            cleaned = tailPattern.matcher(cleaned).replaceAll(TextConstant.EMPTY);
        }

        cleaned = cleaned.replaceAll("^[^\\p{L}]+", TextConstant.EMPTY)
            .replaceAll("[^\\p{L}]+$", TextConstant.EMPTY)
            .trim()
            .replaceAll(TextConstant.MULTIPLE_WHITESPACE_REGEX, TextConstant.SPACE);

        return fixVietnameseDiacritics(toTitleCase(cleaned));
    }

    /**
     * Normalizes department names into standard canonical forms or cleans up noisy suffixes.
     */
    public static String cleanDepartmentName(String value) {
        if (value == null || value.isBlank()) {
            return TextConstant.EMPTY;
        }

        String cleaned = value.replaceAll("\\s+", TextConstant.SPACE).trim();
        String normalized = normalize(cleaned);

        for (DepartmentRule rule : DEPARTMENT_RULES) {
            if (rule.matches(normalized)) {
                return rule.canonicalName();
            }
        }

        cleaned = cleaned.replaceAll("\\s*\\([^)]*\\)", TextConstant.EMPTY)
            .replaceAll("(?iu)\\s*[-–—].*$", TextConstant.EMPTY)
            .replaceAll("(?iu)\\s+S\\d{2}[-–\\s]*.*$", TextConstant.EMPTY)
            .replaceAll("[^\\p{L}\\d\\s]+$", TextConstant.EMPTY)
            .replaceAll("\\s+\\d+$", TextConstant.EMPTY)
            .trim();

        if (cleaned.isBlank()) {
            return TextConstant.EMPTY;
        }

        String normCleaned = normalize(cleaned);
        cleaned = toTitleCase(cleaned);
        
        // Ensure "Bộ môn " is capitalized properly and not duplicated
        if (!normCleaned.startsWith("bo mon") && !cleaned.equalsIgnoreCase("KHCB") && !cleaned.equalsIgnoreCase("KTCT")) {
            return "Bộ môn " + cleaned;
        } else if (normCleaned.startsWith("bo mon ")) {
            // If it already contains 'bộ môn' but maybe case is wrong, enforce it
            return "Bộ môn " + cleaned.substring(7).trim();
        }
        return cleaned;
    }

    private static String fixVietnameseDiacritics(String text) {
        if (text.isBlank()) {
            return text;
        }
        String firstToken = text.split("\\s+", 2)[0];
        if (normalize(firstToken).equals("le") && !firstToken.equalsIgnoreCase("Le")) {
            return text.replaceFirst("^\\S+", "Lê");
        }
        return text;
    }

    public static String toTitleCase(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toTitleCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
}
