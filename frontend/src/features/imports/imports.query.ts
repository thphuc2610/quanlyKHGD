import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { message } from 'antd';
import type { RcFile } from 'antd/es/upload/interface';
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
      const files: RcFile[] = values.file.fileList.flatMap((item) => item.originFileObj ? [item.originFileObj] : []);
      if (files.length === 0) {
        throw new Error('Chưa chọn file Excel/PDF');
      }
      const batches: ImportBatch[] = [];
      for (const file of files) {
        batches.push(await importsService.uploadBatch(file, values.academicYear, values.semester));
      }
      return batches;
    },
    onSuccess: (batches) => {
      message.success(`Đã nhập ${batches.length} file thành công`);
      onSuccess?.(batches[batches.length - 1]);
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
