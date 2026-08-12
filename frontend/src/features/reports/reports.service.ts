import { getWorkloadReportExcel, type WorkloadReportParams } from './reports.api';

const toFilePart = (value?: string, fallback = 'tat-ca') => {
  const normalized = (value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9-]+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '');
  return normalized || fallback;
};

const buildWorkloadReportFileName = (params?: WorkloadReportParams) => {
  const academicYear = toFilePart(params?.academicYear);
  const semester = toFilePart(params?.semester);
  return `bao-cao-khoi-luong-giang-day_${academicYear}_${semester}.xlsx`;
};

export const reportsService = {
  async downloadWorkloadReport(params?: WorkloadReportParams) {
    const blob = await getWorkloadReportExcel(params);
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = buildWorkloadReportFileName(params);
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }
};
