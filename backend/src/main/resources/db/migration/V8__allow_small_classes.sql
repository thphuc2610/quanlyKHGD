UPDATE class_record
SET valid = TRUE,
    warning_message = NULL
WHERE credits IS NOT NULL
  AND credits > 0
  AND student_count IS NOT NULL
  AND student_count >= 1
  AND subject_name IS NOT NULL
  AND trim(subject_name) <> ''
  AND teacher_original_name IS NOT NULL
  AND trim(teacher_original_name) <> ''
  AND valid = FALSE;
