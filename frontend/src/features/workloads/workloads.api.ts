import { API_ENDPOINTS } from '../../constants/endpoints';
import { api } from '../../lib/api';
import type { CalculationSettings } from '../imports/imports.types';
import type { DashboardOverview, DepartmentWorkload, RuleSummary, SubjectRuleConfig, TeacherWorkload, TeacherWorkloadDetail } from './workloads.types';

export async function getDashboardOverview() {
  const { data } = await api.get<DashboardOverview>(API_ENDPOINTS.dashboard.overview);
  return data;
}

export async function getTeacherWorkloads() {
  const { data } = await api.get<TeacherWorkload[]>(API_ENDPOINTS.workloads.teachers);
  return data;
}

export async function getTeacherWorkloadDetails() {
  const { data } = await api.get<TeacherWorkloadDetail[]>(API_ENDPOINTS.workloads.teacherDetails);
  return data;
}

export async function getDepartmentWorkloads() {
  const { data } = await api.get<DepartmentWorkload[]>(API_ENDPOINTS.workloads.departments);
  return data;
}

export async function getRuleSummaries() {
  const { data } = await api.get<RuleSummary[]>(API_ENDPOINTS.workloads.rules);
  return data;
}

export async function getSubjectRuleConfigs() {
  const { data } = await api.get<SubjectRuleConfig[]>(API_ENDPOINTS.workloads.subjectRules);
  return data;
}

export async function putSubjectRuleConfigs(rules: SubjectRuleConfig[]) {
  const { data } = await api.put<SubjectRuleConfig[]>(API_ENDPOINTS.workloads.subjectRules, rules);
  return data;
}

export async function deleteSubjectRuleConfig(code: string) {
  await api.delete(API_ENDPOINTS.workloads.subjectRule(code));
}

export async function recalculateWorkloads(settings: CalculationSettings) {
  const { data } = await api.post<{ calculatedRows: number }>(API_ENDPOINTS.workloads.recalculate, settings);
  return data;
}
