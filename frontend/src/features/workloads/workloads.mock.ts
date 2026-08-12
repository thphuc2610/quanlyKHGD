import type { DashboardOverview, DepartmentWorkload, RuleSummary, TeacherWorkload } from './workloads.types';

export const workloadMock = {
  overview: {
    teacherCount: 0,
    classCount: 0,
    totalStandardHours: 0,
    guestClassCount: 0
  } satisfies DashboardOverview,
  teachers: [] satisfies TeacherWorkload[],
  departments: [] satisfies DepartmentWorkload[],
  rules: [] satisfies RuleSummary[]
};
