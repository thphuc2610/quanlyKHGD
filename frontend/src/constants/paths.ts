export const APP_PATHS = {
  dashboard: 'dashboard',
  imports: 'imports',
  accounts: 'accounts',
  teachers: 'teachers',
  profile: 'profile',
  rules: 'rules',
  settings: 'settings'
} as const;

export type AppPath = (typeof APP_PATHS)[keyof typeof APP_PATHS];
