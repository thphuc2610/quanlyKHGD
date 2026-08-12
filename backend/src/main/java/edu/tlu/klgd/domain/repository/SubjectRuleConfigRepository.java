package edu.tlu.klgd.domain.repository;

import edu.tlu.klgd.domain.entity.SubjectRuleConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRuleConfigRepository extends JpaRepository<SubjectRuleConfig, Long> {
    Optional<SubjectRuleConfig> findByCode(String code);
    void deleteByCode(String code);
}
