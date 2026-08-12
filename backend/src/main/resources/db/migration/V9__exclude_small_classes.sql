DELETE FROM calculation_result
WHERE class_record_id IN (
  SELECT id
  FROM class_record
  WHERE student_count IS NOT NULL
    AND student_count < 10
);

UPDATE class_record
SET valid = FALSE,
    warning_message = 'Dòng dữ liệu không hợp lệ hoặc thiếu thông tin bắt buộc'
WHERE student_count IS NOT NULL
  AND student_count < 10;

UPDATE class_record
SET valid = TRUE,
    warning_message = NULL
WHERE credits IS NOT NULL
  AND credits > 0
  AND student_count IS NOT NULL
  AND student_count >= 10
  AND NULLIF(TRIM(subject_name), '') IS NOT NULL
  AND NULLIF(TRIM(teacher_original_name), '') IS NOT NULL
  AND valid = FALSE;
