import { API_ENDPOINTS } from '../../constants/endpoints';
import { api } from '../../lib/api';

export type WorkloadReportParams = {
  academicYear?: string;
  semester?: string;
};

export async function getWorkloadReportExcel(params?: WorkloadReportParams) {
  const { data } = await api.get<Blob>(API_ENDPOINTS.reports.workloadExcel, { params, responseType: 'blob' });
  return data;
}
