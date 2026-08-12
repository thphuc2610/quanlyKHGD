export const API_ENDPOINTS = {
  auth: {
    login: '/api/auth/login',
    me: '/api/auth/me',
    changePassword: '/api/auth/change-password',
    forgotPassword: '/api/auth/forgot-password',
    resetPassword: '/api/auth/reset-password'
  },
  admin: {
    users: '/api/admin/users',
    user: (id: number) => `/api/admin/users/${id}`,
    teacherOptions: '/api/admin/teacher-options'
  },
  dashboard: {
    overview: '/api/dashboard/overview'
  },
  workloads: {
    teachers: '/api/workloads/teachers',
    teacherDetails: '/api/workloads/teacher-details',
    departments: '/api/workloads/departments',
    rules: '/api/workloads/rules',
    recalculate: '/api/workloads/recalculate',
    subjectRules: '/api/workloads/subject-rules',
    subjectRule: (code: string) => `/api/workloads/subject-rules/${encodeURIComponent(code)}`
  },
  imports: {
    batches: '/api/import-batches',
    batch: (id: number) => `/api/import-batches/${id}`,
    records: (id: number) => `/api/import-batches/${id}/records`,
    calculate: (id: number) => `/api/import-batches/${id}/calculate`
  },
  reports: {
    workloadExcel: '/api/reports/workload.xlsx'
  }
} as const;
