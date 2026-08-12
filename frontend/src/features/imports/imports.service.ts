import { deleteImportBatch, getImportBatches, getImportBatchRecords, postCalculateBatch, postImportBatch } from './imports.api';

export const importsService = {
  getBatches: getImportBatches,
  getRecords: getImportBatchRecords,
  uploadBatch: postImportBatch,
  calculateBatch: postCalculateBatch,
  deleteBatch: deleteImportBatch
};
