import { Alert, Button, Card, Col, Empty, Row, Select, Skeleton, Space, Statistic, Table, Tabs } from 'antd';
import ReactECharts from 'echarts-for-react';
import { useEffect, useMemo, useState } from 'react';
import type { TermFilter } from '../components/AppShell';
import { smallPagination, sttColumn, widePagination } from '../constants/table';
import { DEFAULT_WORKLOAD_RULES, readWorkloadRules } from '../constants/workloadConfig';
import { useSubjectRuleConfigsQuery, useTeacherWorkloadDetailsQuery } from '../features/workloads/workloads.query';
import type { TeacherWorkloadDetail } from '../features/workloads/workloads.types';
import { antSelectFilterOption, normalizeSearchText } from '../utils/search';

type DashboardPageProps = {
  termFilter: TermFilter;
  canDownloadReport: boolean;
  onOpenImports?: () => void;
};

type TeacherSummaryRow = {
  teacherName: string;
  departmentPh: string;
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

type RuleSummaryRow = {
  ruleCode: string;
  ruleName: string;
  classCount: number;
  totalStandardHours: number;
};

const CHART_TEXT_COLOR = '#0b6258';
const CHART_GRID_COLOR = 'rgba(11, 98, 88, 0.16)';
const ALL_YEAR_SEMESTER = 'Cả năm';
const GROUP_COLORS = ['#0b6258', '#168477', '#3a9465', '#b98218', '#7d7a3f', '#ffefb3'];
const WORKLOAD_GROUPS = [
  { ruleCode: 'GIANG_DAY', ruleName: 'Giảng Dạy' },
  { ruleCode: 'HUONG_DAN', ruleName: 'Hướng Dẫn' },
  { ruleCode: 'HOI_DONG', ruleName: 'Hội Đồng' },
  { ruleCode: 'COI_CHAM_THI', ruleName: 'Coi chấm Thi' },
  { ruleCode: 'CONG_TAC_KHAC', ruleName: 'Công tác Khác' }
];
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
const workloadGroupOf = (record: TeacherWorkloadDetail) => {
  const text = normalizeSearchText(`${record.ruleName} ${record.subjectName} ${record.className}`);
  if (text.includes('hoi dong')) {
    return 'HOI_DONG';
  }
  if (text.includes('coi thi') || text.includes('cham thi') || text.includes('coi cham')) {
    return 'COI_CHAM_THI';
  }
  if (text.includes('huong dan') || text.includes('tot nghiep') || text.includes('do an') || text.includes('khoa luan') || text.includes('luan van')) {
    return 'HUONG_DAN';
  }
  if (text.includes('cong tac')) {
    return 'CONG_TAC_KHAC';
  }
  return 'GIANG_DAY';
};

const getTeachingStatus = (academicYear?: string, semester?: string) => {
  const match = academicYear?.match(/(\d{4})\s*-\s*(\d{4})/);
  if (!match) {
    return 'Chưa hoàn thành';
  }
  const now = new Date();
  const endYear = Number(match[2]);
  const finishMonth = semester === 'Kỳ 1' ? 1 : 8;
  const finishDate = new Date(endYear, finishMonth - 1, semester === 'Kỳ 1' ? 31 : 31);
  return now >= finishDate ? 'Hoàn thành' : 'Chưa hoàn thành';
};

export default function DashboardPage({ termFilter, canDownloadReport, onOpenImports }: DashboardPageProps) {
  const [selectedTeacher, setSelectedTeacher] = useState<string>();
  const [selectedDepartment, setSelectedDepartment] = useState<string>();
  const [selectedSubject, setSelectedSubject] = useState<string>();
  const [selectedRule, setSelectedRule] = useState<string>();
  const details = useTeacherWorkloadDetailsQuery();
  const subjectRules = useSubjectRuleConfigsQuery();

  useEffect(() => {
    setSelectedTeacher(undefined);
    setSelectedDepartment(undefined);
    setSelectedSubject(undefined);
    setSelectedRule(undefined);
  }, [termFilter.academicYear, termFilter.semester]);

  const periodDetails = useMemo(() => {
    return (details.data ?? []).filter((item) => {
      const matchYear = item.academicYear === termFilter.academicYear;
      const matchSemester = termFilter.semester === ALL_YEAR_SEMESTER || item.semester === termFilter.semester;
      return matchYear && matchSemester;
    });
  }, [details.data, termFilter.academicYear, termFilter.semester]);

  const departmentOptions = useMemo(() => Array.from(new Set(periodDetails.map((item) => item.departmentPh).filter(Boolean)))
    .sort((left, right) => left.localeCompare(right, 'vi'))
    .map((department) => ({ label: department, value: department })), [periodDetails]);

  const subjectOptions = useMemo(() => {
    const sourceRows = selectedDepartment
      ? periodDetails.filter((item) => item.departmentPh === selectedDepartment)
      : periodDetails;
    return Array.from(new Set(sourceRows.map((item) => item.subjectName).filter(Boolean)))
      .sort((left, right) => left.localeCompare(right, 'vi'))
      .map((subject) => ({ label: subject, value: subject }));
  }, [periodDetails, selectedDepartment]);

  const ruleOptions = useMemo(() => Array.from(new Set(periodDetails.map((item) => item.ruleName || 'Chưa xác định')))
    .sort((left, right) => left.localeCompare(right, 'vi'))
    .map((ruleName) => ({ label: ruleName, value: ruleName })), [periodDetails]);

  const allRuleNames = useMemo(() => {
    const ruleNames = new Set<string>();
    DEFAULT_WORKLOAD_RULES.forEach((rule) => ruleNames.add(rule.name));
    readWorkloadRules().forEach((rule) => ruleNames.add(rule.name));
    (subjectRules.data ?? []).forEach((rule) => ruleNames.add(rule.name));
    const periodRuleTotals = new Map<string, number>();
    periodDetails.forEach((item) => {
      const ruleName = item.ruleName || 'Chưa xác định';
      ruleNames.add(ruleName);
      periodRuleTotals.set(ruleName, (periodRuleTotals.get(ruleName) ?? 0) + (item.standardHours ?? 0));
    });
    return Array.from(ruleNames)
      .sort((left, right) => (periodRuleTotals.get(right) ?? 0) - (periodRuleTotals.get(left) ?? 0) || left.localeCompare(right, 'vi'));
  }, [periodDetails, subjectRules.data]);

  const filteredDetails = useMemo(() => {
    return periodDetails.filter((item) => {
      const ruleName = item.ruleName || 'Chưa xác định';
      const matchTeacher = !selectedTeacher || selectedTeacher === item.teacherName;
      const matchDepartment = !selectedDepartment || selectedDepartment === item.departmentPh;
      const matchSubject = !selectedSubject || selectedSubject === item.subjectName;
      const matchRule = !selectedRule || selectedRule === ruleName;
      return matchTeacher && matchDepartment && matchSubject && matchRule;
    });
  }, [periodDetails, selectedDepartment, selectedRule, selectedSubject, selectedTeacher]);

  const teacherRows = useMemo<TeacherSummaryRow[]>(() => {
    const grouped = new Map<string, TeacherSummaryRow>();
    filteredDetails.forEach((item) => {
      const key = `${item.teacherName}__${item.departmentPh}`;
      const current = grouped.get(key) ?? {
        teacherName: item.teacherName,
        departmentPh: item.departmentPh,
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

  const hasData = teacherRows.length > 0;
  const filteredTeacherRows = useMemo(() => {
    if (!selectedTeacher) {
      return teacherRows;
    }
    return teacherRows.filter((item) => selectedTeacher === item.teacherName);
  }, [selectedTeacher, teacherRows]);
  const departmentRows = useMemo<DepartmentSummaryRow[]>(() => {
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

  const ruleRows = useMemo<RuleSummaryRow[]>(() => {
    const grouped = new Map<string, RuleSummaryRow>();
    filteredDetails.forEach((item) => {
      const ruleName = item.ruleName || 'Chưa xác định';
      const current = grouped.get(ruleName) ?? { ruleCode: ruleName, ruleName, classCount: 0, totalStandardHours: 0 };
      current.classCount += 1;
      current.totalStandardHours += item.standardHours ?? 0;
      grouped.set(ruleName, current);
    });
    return allRuleNames.map((ruleName) => grouped.get(ruleName) ?? { ruleCode: ruleName, ruleName, classCount: 0, totalStandardHours: 0 })
      .map((item) => ({ ...item, totalStandardHours: Math.round(item.totalStandardHours * 100) / 100 }))
      .sort((left, right) => right.totalStandardHours - left.totalStandardHours);
  }, [allRuleNames, filteredDetails]);

  const overviewData = useMemo(() => {
    const teachers = new Set(filteredDetails.map((item) => item.teacherName));
    const departments = new Set(filteredDetails.map((item) => item.departmentPh));
    const subjects = new Set(filteredDetails.map((item) => item.subjectName));
    const totalStandardHours = filteredDetails.reduce((sum, item) => sum + (item.standardHours ?? 0), 0);
    const guestClassCount = filteredDetails.filter((item) => item.ruleName?.toLowerCase().includes('thỉnh')).length;
    const totalStudents = filteredDetails.reduce((sum, item) => sum + (item.studentCount ?? 0), 0);
    const totalCredits = filteredDetails.reduce((sum, item) => sum + (item.credits ?? 0), 0);
    const averageK = filteredDetails.reduce((sum, item) => sum + (item.coefficientK ?? item.coefficientTheory ?? 0), 0) / Math.max(filteredDetails.length, 1);
    return {
      teacherCount: teachers.size,
      departmentCount: departments.size,
      subjectCount: subjects.size,
      classCount: filteredDetails.length,
      totalStandardHours: Math.round(totalStandardHours * 100) / 100,
      guestClassCount,
      totalStudents,
      totalCredits: Math.round(totalCredits * 100) / 100,
      averageK: Math.round(averageK * 100) / 100
    };
  }, [filteredDetails]);

  const teacherOptions = Array.from(new Set(periodDetails.map((item) => item.teacherName))).sort((left, right) => left.localeCompare(right, 'vi')).map((teacherName) => ({
    label: teacherName,
    value: teacherName
  }));
  const chartRows = filteredTeacherRows.slice(0, 12).reverse();
  const heroGroupRows = useMemo<RuleSummaryRow[]>(() => {
    const grouped = new Map(WORKLOAD_GROUPS.map((group) => [group.ruleCode, { ...group, classCount: 0, totalStandardHours: 0 }]));
    filteredDetails.forEach((item) => {
      const groupCode = workloadGroupOf(item);
      const current = grouped.get(groupCode);
      if (!current) {
        return;
      }
      current.classCount += 1;
      current.totalStandardHours += item.standardHours ?? 0;
    });
    return WORKLOAD_GROUPS.map((group) => {
      const current = grouped.get(group.ruleCode) ?? { ...group, classCount: 0, totalStandardHours: 0 };
      return { ...current, totalStandardHours: Math.round(current.totalStandardHours * 100) / 100 };
    });
  }, [filteredDetails]);
  const donutRows = heroGroupRows.map((item, index) => ({
    name: item.ruleName,
    value: item.totalStandardHours,
    itemStyle: { color: GROUP_COLORS[index % GROUP_COLORS.length] }
  }));

  if (details.isError) {
    return <Alert type="warning" message="Chưa kết nối được backend hoặc chưa có dữ liệu tính toán." showIcon />;
  }

  return (
    <div className="page-stack dashboard-page">
      <Card className="dashboard-filter-card dashboard-filter-card-top">
        <Space wrap className="table-toolbar dashboard-top-filter-toolbar">
          <Select
            allowClear
            showSearch
            placeholder="Giảng viên"
            options={teacherOptions}
            value={selectedTeacher}
            onChange={setSelectedTeacher}
            filterOption={antSelectFilterOption}
            className="filter-select teacher-filter-select"
          />
          <Select
            allowClear
            showSearch
            placeholder="Bộ môn"
            options={departmentOptions}
            value={selectedDepartment}
            onChange={(department) => {
              setSelectedDepartment(department);
              setSelectedSubject(undefined);
            }}
            filterOption={antSelectFilterOption}
            className="filter-select"
          />
          <Select
            allowClear
            showSearch
            placeholder="Môn học"
            options={subjectOptions}
            value={selectedSubject}
            onChange={setSelectedSubject}
            filterOption={antSelectFilterOption}
            className="filter-select subject-filter-select"
            disabled={subjectOptions.length === 0}
          />
          <Select
            allowClear
            showSearch
            placeholder="Quy tắc tính"
            options={ruleOptions}
            value={selectedRule}
            onChange={setSelectedRule}
            filterOption={antSelectFilterOption}
            className="filter-select"
          />
          <Button onClick={() => {
            setSelectedTeacher(undefined);
            setSelectedDepartment(undefined);
            setSelectedSubject(undefined);
            setSelectedRule(undefined);
          }}>
            Xóa lọc
          </Button>
        </Space>
      </Card>

      <Card className="teaching-hero-card">
        <Row gutter={[24, 18]} align="middle">
          <Col xs={24} lg={5}>
            <div className="teaching-total-card">
              <div className="teaching-total-label">GIẢNG DẠY</div>
              <div className="teaching-total-value">
                {formatNumber(overviewData.totalStandardHours)}
                <span>GC</span>
              </div>
              <div className="teaching-total-meta">{overviewData.classCount} lớp học phần</div>
              <div className="teaching-total-progress">
                <span style={{ width: `${Math.min(100, Math.max(8, overviewData.averageK * 65))}%` }} />
              </div>
              <div className="teaching-total-meta">K trung bình {formatNumber(overviewData.averageK)}</div>
            </div>
          </Col>
          <Col xs={24} lg={5}>
            <div className="donut-block">
              <div className="donut-title">PHÂN BỔ GIỜ CHUẨN</div>
              <ReactECharts
                style={{ height: 250 }}
                option={{
                  tooltip: { trigger: 'item', formatter: '{b}: {c} GC ({d}%)' },
                  series: [{
                    type: 'pie',
                    radius: ['64%', '86%'],
                    center: ['50%', '50%'],
                    label: { show: false },
                    data: donutRows
                  }]
                }}
              />
              <div className="donut-center-label">
                <strong>{formatNumber(overviewData.totalStandardHours)}</strong>
                <span>GC</span>
              </div>
            </div>
          </Col>
          <Col xs={24} lg={14}>
            <div className="group-detail-block">
              <div className="group-detail-title">CHI TIẾT THEO NHÓM</div>
              <div className="group-summary-grid">
                {heroGroupRows.map((item, index) => (
                  <div className="group-summary-item workload-insight-item" key={item.ruleCode || item.ruleName}>
                    <span className="group-dot" style={{ background: GROUP_COLORS[index % GROUP_COLORS.length] }} />
                    <div>
                      <strong>{item.ruleName}</strong>
                      <div>
                        <b>{formatNumber(item.totalStandardHours)}</b>
                        <span>GC</span>
                      </div>
                      <small>{formatNumber(item.classCount)} hoạt động</small>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </Col>
        </Row>
      </Card>

      <Row gutter={[14, 14]} className="compact-stat-row">
        <Col xs={12} md={6} xl={3}><Card className="stat-card stat-card-blue"><Statistic title="GV" value={overviewData.teacherCount} loading={details.isLoading} /></Card></Col>
        <Col xs={12} md={6} xl={3}><Card className="stat-card stat-card-cyan"><Statistic title="Lớp" value={overviewData.classCount} loading={details.isLoading} /></Card></Col>
        <Col xs={12} md={6} xl={3}><Card className="stat-card stat-card-violet"><Statistic title="Bộ môn" value={overviewData.departmentCount} loading={details.isLoading} /></Card></Col>
        <Col xs={12} md={6} xl={3}><Card className="stat-card stat-card-amber"><Statistic title="Học phần" value={overviewData.subjectCount} loading={details.isLoading} /></Card></Col>
        <Col xs={12} md={6} xl={3}><Card className="stat-card stat-card-emerald"><Statistic title="Sinh viên" value={overviewData.totalStudents} loading={details.isLoading} /></Card></Col>
        <Col xs={12} md={6} xl={3}><Card className="stat-card stat-card-rose"><Statistic title="Thỉnh giảng" value={overviewData.guestClassCount} loading={details.isLoading} /></Card></Col>
        <Col xs={12} md={6} xl={3}><Card className="stat-card stat-card-indigo"><Statistic title="K TB" value={overviewData.averageK} precision={2} loading={details.isLoading} /></Card></Col>
        <Col xs={12} md={6} xl={3}><Card className="stat-card stat-card-slate"><Statistic title="Giờ chuẩn" value={overviewData.totalStandardHours} precision={1} loading={details.isLoading} /></Card></Col>
      </Row>

      {!details.isLoading && !hasData && (
        <Card>
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={`Chưa có dữ liệu khối lượng giảng dạy cho ${termFilter.academicYear} - ${termFilter.semester}`}>
            {canDownloadReport && onOpenImports && (
              <Button type="primary" onClick={onOpenImports}>
                Nhập dữ liệu Excel
              </Button>
            )}
          </Empty>
        </Card>
      )}

      {hasData && (
        <>
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
                      rowKey={(record) => `${record.teacherName}-${record.departmentPh}`}
                      tableLayout="fixed"
                      scroll={{ x: 1120 }}
                      dataSource={filteredTeacherRows}
                      loading={details.isLoading}
                      size="small"
                      pagination={widePagination}
                      columns={[
                        sttColumn,
                        { title: 'GV', dataIndex: 'teacherName', width: 210, className: 'text-left' },
                        { title: 'Bộ môn', dataIndex: 'departmentPh', width: 160, className: 'text-left' },
                        { title: 'Số lớp', dataIndex: 'classCount', width: 100, sorter: numberSorter((record) => record.classCount) },
                        { title: 'Tổng TC', dataIndex: 'totalCredits', width: 110, render: formatNumber, sorter: numberSorter((record) => record.totalCredits) },
                        { title: 'Tổng SV', dataIndex: 'totalStudents', width: 110, render: formatNumber, sorter: numberSorter((record) => record.totalStudents) },
                        { title: 'Tổng tiết quy đổi', dataIndex: 'totalStandardHours', width: 160, render: formatNumber, sorter: numberSorter((record) => record.totalStandardHours) }
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
                        { title: 'BM', dataIndex: 'departmentPh', width: 120, className: 'text-left' },
                        { title: 'Đơn vị', dataIndex: 'unitName', width: 250, className: 'text-left' },
                        {
                          title: '',
                          children: [
                            { title: 'K_LT', dataIndex: 'coefficientTheory', width: 100, render: renderTheoryCoefficient, sorter: numberSorter((record) => hasSplitCoefficient(record) ? record.coefficientTheory : record.coefficientK) },
                            { title: 'K_TH', dataIndex: 'coefficientPractice', width: 100, render: renderPracticeCoefficient, sorter: numberSorter((record) => record.coefficientPractice) }
                          ]
                        },
                        { title: 'Tiết quy đổi', dataIndex: 'standardHours', width: 130, render: formatNumber, sorter: numberSorter((record) => record.standardHours) },
                        { title: 'Quy tắc', dataIndex: 'ruleName', width: 220, className: 'text-left' }
                      ]}
                    />
                  )
                }
              ]}
            />
          </Card>

          <Row gutter={[16, 16]}>
            <Col xs={24}>
              <Card
                title="Khối lượng theo giảng viên"
                className="chart-card"
                extra={
                  <Space wrap className="chart-filter">
                    <Select
                      allowClear
                      showSearch
                      placeholder="Chọn giảng viên"
                      options={teacherOptions}
                      value={selectedTeacher}
                      onChange={setSelectedTeacher}
                      filterOption={antSelectFilterOption}
                      className="teacher-select"
                      popupClassName="teacher-select-popup"
                      optionLabelProp="label"
                    />
                    <Button onClick={() => setSelectedTeacher(teacherRows[0]?.teacherName)}>Cao nhất</Button>
                    <Button onClick={() => setSelectedTeacher(undefined)}>Tất cả</Button>
                  </Space>
                }
              >
                {details.isLoading ? (
                  <Skeleton active />
                ) : (
                  <ReactECharts
                    style={{ height: 390 }}
                    option={{
                      textStyle: { fontFamily: '"Inter", "Segoe UI Variable", "Segoe UI", Arial, sans-serif', color: CHART_TEXT_COLOR },
                      tooltip: {
                        trigger: 'axis',
                        axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(11, 98, 88, 0.1)' } },
                        backgroundColor: 'rgba(255, 255, 255, 0.98)',
                        borderColor: 'rgba(11, 98, 88, 0.18)',
                        borderWidth: 1,
                        padding: 12,
                        textStyle: { color: '#0b6258' },
                        valueFormatter: (value: number) => `${formatNumber(value)} giờ`
                      },
                      grid: { left: 160, right: 42, top: 24, bottom: 34 },
                      xAxis: {
                        type: 'value',
                        axisLine: { show: false },
                        axisTick: { show: false },
                        axisLabel: { color: CHART_TEXT_COLOR },
                        splitLine: { lineStyle: { color: CHART_GRID_COLOR, type: 'dashed' } }
                      },
                      yAxis: {
                        type: 'category',
                        data: chartRows.map((item) => item.teacherName),
                        axisLine: { show: false },
                        axisTick: { show: false },
                        axisLabel: { color: CHART_TEXT_COLOR, width: 140, overflow: 'truncate' }
                      },
                      series: [
                        {
                          name: 'Giờ chuẩn',
                          type: 'bar',
                          barWidth: 13,
                          data: chartRows.map((item) => item.totalStandardHours),
                          itemStyle: {
                            borderRadius: [0, 999, 999, 0],
                            color: {
                              type: 'linear',
                              x: 0,
                              y: 0,
                              x2: 1,
                              y2: 0,
                              colorStops: [
                                { offset: 0, color: '#168477' },
                                { offset: 1, color: '#0b6258' }
                              ]
                            }
                          },
                          label: {
                            show: true,
                            position: 'right',
                            color: '#0b6258',
                            fontSize: 12,
                            fontWeight: 600,
                            formatter: ({ value }: { value: number }) => formatNumber(value)
                          }
                        }
                      ]
                    }}
                  />
                )}
              </Card>
            </Col>
            <Col xs={24}>
              <Card title="Tổng hợp theo bộ môn">
                <Table
                  rowKey="departmentPh"
                  tableLayout="fixed"
                  size="small"
                  dataSource={departmentRows}
                  loading={details.isLoading}
                  pagination={smallPagination}
                  columns={[
                    sttColumn,
                    { title: 'Bộ môn', dataIndex: 'departmentPh', className: 'text-left' },
                    { title: 'GV', dataIndex: 'teacherCount' },
                    { title: 'Lớp', dataIndex: 'classCount' },
                    { title: 'Giờ chuẩn', dataIndex: 'totalStandardHours', render: formatNumber }
                  ]}
                />
              </Card>
            </Col>
          </Row>

          <Card title="Cơ cấu quy tắc tính">
            <Table
              rowKey="ruleCode"
              tableLayout="fixed"
              size="small"
              dataSource={ruleRows}
              loading={details.isLoading}
              pagination={smallPagination}
              columns={[
                sttColumn,
                { title: 'Tên quy tắc', dataIndex: 'ruleName', className: 'text-left' },
                { title: 'Số lớp', dataIndex: 'classCount' },
                { title: 'Giờ chuẩn', dataIndex: 'totalStandardHours', render: formatNumber }
              ]}
            />
          </Card>

        </>
      )}
    </div>
  );
}
