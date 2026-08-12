package edu.tlu.klgd.domain.common;

public final class TextConstant {
    private TextConstant() {
    }

    public static final String EMPTY = "";
    public static final String SPACE = " ";
    public static final String MARK_REGEX = "\\p{M}";
    public static final String NON_ALPHANUMERIC_REGEX = "[^a-z0-9]+";
    public static final String MULTIPLE_WHITESPACE_REGEX = "\\s+";
    public static final char VIETNAMESE_LOWER_D_WITH_STROKE = 'đ';
    public static final char VIETNAMESE_UPPER_D_WITH_STROKE = 'Đ';
    public static final char LATIN_LOWER_D = 'd';
    public static final char LATIN_UPPER_D = 'D';
}
