package edu.tlu.klgd.domain.common.util;

import edu.tlu.klgd.domain.common.ImportConstant;
import java.util.Objects;
import org.springframework.web.multipart.MultipartFile;

public final class ExcelHelper {
    private ExcelHelper() {
    }

    public static boolean hasExcelFormat(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = Objects.requireNonNullElse(file.getOriginalFilename(), ImportConstant.EMPTY_TEXT).toLowerCase();
        return filename.endsWith(ImportConstant.EXCEL_XLSX_EXTENSION)
            || ImportConstant.EXCEL_XLSX_MEDIA_TYPE.equals(contentType);
    }
}
