import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workloadsService } from './workloads.service';
import type { CalculationSettings } from '../imports/imports.types';
import type { SubjectRuleConfig } from './workloads.types';

export const workloadQueryKeys = {
  overview: ['workloads', 'overview'] as const,
  teachers: ['workloads', 'teachers'] as const,
  teacherDetails: ['workloads', 'teacher-details'] as const,
  departments: ['workloads', 'departments'] as const,
  rules: ['workloads', 'rules'] as const,
  subjectRules: ['workloads', 'subject-rules'] as const
};

export function useDashboardOverviewQuery() {
  return useQuery({ queryKey: workloadQueryKeys.overview, queryFn: workloadsService.getOverview });
}

export function useTeacherWorkloadsQuery() {
  return useQuery({ queryKey: workloadQueryKeys.teachers, queryFn: workloadsService.getTeachers });
}

export function useTeacherWorkloadDetailsQuery() {
  return useQuery({ queryKey: workloadQueryKeys.teacherDetails, queryFn: workloadsService.getTeacherDetails });
}

export function useDepartmentWorkloadsQuery() {
  return useQuery({ queryKey: workloadQueryKeys.departments, queryFn: workloadsService.getDepartments });
}

export function useRuleSummariesQuery() {
  return useQuery({ queryKey: workloadQueryKeys.rules, queryFn: workloadsService.getRules });
}

export function useSubjectRuleConfigsQuery() {
  return useQuery({ queryKey: workloadQueryKeys.subjectRules, queryFn: workloadsService.getSubjectRules });
}

export function useSaveSubjectRuleConfigsMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (rules: SubjectRuleConfig[]) => workloadsService.saveSubjectRules(rules),
    onSuccess: (rules) => {
      queryClient.setQueryData(workloadQueryKeys.subjectRules, rules);
      queryClient.invalidateQueries({ queryKey: workloadQueryKeys.rules });
    }
  });
}

export function useDeleteSubjectRuleConfigMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (code: string) => workloadsService.deleteSubjectRule(code),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workloadQueryKeys.subjectRules });
      queryClient.invalidateQueries({ queryKey: workloadQueryKeys.rules });
    }
  });
}

export function useRecalculateWorkloadsMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (settings: CalculationSettings) => workloadsService.recalculate(settings),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workloadQueryKeys.overview });
      queryClient.invalidateQueries({ queryKey: workloadQueryKeys.teachers });
      queryClient.invalidateQueries({ queryKey: workloadQueryKeys.teacherDetails });
      queryClient.invalidateQueries({ queryKey: workloadQueryKeys.departments });
      queryClient.invalidateQueries({ queryKey: workloadQueryKeys.rules });
    }
  });
}
