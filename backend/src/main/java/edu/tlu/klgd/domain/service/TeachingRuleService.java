package edu.tlu.klgd.domain.service;

import edu.tlu.klgd.application.dto.CalculationSettingsDTO;
import edu.tlu.klgd.application.dto.RuleResultDTO;
import edu.tlu.klgd.domain.entity.ClassRecord;

public interface TeachingRuleService {
    RuleResultDTO calculate(ClassRecord record, CalculationSettingsDTO settings);
}
