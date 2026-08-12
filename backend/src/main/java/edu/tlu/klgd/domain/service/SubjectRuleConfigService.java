package edu.tlu.klgd.domain.service;

import edu.tlu.klgd.application.dto.SubjectRuleConfigDTO;
import java.util.List;

public interface SubjectRuleConfigService {
    List<SubjectRuleConfigDTO> findAll();

    List<SubjectRuleConfigDTO> saveAll(List<SubjectRuleConfigDTO> rules);

    void deleteByCode(String code);
}
