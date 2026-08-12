import type { TablePaginationConfig } from 'antd';

export const DEFAULT_PAGE_SIZE = 10;

export const COMMON_PAGE_SIZE_OPTIONS = [10, 20, 50];

export const commonPagination: TablePaginationConfig = {
  defaultPageSize: DEFAULT_PAGE_SIZE,
  showSizeChanger: true,
  pageSizeOptions: COMMON_PAGE_SIZE_OPTIONS.map(String),
  showTotal: (total, range) => `${range[0]}-${range[1]} / ${total} dòng`,
  locale: {
    items_per_page: 'dòng/trang'
  }
};

export const smallPagination: TablePaginationConfig = {
  ...commonPagination,
  defaultPageSize: 8
};

export const widePagination: TablePaginationConfig = {
  ...commonPagination,
  defaultPageSize: 12
};

export const sttColumn = {
  title: 'STT',
  key: 'stt',
  width: 72,
  align: 'center' as const,
  render: (_: unknown, __: unknown, index: number) => index + 1
};
