import { useMutation } from '@tanstack/react-query';
import type { WorkloadReportParams } from './reports.api';
import { reportsService } from './reports.service';

export function useDownloadWorkloadReportMutation() {
  return useMutation({ mutationFn: (params?: WorkloadReportParams) => reportsService.downloadWorkloadReport(params) });
}
