import type { UploadFile } from 'antd';

export type ImportBatch = {
  id: number;
  fileName: string;
  academicYear: string;
  semester: string;
  status: string;
  createdAt: string;
  totalRows: number;
  validRows: number;
  warningRows: number;
};

export type ClassRecord = {
  id: number;
  academicYear: string;
  semester: string;
  className: string;
  subjectName: string;
  credits: number | null;
  departmentHn: string;
  departmentPh: string;
  studentCount: number | null;
  teacherName: string;
  position: string;
  degree: string;
  academicTitle: string;
  valid: boolean;
  warningMessage: string | null;
};

export type UploadImportBatchForm = {
  academicYear: string;
  semester: string;
  file: {
    fileList: UploadFile[];
  };
};

export type CalculateBatchResponse = {
  calculatedRows: number;
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

export type CalculationSettings = {
  detectGuestByPosition: boolean;
  graduationInternshipWeeks: number;
  defaultInternshipDays: number;
  surveyingInternshipDays: number;
  materialTheoryHours?: number;
  materialLabHours?: number;
  soilTheoryHours?: number;
  soilLabHours?: number;
  geotechnicalTheoryHours?: number;
  geotechnicalLabHours?: number;
  labGroupSize?: number;
  labBaseK?: number;
  labBaseStudents?: number;
  labIncrementPerStudent?: number;
  labMinK?: number;
  labMaxK?: number;
  theoryBaseK?: number;
  theoryBaseStudents?: number;
  theoryIncrementPerStudent?: number;
  theoryMinK?: number;
  theoryMaxK?: number;
  subjectRules?: SubjectRuleConfig[];
};
