ALTER TABLE subject_rule_config
    ADD COLUMN IF NOT EXISTS coefficient_theory_formula VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS coefficient_practice_formula VARCHAR(1000);
