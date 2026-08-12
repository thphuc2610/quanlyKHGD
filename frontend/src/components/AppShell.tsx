import {
  BarChartOutlined,
  CloudUploadOutlined,
  ControlOutlined,
  PoweroffOutlined,
  SettingOutlined,
  SolutionOutlined,
  UserOutlined
} from '@ant-design/icons';
import { Avatar, Button, Layout, Menu, Select, Space, Typography } from 'antd';
import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { APP_PATHS, type AppPath } from '../constants/paths';
import { readSystemSettings, saveSystemSettings } from '../constants/workloadConfig';
import type { AuthSession, AuthUser } from '../features/auth/auth.types';
import { antSelectFilterOption } from '../utils/search';

const { Header, Sider, Content } = Layout;

type AppShellProps = {
  activePath: AppPath;
  onPathChange: (path: AppPath) => void;
  session: AuthSession;
  user: AuthUser;
  onSessionChange: (session: AuthSession) => void;
  onLogout: () => void;
  termFilter: TermFilter;
  onTermFilterChange: (termFilter: TermFilter) => void;
  headerExtra?: ReactNode;
  children: ReactNode;
};

export type TermFilter = {
  academicYear: string;
  semester: string;
};

const semesterOptions = [
  { value: 'Kỳ 1', label: 'Kỳ 1' },
  { value: 'Kỳ 2', label: 'Kỳ 2' },
  { value: 'Cả năm', label: 'Cả năm' }
];

const APP_BRAND_NAME = 'Quản lý Khối lượng Giảng dạy';
export const SIDEBAR_TITLE_KEY = 'klgd.sidebar_academic_title';
export const ACADEMIC_TITLE_UPDATED_EVENT = 'klgd.academic_title_updated';

const academicYearStart = (academicYear?: string) => {
  const match = academicYear?.match(/^(\d{4})-\d{4}$/);
  return match ? Number(match[1]) : new Date().getFullYear();
};

const academicYearLabel = (startYear: number) => `${startYear}-${startYear + 1}`;

export default function AppShell({
  activePath,
  onPathChange,
  session,
  user,
  onSessionChange,
  onLogout,
  termFilter,
  onTermFilterChange,
  headerExtra,
  children
}: AppShellProps) {
  const isAdmin = user.roles.includes('ADMIN');
  const [academicTitle, setAcademicTitle] = useState(() => window.localStorage.getItem(SIDEBAR_TITLE_KEY) || 'Tiến sĩ');
  const academicYearOptions = useMemo(() => {
    const selectedStartYear = academicYearStart(termFilter.academicYear);
    const currentStartYear = new Date().getFullYear();
    const firstYear = Math.min(2024, selectedStartYear - 1, currentStartYear - 1);
    const lastYear = Math.max(2027, selectedStartYear + 1, currentStartYear + 1);
    const years = new Set<string>();
    for (let year = firstYear; year <= lastYear; year += 1) {
      years.add(academicYearLabel(year));
    }
    years.add(termFilter.academicYear);
    return Array.from(years).map((year) => ({ value: year, label: year }));
  }, [termFilter.academicYear]);

  useEffect(() => {
    const syncTitle = () => setAcademicTitle(window.localStorage.getItem(SIDEBAR_TITLE_KEY) || 'Tiến sĩ');
    window.addEventListener(ACADEMIC_TITLE_UPDATED_EVENT, syncTitle);
    window.addEventListener('storage', syncTitle);
    return () => {
      window.removeEventListener(ACADEMIC_TITLE_UPDATED_EVENT, syncTitle);
      window.removeEventListener('storage', syncTitle);
    };
  }, []);

  const menuItems = [
    ...(isAdmin ? [{ key: APP_PATHS.dashboard, icon: <BarChartOutlined />, label: 'Tổng quan' }] : []),
    ...(isAdmin ? [{ key: APP_PATHS.imports, icon: <CloudUploadOutlined />, label: 'Nhập dữ liệu' }] : []),
    ...(!isAdmin ? [{ key: APP_PATHS.teachers, icon: <SolutionOutlined />, label: 'Khối lượng của tôi' }] : []),
    { key: APP_PATHS.settings, icon: <SettingOutlined />, label: 'Cài đặt' },
    ...(isAdmin ? [{ key: APP_PATHS.rules, icon: <ControlOutlined />, label: 'Quy tắc tính' }] : [])
  ];

  const handleTermChange = (nextTermFilter: TermFilter) => {
    onTermFilterChange(nextTermFilter);
    const settings = readSystemSettings();
    saveSystemSettings(
      settings.map((item) => {
        if (item.key === 'academicYear') {
          return { ...item, value: nextTermFilter.academicYear };
        }
        if (item.key === 'semester') {
          return { ...item, value: nextTermFilter.semester };
        }
        return item;
      })
    );
  };

  return (
    <Layout className="app-shell">
      <Sider width={300} className="app-sidebar garden-nav">
        <div className="sidebar-brand-kicker">
          Phần mềm quản trị đào tạo
        </div>
        <div className="sidebar-identity-panel">
          <div className="brand">
            <div>
              <Typography.Text className="sidebar-brand-eyebrow">Phần mềm quản trị đào tạo</Typography.Text>
              <Typography.Title level={5}>{APP_BRAND_NAME}</Typography.Title>
            </div>
          </div>
          <div className="sidebar-profile-card">
            <Avatar size={72} src={user.avatarUrl || undefined} icon={<UserOutlined />} />
            <Typography.Text strong className="sidebar-profile-name">{user.fullName}</Typography.Text>
            <Typography.Text className="sidebar-profile-title">{academicTitle}</Typography.Text>
          </div>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[activePath]}
          onClick={(event) => onPathChange(event.key as AppPath)}
          items={menuItems}
        />
        <div className="sidebar-footer">
          <Button danger className="sidebar-logout-button" icon={<PoweroffOutlined />} onClick={onLogout}>
            Đăng xuất
          </Button>
        </div>
      </Sider>
      <Layout>
        <Header className="app-header">
          <div className="header-title-block">
            <Typography.Title level={3}>Phần mềm quản trị đào tạo</Typography.Title>
          </div>
          <Space wrap className="header-actions" size={10}>
            <Select
              value={termFilter.academicYear}
              options={academicYearOptions}
              onChange={(academicYear) => handleTermChange({ ...termFilter, academicYear })}
              className="current-term-select"
              popupMatchSelectWidth={false}
              popupClassName="current-term-select-popup"
              showSearch
              filterOption={antSelectFilterOption}
            />
            <Select
              value={termFilter.semester}
              options={semesterOptions}
              onChange={(semester) => handleTermChange({ ...termFilter, semester })}
              className="current-term-select semester-term-select"
              popupMatchSelectWidth={false}
              popupClassName="current-term-select-popup"
            />
            {headerExtra}
          </Space>
        </Header>
        <Content className="app-content">{children}</Content>
      </Layout>
    </Layout>
  );
}
