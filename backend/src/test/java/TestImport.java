import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.*;
import java.text.Normalizer;
import java.util.regex.Pattern;

public class TestImport {
    public static void main(String[] args) throws Exception {
        try (FileInputStream fis = new FileInputStream("tailieu/input/DS HPTN-cuoi K1.xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
             
            System.out.println("Workbook loaded. Sheets: " + workbook.getNumberOfSheets());
            Sheet sheet = workbook.getSheetAt(0);
            System.out.println("Physical rows: " + sheet.getPhysicalNumberOfRows());
            
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            int firstRow = sheet.getFirstRowNum();
            System.out.println("First row: " + firstRow);
            
            for (int rowIndex = firstRow; rowIndex <= Math.min(sheet.getLastRowNum(), firstRow + 20); rowIndex++) {
                Row headerRow = sheet.getRow(rowIndex);
                if (headerRow == null) continue;
                
                List<String> headers = new ArrayList<>();
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (cell == null) {
                        headers.add("");
                    } else {
                        headers.add(new DataFormatter().formatCellValue(cell).trim().replaceAll("\\s+", " "));
                    }
                }
                
                // Let's check headers
                if (!headers.isEmpty() && headers.size() > 5) {
                    System.out.println("Row " + rowIndex + " headers: " + headers.subList(0, Math.min(6, headers.size())));
                    
                    // check mapping
                    System.out.println("MSSV index: " + findGraduationProjectColumn(headers, List.of("mssv", "msv", "ma sinh vien", "ma sv")));
                    System.out.println("Teacher index: " + findGraduationProjectColumn(headers, List.of("gv huong dan 1", "giang vien huong dan 1", "can bo huong dan", "gv huong dan")));
                }
            }
        }
    }
    
    private static int findGraduationProjectColumn(List<String> headers, List<String> aliases) {
        int bestIndex = -1;
        int bestScore = 0;
        for (String alias : aliases) {
            String aliasKey = normalize(alias);
            for (int i = 0; i < headers.size(); i++) {
                String headerKey = normalize(headers.get(i));
                int score = headerKey.equals(aliasKey) ? 120 : (headerKey.contains(aliasKey) ? 70 : 0);
                if (score > bestScore) {
                    bestIndex = i;
                    bestScore = score;
                }
            }
        }
        return bestScore > 0 ? bestIndex : -1;
    }
    
    public static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(
                value.replace('đ', 'd').replace('Đ', 'D'),
                Normalizer.Form.NFD
            )
            .replaceAll("\\p{M}", "")
            .toLowerCase()
            .replaceAll("[^a-z0-9]+", " ")
            .trim()
            .replaceAll("\\s+", " ");
    }
}
