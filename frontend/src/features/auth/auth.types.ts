export type UserRole = 'ADMIN' | 'GIANG_VIEN';

export type AuthUser = {
  id: number;
  username: string;
  fullName: string;
  teacherName?: string | null;
  email?: string | null;
  phone?: string | null;
  avatarUrl?: string | null;
  roles: UserRole[];
};

export type UpdateProfileRequest = {
  fullName: string;
  email?: string | null;
  phone?: string | null;
  avatarUrl?: string | null;
};

export type ChangePasswordRequest = {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};

export type LoginRequest = {
  username: string;
  password: string;
};

export type LoginResponse = {
  accessToken: string;
  tokenType: 'Bearer';
  expiresInSeconds: number;
  user: AuthUser;
};

export type ForgotPasswordRequest = {
  username: string;
};

export type ForgotPasswordResponse = {
  message: string;
  resetToken: string;
  expiresAt: string;
};

export type ResetPasswordRequest = {
  resetToken: string;
  newPassword: string;
  confirmPassword: string;
};

export type AuthSession = {
  token: string;
  user: AuthUser;
};
