import type { UserRole } from '../auth/auth.types';

export type AdminUser = {
  id: number;
  username: string;
  fullName: string;
  teacherName?: string | null;
  email?: string | null;
  phone?: string | null;
  enabled: boolean;
  roles: UserRole[];
};

export type TeacherOption = {
  teacherName: string;
  departmentPh?: string | null;
};

export type CreateUserRequest = {
  username: string;
  password: string;
  fullName: string;
  teacherName?: string | null;
  roles: UserRole[];
  enabled: boolean;
};

export type UpdateUserRequest = {
  fullName: string;
  teacherName?: string | null;
  roles: UserRole[];
  enabled: boolean;
};
