import { DeleteOutlined, DownloadOutlined, EyeOutlined, InboxOutlined, ReloadOutlined, UploadOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Drawer, Form, Input, message, Popconfirm, Select, Space, Table, Tabs, Tag, Upload } from 'antd';
import ReactECharts from 'echarts-for-react';
import { useEffect, useMemo, useState } from 'react';
import * as XLSX from 'xlsx';
import type { TermFilter } from '../components/AppShell';
import { smallPagination, sttColumn, widePagination } from '../constants/table';
import { buildCalculationSettings, DEFAULT_WORKLOAD_RULES, readSystemSettings, readWorkloadRules } from '../constants/workloadConfig';
import {
  useCalculateBatchMutation,
  useDeleteImportBatchMutation,
  useImportBatchRecordsQuery,
  useImportBatchesQuery,
  useUploadImportBatchMutation
} from '../features/imports/imports.query';
import type { UploadImportBatchForm } from '../features/imports/imports.types';
import { useSubjectRuleConfigsQuery, useTeacherWorkloadDetailsQuery } from '../features/workloads/workloads.query';
import type { TeacherWorkloadDetail } from '../features/workloads/workloads.types';
import { searchMatches } from '../utils/search';

const statusColor: Record<string, string> = {
  UPLOADED: 'cyan',
  CALCULATED: 'green',
  FAILED: 'red',
  WARNING: 'orange'
};

const statusLabel: Record<string, string> = {
  UPLOADED: 'Đã nhập',
  CALCULATED: 'Đã tính',
  FAILED: 'Lỗi',
  WARNING: 'Có cảnh báo'
};

const semesterOptions = [
  { value: 'Kỳ 1', label: 'Kỳ 1' },
  { value: 'Kỳ 2', label: 'Kỳ 2' },
  { value: 'Cả năm', label: 'Cả năm' }
];

const ALL_YEAR_SEMESTER = semesterOptions[2].value;
const formatNumber = (value?: number | null) => Number(value ?? 0).toLocaleString('vi-VN', { maximumFractionDigits: 2 });
const numberSorter = <T,>(selector: (record: T) => number | null | undefined) =>
  (left: T, right: T) => Number(selector(left) ?? 0) - Number(selector(right) ?? 0);
const hasSplitCoefficient = (record: TeacherWorkloadDetail) =>
  Number(record.coefficientTheory ?? 0) > 0 || Number(record.coefficientPractice ?? 0) > 0;
const renderGeneralCoefficient = (value: number | null | undefined, record: TeacherWorkloadDetail) =>
  hasSplitCoefficient(record) || value == null ? '' : formatNumber(value);
const renderTheoryCoefficient = (value: number | null | undefined, record: TeacherWorkloadDetail) =>
  hasSplitCoefficient(record) ? formatNumber(value) : '';
const renderPracticeCoefficient = (value: number | null | undefined, record: TeacherWorkloadDetail) =>
  hasSplitCoefficient(record) ? formatNumber(value) : '';

const getTeachingStatus = (academicYear?: string, semester?: string) => {
  const match = academicYear?.match(/(\d{4})\s*-\s*(\d{4})/);
  if (!match) {
    return 'Chưa hoàn thành';
  }
  const endYear = Number(match[2]);
  const finishMonth = semester === 'Kỳ 1' ? 1 : 8;
  return new Date() >= new Date(endYear, finishMonth - 1, 31) ? 'Hoàn thành' : 'Chưa hoàn thành';
};
const sameText = (left?: string, right?: string) => (left ?? '').trim() === (right ?? '').trim();

type TeacherSummaryRow = {
  teacherName: string;
  subjectName: string;
  classCount: number;
  totalCredits: number;
  totalStudents: number;
  totalStandardHours: number;
};

type DepartmentSummaryRow = {
  departmentPh: string;
  teacherCount: number;
  classCount: number;
  totalStandardHours: number;
};

type ImportPageProps = {
  termFilter: TermFilter;
  onTermFilterChange: (termFilter: TermFilter) => void;
};

export default function ImportPage({ termFilter, onTermFilterChange }: ImportPageProps) {
  const settings = readSystemSettings();
  const defaultAcademicYear = termFilter.academicYear || settings.find((item) => item.key === 'academicYear')?.value || '2025-2026';
  const defaultSemester = termFilter.semester || settings.find((item) => item.key === 'semester')?.value || 'Kỳ 1';
  const [selectedBatchId, setSelectedBatchId] = useState<number>();
  const [selectedFileName, setSelectedFileName] = useState<string>();
  const [batchStatus, setBatchStatus] = useState<string>();
  const [detailKeyword, setDetailKeyword] = useState('');
  const [selectedTeacher, setSelectedTeacher] = useState<string>();
  const [selectedSubject, setSelectedSubject] = useState<string>();
  const [selectedRule, setSelectedRule] = useState<string>();
  const [form] = Form.useForm();
  const batches = useImportBatchesQuery();
  const records = useImportBatchRecordsQuery(selectedBatchId);
  const details = useTeacherWorkloadDetailsQuery();
  const subjectRules = useSubjectRuleConfigsQuery();
  const uploadMutation = useUploadImportBatchMutation((batch) => {
    form.resetFields();
    setSelectedFileName(undefined);
    onTermFilterChange({ academicYear: batch.academicYear, semester: batch.semester });
    setSelectedBatchId(batch.id);
  });
  const calculateMutation = useCalculateBatchMutation();
  const deleteMutation = useDeleteImportBatchMutation();

  useEffect(() => {
    void batches.refetch();
  }, [batches.refetch, termFilter.academicYear, termFilter.semester]);

  const visibleBatches = useMemo(() => {
    const selectedYear = (termFilter.academicYear || '').trim();
    const selectedSemester = (termFilter.semester || '').trim();
    return (batches.data ?? []).filter((batch) => {
      const matchYear = !selectedYear || sameText(batch.academicYear, selectedYear);
      const matchSemester = !selectedSemester || sameText(selectedSemester, ALL_YEAR_SEMESTER) || sameText(batch.semester, selectedSemester);
      const matchStatus = !batchStatus || batch.status === batchStatus;
      return matchYear && matchSemester && matchStatus;
    });
  }, [batchStatus, batches.data, termFilter.academicYear, termFilter.semester]);

  const periodDetails = useMemo(() => {
    return (details.data ?? []).filter((item) => {
      const matchYear = !termFilter.academicYear || item.academicYear === termFilter.academicYear;
      const matchSemester = !termFilter.semester || termFilter.semester === ALL_YEAR_SEMESTER || item.semester === termFilter.semester;
      return matchYear && matchSemester;
    });
  }, [details.data, termFilter.academicYear, termFilter.semester]);

  const teacherOptions = useMemo(() => Array.from(new Set(periodDetails.map((item) => item.teacherName)))
    .sort((left, right) => left.localeCompare(right, 'vi'))
    .map((teacherName) => ({ label: teacherName, value: teacherName })), [periodDetails]);

  const subjectOptions = useMemo(() => Array.from(new Set(periodDetails.map((item) => item.subjectName).filter(Boolean)))
    .sort((left, right) => left.localeCompare(right, 'vi'))
    .map((subject) => ({ label: subject, value: subject })), [periodDetails]);

  const ruleOptions = useMemo(() => {
    const ruleNames = new Set<string>();
    DEFAULT_WORKLOAD_RULES.forEach((rule) => ruleNames.add(rule.name));
    readWorkloadRules().forEach((rule) => ruleNames.add(rule.name));
    (subjectRules.data ?? []).forEach((rule) => ruleNames.add(rule.name));
    periodDetails.forEach((item) => {
      if (item.ruleName) {
        ruleNames.add(item.ruleName);
      }
    });
    return Array.from(ruleNames)
      .sort((left, right) => left.localeCompare(right, 'vi'))
      .map((ruleName) => ({ label: ruleName, value: ruleName }));
  }, [periodDetails, subjectRules.data]);

  const filteredDetails = useMemo(() => {
    const keyword = detailKeyword.trim().toLowerCase();
    return periodDetails.filter((item) => {
      const ruleName = item.ruleName || 'Chưa xác định';
      const matchKeyword = !keyword || searchMatches(`${item.teacherName} ${item.className} ${item.subjectName} ${item.departmentPh} ${item.unitName ?? ''}`, keyword);
      const matchTeacher = !selectedTeacher || selectedTeacher === item.teacherName;
      const matchSubject = !selectedSubject || selectedSubject === item.subjectName;
      const matchRule = !selectedRule || selectedRule === ruleName;
      return matchKeyword && matchTeacher && matchSubject && matchRule;
    });
  }, [detailKeyword, periodDetails, selectedRule, selectedSubject, selectedTeacher]);

  const teacherSummaryRows = useMemo<TeacherSummaryRow[]>(() => {
    const grouped = new Map<string, TeacherSummaryRow>();
    filteredDetails.forEach((item) => {
      const key = `${item.teacherName}__${item.subjectName}`;
      const current = grouped.get(key) ?? {
        teacherName: item.teacherName,
        subjectName: item.subjectName,
        classCount: 0,
        totalCredits: 0,
        totalStudents: 0,
        totalStandardHours: 0
      };
      current.classCount += 1;
      current.totalCredits += item.credits ?? 0;
      current.totalStudents += item.studentCount ?? 0;
      current.totalStandardHours += item.standardHours ?? 0;
      grouped.set(key, current);
    });
    return Array.from(grouped.values())
      .map((item) => ({
        ...item,
        totalCredits: Math.round(item.totalCredits * 100) / 100,
        totalStandardHours: Math.round(item.totalStandardHours * 100) / 100
      }))
      .sort((left, right) => right.totalStandardHours - left.totalStandardHours);
  }, [filteredDetails]);

  const departmentSummaryRows = useMemo<DepartmentSummaryRow[]>(() => {
    const grouped = new Map<string, { teachers: Set<string>; classCount: number; totalStandardHours: number }>();
    filteredDetails.forEach((item) => {
      const current = grouped.get(item.departmentPh) ?? { teachers: new Set<string>(), classCount: 0, totalStandardHours: 0 };
      current.teachers.add(item.teacherName);
      current.classCount += 1;
      current.totalStandardHours += item.standardHours ?? 0;
      grouped.set(item.departmentPh, current);
    });
    return Array.from(grouped.entries())
      .map(([departmentPh, item]) => ({
        departmentPh,
        teacherCount: item.teachers.size,
        classCount: item.classCount,
        totalStandardHours: Math.round(item.totalStandardHours * 100) / 100
      }))
      .sort((left, right) => right.totalStandardHours - left.totalStandardHours);
  }, [filteredDetails]);

  const teacherChartRows = teacherSummaryRows.slice(0, 10).reverse();
  const departmentChartRows = departmentSummaryRows.slice(0, 8);

  const handleCalculate = (id: number) => {
    const nextSettings = buildCalculationSettings(readSystemSettings());
    if (subjectRules.data && subjectRules.data.length > 0) {
      nextSettings.subjectRules = subjectRules.data.map((rule) => ({
        code: rule.code,
        name: rule.name,
        coefficientTheoryFormula: rule.coefficientTheoryFormula,
        coefficientPracticeFormula: rule.coefficientPracticeFormula,
        formula: rule.formula,
        note: rule.note ?? ''
      }));
    }
    calculateMutation.mutate({ id, settings: nextSettings });
  };

  const handleUpload = (values: UploadImportBatchForm) => {
    if (termFilter.semester === ALL_YEAR_SEMESTER) {
      message.warning('Chọn Kỳ 1 hoặc Kỳ 2 trên header trước khi nhập file Excel.');
      return;
    }
    uploadMutation.mutate({
      ...values,
      academicYear: termFilter.academicYear || defaultAcademicYear,
      semester: termFilter.semester || defaultSemester
    });
  };

  return (
    <div className="page-stack">

      <Form
        form={form}
        layout="vertical"
        onFinish={handleUpload}
      >
        <Card
          title="Tạo lần nhập dữ liệu"
          extra={
            <Space wrap className="card-header-actions">
              <Button icon={<DownloadOutlined />} onClick={() => {
                const ws = XLSX.utils.aoa_to_sheet([
                  ['Lớp học phần', 'Tên học phần', 'Tín chỉ', 'Bộ môn HN', 'Bộ môn PH', 'Số SV', 'Giảng viên', 'Chức danh', 'Trình độ']
                ]);
                ws['!cols'] = [{ wch: 25 }, { wch: 35 }, { wch: 10 }, { wch: 20 }, { wch: 20 }, { wch: 10 }, { wch: 25 }, { wch: 15 }, { wch: 15 }];
                const wb = XLSX.utils.book_new();
                XLSX.utils.book_append_sheet(wb, ws, 'Data');
                XLSX.writeFile(wb, 'mau-import-klgd.xlsx');
              }}>
                Tải mẫu Excel
              </Button>
              <Button type="primary" icon={<UploadOutlined />} loading={uploadMutation.isPending} onClick={() => form.submit()}>
                Nhập file
              </Button>
              <Button icon={<ReloadOutlined />} onClick={() => batches.refetch()}>
                Làm mới danh sách
              </Button>
            </Space>
          }
        >
          <Form.Item name="file" label="File Excel lớp học phần/HPTN hoặc PDF HPTN" rules={[{ required: true, message: 'Chọn file' }]}>
            <Upload.Dragger
              multiple
              accept=".xlsx,.pdf"
              beforeUpload={() => false}
              onChange={(info) => {
                const names = info.fileList.map((item) => item.name);
                setSelectedFileName(names.length > 1 ? `${names.length} file đã chọn` : names[0]);
              }}
              onRemove={() => {
                setSelectedFileName(undefined);
                return true;
              }}
            >
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p className="ant-upload-text">Kéo thả hoặc chọn file danh sách lớp học phần/HPTN hoặc PDF giao đề tài HPTN</p>
              <p className={selectedFileName ? 'upload-file-name is-selected' : 'upload-file-name'}>
                {selectedFileName || `Năm học ${termFilter.academicYear || defaultAcademicYear} - ${termFilter.semester || defaultSemester}`}
              </p>
            </Upload.Dragger>
          </Form.Item>
        </Card>
      </Form>

      <Card title="Lịch sử nhập dữ liệu">
        {batches.isError && (
          <Alert
            type="error"
            showIcon
            className="import-history-alert"
            message="Không lấy được lịch sử nhập dữ liệu"
            description="Vui lòng đăng nhập lại hoặc bấm Làm mới danh sách. Dữ liệu đã nhập vẫn còn trong hệ thống nếu phần tổng quan vẫn hiển thị kết quả."
          />
        )}
        <Table
          rowKey="id"
          tableLayout="fixed"
          dataSource={visibleBatches}
          loading={batches.isLoading}
          pagination={smallPagination}
          columns={[
            sttColumn,
            { title: 'Tên file', dataIndex: 'fileName', className: 'text-left' },
            { title: 'Năm học', dataIndex: 'academicYear' },
            { title: 'Học kỳ', dataIndex: 'semester' },
            { title: 'Trạng thái', dataIndex: 'status', render: (value: string) => <Tag color={statusColor[value] ?? 'default'}>{statusLabel[value] ?? value}</Tag> },
            { title: 'Dòng', dataIndex: 'totalRows' },
            { title: 'Hợp lệ', dataIndex: 'validRows' },
            { title: 'Cảnh báo', dataIndex: 'warningRows' },
            {
              title: 'Thao tác',
              width: 260,
              render: (_, record) => (
                <Space wrap>
                  <Button size="small" icon={<EyeOutlined />} onClick={() => setSelectedBatchId(record.id)}>Xem</Button>
                  <Button
                    size="small"
                    type={record.status === 'CALCULATED' ? 'default' : 'primary'}
                    onClick={() => handleCalculate(record.id)}
                    loading={calculateMutation.isPending}
                  >
                    Tính KLGD
                  </Button>
                  <Popconfirm
                    title="Xóa lần nhập dữ liệu?"
                    description="Dữ liệu lớp và kết quả tính liên quan cũng sẽ bị xóa."
                    okText="Xóa"
                    cancelText="Hủy"
                    okButtonProps={{ danger: true }}
                    onConfirm={() => deleteMutation.mutate(record.id)}
                  >
                    <Button size="small" danger icon={<DeleteOutlined />} loading={deleteMutation.isPending}>Xóa</Button>
                  </Popconfirm>
                </Space>
              )
            }
          ]}
        />
      </Card>

      <Card title="Top giảng viên theo tiết quy đổi">
        <ReactECharts
          className="import-chart"
          option={{
            tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
            grid: { left: 150, right: 36, top: 20, bottom: 32 },
            xAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(11, 98, 88, 0.16)', type: 'dashed' } } },
            yAxis: {
              type: 'category',
              data: teacherChartRows.map((item) => item.teacherName),
              axisLabel: { width: 130, overflow: 'truncate' }
            },
            series: [{
              type: 'bar',
              barWidth: 14,
              data: teacherChartRows.map((item) => item.totalStandardHours),
              itemStyle: { color: '#0b6258', borderRadius: [0, 999, 999, 0] },
              label: { show: true, position: 'right', formatter: ({ value }: { value: number }) => formatNumber(value) }
            }]
          }}
        />
      </Card>

      <Card title="Cơ cấu theo bộ môn">
        <ReactECharts
          className="import-chart"
          option={{
            tooltip: { trigger: 'item', formatter: '{b}: {c} giờ ({d}%)' },
            legend: { bottom: 0, type: 'scroll' },
            series: [{
              type: 'pie',
              radius: ['42%', '68%'],
              center: ['50%', '44%'],
              data: departmentChartRows.map((item) => ({ name: item.departmentPh, value: item.totalStandardHours })),
              label: { formatter: '{b}' }
            }]
          }}
        />
      </Card>

      <Card title="Xem khối lượng giảng dạy">
        <Tabs
          className="workload-tabs"
          items={[
            {
              key: 'summary',
              label: 'Tổng hợp giảng dạy',
              children: (
                <Table
                  className="workload-table"
                  rowKey={(record) => `${record.teacherName}-${record.subjectName}`}
                  tableLayout="fixed"
                  scroll={{ x: 1120 }}
                  dataSource={teacherSummaryRows}
                  loading={details.isLoading}
                  size="small"
                  pagination={widePagination}
                  columns={[
                    sttColumn,
                    { title: 'GV', dataIndex: 'teacherName', width: 210, className: 'text-left' },
                    { title: 'Học phần', dataIndex: 'subjectName', width: 260, className: 'text-left workload-subject-cell' },
                    { title: 'Số lớp', dataIndex: 'classCount', width: 100 },
                    { title: 'Tổng TC', dataIndex: 'totalCredits', width: 100, render: formatNumber },
                    { title: 'Tổng SV', dataIndex: 'totalStudents', width: 110, render: formatNumber },
                    { title: 'Tổng tiết quy đổi', dataIndex: 'totalStandardHours', width: 150, render: formatNumber }
                  ]}
                />
              )
            },
            {
              key: 'detail',
              label: 'Chi tiết từng giảng viên',
              children: (
                <Table
                  className="workload-table"
                  rowKey={(record, index) => `${record.teacherName}-${record.className}-${record.subjectName}-${index}`}
                  tableLayout="fixed"
                  scroll={{ x: 1950 }}
                  dataSource={filteredDetails}
                  loading={details.isLoading}
                  size="small"
                  pagination={widePagination}
                  columns={[
                    sttColumn,
                    { title: 'GV', dataIndex: 'teacherName', width: 190, className: 'text-left' },
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
                        { title: 'K', dataIndex: 'coefficientK', width: 90, render: renderGeneralCoefficient, sorter: numberSorter((record) => record.coefficientK) },
                        { title: 'K_LT', dataIndex: 'coefficientTheory', width: 100, render: renderTheoryCoefficient, sorter: numberSorter((record) => record.coefficientTheory) },
                        { title: 'K_TH', dataIndex: 'coefficientPractice', width: 100, render: renderPracticeCoefficient, sorter: numberSorter((record) => record.coefficientPractice) }
                      ]
                    },
                    { title: 'GC', dataIndex: 'standardHours', width: 130, render: formatNumber, sorter: numberSorter((record) => record.standardHours) },
                    { title: 'Quy tắc', dataIndex: 'ruleName', width: 220, className: 'text-left' }
                  ]}
                />
              )
            }
          ]}
        />
      </Card>

      <Drawer title="Chi tiết dữ liệu đã nhập" open={Boolean(selectedBatchId)} onClose={() => setSelectedBatchId(undefined)} width="min(1120px, 92vw)">
        <Table
          className="workload-table"
          rowKey="id"
          tableLayout="fixed"
          scroll={{ x: 1420 }}
          dataSource={records.data ?? []}
          loading={records.isLoading}
          size="small"
          pagination={widePagination}
          columns={[
            sttColumn,
            { title: 'Lớp', dataIndex: 'className', width: 220, className: 'text-left' },
            { title: 'Môn học', dataIndex: 'subjectName', width: 300, className: 'text-left workload-subject-cell' },
            { title: 'TC', dataIndex: 'credits', width: 80 },
            { title: 'SV', dataIndex: 'studentCount', width: 80 },
            { title: 'Bộ môn HN', dataIndex: 'departmentHn', width: 170, className: 'text-left' },
            { title: 'Bộ môn PH', dataIndex: 'departmentPh', width: 150, className: 'text-left' },
            { title: 'Giảng viên', dataIndex: 'teacherName', width: 190, className: 'text-left' },
            { title: 'Trạng thái', width: 130, render: (_, record) => record.valid ? <Tag color="green">Hợp lệ</Tag> : <Tag color="orange">Cảnh báo</Tag> },
            { title: 'Ghi chú', dataIndex: 'warningMessage', width: 220, className: 'text-left' }
          ]}
        />
      </Drawer>
    </div>
  );
}

