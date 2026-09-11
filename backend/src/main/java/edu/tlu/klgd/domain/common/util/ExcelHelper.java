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

    public static boolean hasSupportedImportFormat(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = Objects.requireNonNullElse(file.getOriginalFilename(), ImportConstant.EMPTY_TEXT).toLowerCase();
        return hasExcelFormat(file)
            || filename.endsWith(ImportConstant.PDF_EXTENSION)
            || ImportConstant.PDF_MEDIA_TYPE.equals(contentType);
    }

    public static boolean hasPdfFormat(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = Objects.requireNonNullElse(file.getOriginalFilename(), ImportConstant.EMPTY_TEXT).toLowerCase();
        return filename.endsWith(ImportConstant.PDF_EXTENSION)
            || ImportConstant.PDF_MEDIA_TYPE.equals(contentType);
    }
}
