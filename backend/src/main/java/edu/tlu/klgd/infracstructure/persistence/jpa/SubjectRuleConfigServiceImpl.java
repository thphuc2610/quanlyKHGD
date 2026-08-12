package edu.tlu.klgd.infracstructure.persistence.jpa;

import edu.tlu.klgd.application.dto.SubjectRuleConfigDTO;
import edu.tlu.klgd.domain.entity.SubjectRuleConfig;
import edu.tlu.klgd.domain.repository.SubjectRuleConfigRepository;
import edu.tlu.klgd.domain.service.SubjectRuleConfigService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectRuleConfigServiceImpl implements SubjectRuleConfigService {
    private final SubjectRuleConfigRepository repository;

    public SubjectRuleConfigServiceImpl(SubjectRuleConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectRuleConfigDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public List<SubjectRuleConfigDTO> saveAll(List<SubjectRuleConfigDTO> rules) {
        List<SubjectRuleConfig> saved = rules.stream()
            .filter(rule -> rule.code() != null && !rule.code().isBlank())
            .map(this::upsert)
            .toList();
        return repository.saveAll(saved).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void deleteByCode(String code) {
        if (code != null && !code.isBlank()) {
            repository.deleteByCode(code.trim());
        }
    }

    private SubjectRuleConfig upsert(SubjectRuleConfigDTO dto) {
        SubjectRuleConfig entity = repository.findByCode(dto.code()).orElseGet(SubjectRuleConfig::new);
        entity.setCode(dto.code().trim());
        entity.setName(dto.name() == null || dto.name().isBlank() ? dto.code().trim() : dto.name().trim());
        entity.setCoefficientTheoryFormula(trimToNull(dto.coefficientTheoryFormula()));
        entity.setCoefficientPracticeFormula(trimToNull(dto.coefficientPracticeFormula()));
        entity.setFormula(dto.formula() == null ? "" : dto.formula().trim());
        entity.setNote(dto.note());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    private SubjectRuleConfigDTO toDto(SubjectRuleConfig entity) {
        return new SubjectRuleConfigDTO(
            entity.getId(),
            entity.getCode(),
            entity.getName(),
            entity.getCoefficientTheoryFormula(),
            entity.getCoefficientPracticeFormula(),
            entity.getFormula(),
            entity.getNote()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
