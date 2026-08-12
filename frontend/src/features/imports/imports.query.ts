import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { message } from 'antd';
import { importsService } from './imports.service';
import type { ImportBatch, UploadImportBatchForm } from './imports.types';

export const importQueryKeys = {
  batches: ['imports', 'batches'] as const,
  records: (id?: number) => ['imports', 'records', id] as const
};

export function useImportBatchesQuery() {
  return useQuery({
    queryKey: importQueryKeys.batches,
    queryFn: importsService.getBatches,
    staleTime: 0,
    refetchOnMount: 'always'
  });
}

export function useImportBatchRecordsQuery(id?: number) {
  return useQuery({
    queryKey: importQueryKeys.records(id),
    queryFn: () => importsService.getRecords(id as number),
    enabled: Boolean(id)
  });
}

export function useUploadImportBatchMutation(onSuccess?: (batch: ImportBatch) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (values: UploadImportBatchForm) => {
      const file = values.file.fileList[0].originFileObj;
      if (!file) {
        throw new Error('Chưa chọn file Excel');
      }
      return importsService.uploadBatch(file, values.academicYear, values.semester);
    },
    onSuccess: (batch) => {
      message.success('Nhập file thành công');
      onSuccess?.(batch);
      queryClient.invalidateQueries({ queryKey: importQueryKeys.batches });
    }
  });
}

export function useCalculateBatchMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: importsService.calculateBatch,
    onSuccess: () => {
      message.success('Đã chạy tính KLGD');
      queryClient.invalidateQueries();
    }
  });
}

export function useDeleteImportBatchMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: importsService.deleteBatch,
    onSuccess: () => {
      message.success('Đã xóa lần nhập dữ liệu');
      queryClient.invalidateQueries();
    }
  });
}
