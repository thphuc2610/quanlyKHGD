package edu.tlu.klgd.domain.common.util;

import edu.tlu.klgd.domain.common.TextConstant;
import java.text.Normalizer;

public final class TextNormalizer {
    private TextNormalizer() {
    }

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
}
