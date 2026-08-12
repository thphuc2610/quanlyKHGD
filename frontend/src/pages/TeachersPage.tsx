import { SearchOutlined } from '@ant-design/icons';
import { Card, Empty, Input, Progress, Radio, Space, Table, Tag, Typography } from 'antd';
import { useMemo, useState } from 'react';
import PageHeader from '../components/PageHeader';
import { sttColumn, widePagination } from '../constants/table';
import type { AuthUser } from '../features/auth/auth.types';
import { useTeacherWorkloadDetailsQuery, useTeacherWorkloadsQuery } from '../features/workloads/workloads.query';
import type { TeacherWorkload, TeacherWorkloadDetail } from '../features/workloads/workloads.types';
import { searchMatches } from '../utils/search';

type TeachersPageProps = {
  user: AuthUser;
};

type ViewMode = 'summary' | 'detail';

const formatNumber = (value?: number | null) => Number(value ?? 0).toLocaleString('vi-VN', { maximumFractionDigits: 2 });
const numberSorter = <T,>(selector: (record: T) => number | null | undefined) =>
  (left: T, right: T) => Number(selector(left) ?? 0) - Number(selector(right) ?? 0);
const hasSplitCoefficient = (record: TeacherWorkloadDetail) =>
  Number(record.coefficientTheory ?? 0) > 0 || Number(record.coefficientPractice ?? 0) > 0;
const renderTheoryCoefficient = (value: number | null | undefined, record: TeacherWorkloadDetail) =>
  hasSplitCoefficient(record)
    ? formatNumber(value)
    : { children: formatNumber(record.coefficientK), props: { colSpan: 2 } };
const renderPracticeCoefficient = (value: number | null | undefined, record: TeacherWorkloadDetail) =>
  hasSplitCoefficient(record)
    ? formatNumber(value)
    : { children: null, props: { colSpan: 0 } };

const getTeachingStatus = (academicYear?: string, semester?: string) => {
  const match = academicYear?.match(/(\d{4})\s*-\s*(\d{4})/);
  if (!match) {
    return 'Chưa hoàn thành';
  }
  const endYear = Number(match[2]);
  const finishMonth = semester === 'Kỳ 1' ? 1 : 8;
  return new Date() >= new Date(endYear, finishMonth - 1, 31) ? 'Hoàn thành' : 'Chưa hoàn thành';
};
const sameName = (left?: string | null, right?: string | null) =>
  (left ?? '').trim().toLowerCase() === (right ?? '').trim().toLowerCase();

export default function TeachersPage({ user }: TeachersPageProps) {
  const [keyword, setKeyword] = useState('');
  const [viewMode, setViewMode] = useState<ViewMode>('summary');
  const teachers = useTeacherWorkloadsQuery();
  const details = useTeacherWorkloadDetailsQuery();
  const isAdmin = user.roles.includes('ADMIN');
  const rows = teachers.data ?? [];
  const detailRows = details.data ?? [];
  const maxHours = Math.max(...rows.map((item) => item.totalStandardHours), 0);

  const scopedTeacherRows = useMemo(() => {
    return isAdmin
      ? rows
      : rows.filter((item) => sameName(item.teacherName, user.teacherName) || sameName(item.teacherName, user.fullName));
  }, [isAdmin, rows, user.fullName, user.teacherName]);

  const scopedDetailRows = useMemo(() => {
    return isAdmin
      ? detailRows
      : detailRows.filter((item) => sameName(item.teacherName, user.teacherName) || sameName(item.teacherName, user.fullName));
  }, [detailRows, isAdmin, user.fullName, user.teacherName]);

  const visibleRows = useMemo(() => {
    if (!keyword.trim()) {
      return scopedTeacherRows;
    }
    return scopedTeacherRows.filter((item) =>
      searchMatches(`${item.teacherName} ${item.departmentPh}`, keyword)
    );
  }, [keyword, scopedTeacherRows]);

  const visibleDetailRows = useMemo(() => {
    if (!keyword.trim()) {
      return scopedDetailRows;
    }
    return scopedDetailRows.filter((item) =>
      searchMatches(`${item.teacherName} ${item.className} ${item.subjectName} ${item.departmentPh} ${item.unitName ?? ''}`, keyword)
    );
  }, [keyword, scopedDetailRows]);

  return (
    <div className="page-stack">
      <PageHeader title={isAdmin ? 'Khối lượng giảng dạy' : 'Khối lượng của tôi'} />

      <Card title={viewMode === 'summary' ? 'Tổng hợp giảng dạy' : isAdmin ? 'Chi tiết từng giảng viên' : 'Chi tiết học phần'}>
        <Space direction="vertical" size={16} className="full-width">
          <Space wrap className="toolbar-row">
            <Radio.Group
              optionType="button"
              buttonStyle="solid"
              value={viewMode}
              onChange={(event) => setViewMode(event.target.value)}
              options={[
                { label: 'Tổng hợp giảng dạy', value: 'summary' },
                { label: isAdmin ? 'Chi tiết từng giảng viên' : 'Chi tiết học phần', value: 'detail' }
              ]}
            />
            <Input
              allowClear
              prefix={<SearchOutlined />}
              placeholder={viewMode === 'summary' ? 'Tìm theo giảng viên hoặc bộ môn' : 'Tìm theo học phần, bộ môn hoặc đơn vị'}
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              className="search-input"
            />
          </Space>

          {viewMode === 'summary' ? (
            !teachers.isLoading && visibleRows.length === 0 ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có dữ liệu tổng hợp" />
            ) : (
              <Table
                className="workload-table"
                rowKey={(record) => `${record.teacherName}-${record.departmentPh}`}
                tableLayout="fixed"
                scroll={{ x: 1120 }}
                dataSource={visibleRows}
                loading={teachers.isLoading}
                size="small"
                pagination={widePagination}
                columns={[
                  sttColumn,
                  { title: 'GV', dataIndex: 'teacherName', width: 210, className: 'text-left' },
                  { title: 'Bộ môn', dataIndex: 'departmentPh', width: 140, className: 'text-left' },
                  { title: 'Số lớp', dataIndex: 'classCount', width: 100 },
                  { title: 'Tổng TC', dataIndex: 'totalCredits', width: 100, render: formatNumber },
                  { title: 'Tổng SV', dataIndex: 'totalStudents', width: 110, render: formatNumber },
                  { title: 'Tổng tiết quy đổi', dataIndex: 'totalStandardHours', width: 150, render: formatNumber },
                  {
                    title: 'Tỷ trọng',
                    width: 180,
                    render: (_, record: TeacherWorkload) => (
                      <Progress
                        percent={maxHours > 0 ? Math.round((record.totalStandardHours / maxHours) * 100) : 0}
                        size="small"
                        strokeColor={{ '0%': '#168477', '100%': '#0b6258' }}
                        trailColor="rgba(11, 98, 88, 0.12)"
                      />
                    )
                  },
                  { title: 'Trạng thái', width: 120, render: () => <Tag color="green">Đã tính</Tag> }
                ]}
              />
            )
          ) : !details.isLoading && visibleDetailRows.length === 0 ? (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có dữ liệu chi tiết" />
          ) : (
            <Table
              className="workload-table"
              rowKey={(record, index) => `${record.teacherName}-${record.className}-${record.subjectName}-${index}`}
              tableLayout="fixed"
              scroll={{ x: isAdmin ? 1950 : 1750 }}
              dataSource={visibleDetailRows}
              loading={details.isLoading}
              size="small"
              pagination={widePagination}
              columns={[
                sttColumn,
                ...(isAdmin ? [{ title: 'GV', dataIndex: 'teacherName', width: 190, className: 'text-left' }] : []),
                { title: 'Lớp', dataIndex: 'className', width: 180, className: 'text-left' },
                { title: 'Học phần', dataIndex: 'subjectName', width: 300, className: 'text-left workload-subject-cell' },
                { title: 'Học kỳ', dataIndex: 'semester', width: 100 },
                {
                  title: 'Trạng thái',
                  width: 140,
                  render: (_, record) => getTeachingStatus(record.academicYear, record.semester)
                },
                { title: 'TC', dataIndex: 'credits', width: 80, render: formatNumber, sorter: numberSorter((record) => record.credits) },
                { title: 'SV', dataIndex: 'studentCount', width: 80, render: formatNumber, sorter: numberSorter((record) => record.studentCount) },
                { title: 'BM', dataIndex: 'departmentPh', width: 110, className: 'text-left' },
                { title: 'Đơn vị', dataIndex: 'unitName', width: 260, className: 'text-left' },
                {
                  title: '',
                  children: [
                    { title: 'K_LT', dataIndex: 'coefficientTheory', width: 100, render: renderTheoryCoefficient, sorter: numberSorter((record) => hasSplitCoefficient(record) ? record.coefficientTheory : record.coefficientK) },
                    { title: 'K_TH', dataIndex: 'coefficientPractice', width: 100, render: renderPracticeCoefficient, sorter: numberSorter((record) => record.coefficientPractice) }
                  ]
                },
                { title: 'Tiết quy đổi', dataIndex: 'standardHours', width: 130, render: formatNumber },
                { title: 'Quy tắc', dataIndex: 'ruleName', width: 220, className: 'text-left' }
              ]}
            />
          )}
        </Space>
      </Card>

      {!isAdmin && (
        <Card title="Ghi chú">
          <Typography.Text type="secondary">
            Tài khoản giảng viên chỉ hiển thị phần khối lượng gắn với tên giảng viên trong hồ sơ.
          </Typography.Text>
        </Card>
      )}
    </div>
  );
}
