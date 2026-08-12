import { API_ENDPOINTS } from '../../constants/endpoints';
import { api } from '../../lib/api';
import type {
  AuthUser,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  LoginRequest,
  LoginResponse,
  ResetPasswordRequest,
  UpdateProfileRequest
} from './auth.types';

export async function postLogin(payload: LoginRequest) {
  const { data } = await api.post<LoginResponse>(API_ENDPOINTS.auth.login, payload);
  return data;
}

export async function getCurrentUser() {
  const { data } = await api.get<AuthUser>(API_ENDPOINTS.auth.me);
  return data;
}

export async function putCurrentUser(payload: UpdateProfileRequest) {
  const { data } = await api.put<AuthUser>(API_ENDPOINTS.auth.me, payload);
  return data;
}

export async function postChangePassword(payload: ChangePasswordRequest) {
  await api.post(API_ENDPOINTS.auth.changePassword, payload);
}

export async function postForgotPassword(payload: ForgotPasswordRequest) {
  const { data } = await api.post<ForgotPasswordResponse>(API_ENDPOINTS.auth.forgotPassword, payload);
  return data;
}

export async function postResetPassword(payload: ResetPasswordRequest) {
  await api.post(API_ENDPOINTS.auth.resetPassword, payload);
}
