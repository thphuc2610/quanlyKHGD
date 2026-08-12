export type DashboardOverview = {
  teacherCount: number;
  classCount: number;
  totalStandardHours: number;
  guestClassCount: number;
};

export type TeacherWorkload = {
  teacherName: string;
  departmentPh: string;
  classCount: number;
  totalCredits: number;
  totalStudents: number;
  totalStandardHours: number;
};

export type TeacherWorkloadDetail = {
  teacherName: string;
  className: string;
  subjectName: string;
  credits: number;
  studentCount: number;
  departmentPh: string;
  unitName: string | null;
  coefficientK: number | null;
  coefficientTheory: number | null;
  coefficientPractice: number | null;
  standardHours: number;
  ruleName: string;
  academicYear: string;
  semester: string;
};

export type DepartmentWorkload = {
  departmentPh: string;
  teacherCount: number;
  classCount: number;
  totalStandardHours: number;
};

export type RuleSummary = {
  ruleCode: string;
  ruleName: string;
  classCount: number;
  totalStandardHours: number;
};

export type SubjectRuleConfig = {
  id?: number;
  code: string;
  name: string;
  coefficientTheoryFormula?: string | null;
  coefficientPracticeFormula?: string | null;
  formula: string;
  note?: string;
};
