import { DownloadOutlined } from '@ant-design/icons';
import { Button } from 'antd';
import { useEffect, useState } from 'react';
import AppShell from './components/AppShell';
import type { TermFilter } from './components/AppShell';
import { APP_PATHS, type AppPath } from './constants/paths';
import { readSystemSettings } from './constants/workloadConfig';
import { authService } from './features/auth/auth.service';
import { readStoredSession } from './features/auth/auth.storage';
import type { AuthSession } from './features/auth/auth.types';
import { useDownloadWorkloadReportMutation } from './features/reports/reports.query';
import DashboardPage from './pages/DashboardPage';
import ImportPage from './pages/ImportPage';
import LoginPage from './pages/LoginPage';
import ProfilePage from './pages/ProfilePage';
import RulesPage from './pages/RulesPage';
import TeachersPage from './pages/TeachersPage';

const ADMIN_ONLY_PATHS: AppPath[] = [APP_PATHS.dashboard, APP_PATHS.imports, APP_PATHS.rules];

export default function App() {
  const [session, setSession] = useState<AuthSession | null>(() => readStoredSession());
  const [page, setPage] = useState<AppPath>(APP_PATHS.dashboard);
  const [termFilter, setTermFilter] = useState<TermFilter>(() => {
    const settings = readSystemSettings();
    return {
      academicYear: settings.find((item) => item.key === 'academicYear')?.value ?? '2025-2026',
      semester: settings.find((item) => item.key === 'semester')?.value ?? 'Kỳ 1'
    };
  });
  const isAdmin = session?.user.roles.includes('ADMIN') ?? false;
  const downloadReport = useDownloadWorkloadReportMutation();

  useEffect(() => {
    if (!isAdmin && ADMIN_ONLY_PATHS.includes(page)) {
      setPage(APP_PATHS.teachers);
    }
    if (page === APP_PATHS.profile) {
      setPage(APP_PATHS.settings);
    }
    if (isAdmin && page === APP_PATHS.teachers) {
      setPage(APP_PATHS.dashboard);
    }
  }, [isAdmin, page]);

  if (!session) {
    return <LoginPage onLoggedIn={(nextSession) => {
      setSession(nextSession);
      setPage(APP_PATHS.dashboard);
    }} />;
  }

  const handleLogout = () => {
    authService.logout();
    setSession(null);
    setPage(APP_PATHS.dashboard);
  };

  const headerExtra = page === APP_PATHS.imports && isAdmin ? (
    <Button
      type="primary"
      icon={<DownloadOutlined />}
      loading={downloadReport.isPending}
      onClick={() => downloadReport.mutate({ academicYear: termFilter.academicYear, semester: termFilter.semester })}
    >
      Xuất báo cáo
    </Button>
  ) : undefined;

  return (
    <AppShell
      activePath={page}
      onPathChange={setPage}
      session={session}
      user={session.user}
      onSessionChange={setSession}
      onLogout={handleLogout}
      termFilter={termFilter}
      onTermFilterChange={setTermFilter}
      headerExtra={headerExtra}
    >
      {page === APP_PATHS.dashboard && isAdmin && (
        <DashboardPage termFilter={termFilter} canDownloadReport={isAdmin} />
      )}
      {page === APP_PATHS.imports && isAdmin && <ImportPage termFilter={termFilter} onTermFilterChange={setTermFilter} />}
      {page === APP_PATHS.teachers && <TeachersPage user={session.user} />}
      {page === APP_PATHS.settings && <ProfilePage session={session} onSessionChange={setSession} />}
      {page === APP_PATHS.rules && isAdmin && <RulesPage />}
    </AppShell>
  );
}
