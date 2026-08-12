import { API_ENDPOINTS } from '../../constants/endpoints';
import { api } from '../../lib/api';
import type { CalculateBatchResponse, CalculationSettings, ClassRecord, ImportBatch } from './imports.types';

export async function getImportBatches() {
  const { data } = await api.get<ImportBatch[]>(API_ENDPOINTS.imports.batches);
  return data;
}

export async function postImportBatch(file: File, academicYear: string, semester: string) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('academicYear', academicYear);
  formData.append('semester', semester);
  const { data } = await api.post<ImportBatch>(API_ENDPOINTS.imports.batches, formData);
  return data;
}

export async function getImportBatchRecords(id: number) {
  const { data } = await api.get<ClassRecord[]>(API_ENDPOINTS.imports.records(id));
  return data;
}

export async function postCalculateBatch(input: { id: number; settings: CalculationSettings }) {
  const { data } = await api.post<CalculateBatchResponse>(API_ENDPOINTS.imports.calculate(input.id), input.settings);
  return data;
}

export async function deleteImportBatch(id: number) {
  await api.delete(API_ENDPOINTS.imports.batch(id));
}
