import {
  deleteSubjectRuleConfig,
  getDashboardOverview,
  getDepartmentWorkloads,
  getRuleSummaries,
  getSubjectRuleConfigs,
  putSubjectRuleConfigs,
  recalculateWorkloads,
  getTeacherWorkloadDetails,
  getTeacherWorkloads
} from './workloads.api';

export const workloadsService = {
  getOverview: getDashboardOverview,
  getTeachers: getTeacherWorkloads,
  getTeacherDetails: getTeacherWorkloadDetails,
  getDepartments: getDepartmentWorkloads,
  getRules: getRuleSummaries,
  getSubjectRules: getSubjectRuleConfigs,
  saveSubjectRules: putSubjectRuleConfigs,
  deleteSubjectRule: deleteSubjectRuleConfig,
  recalculate: recalculateWorkloads
};
