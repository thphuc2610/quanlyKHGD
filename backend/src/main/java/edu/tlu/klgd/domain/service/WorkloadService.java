package edu.tlu.klgd.domain.service;

import edu.tlu.klgd.application.dto.*;
import java.util.List;

public interface WorkloadService {
    int calculate(Long importBatchId, CalculationSettingsDTO settings);
    int recalculate(CalculationSettingsDTO settings);
    DashboardOverviewDTO overview(String username);
    List<TeacherWorkloadDTO> teachers();
    List<TeacherWorkloadDTO> teachers(String username);
    List<TeacherWorkloadDetailDTO> teacherDetails(String username);
    List<DepartmentWorkloadDTO> departments(String username);
    List<RuleSummaryDTO> rules(String username);
    List<TeacherOptionDTO> teacherOptions();
}
