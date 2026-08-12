import { API_ENDPOINTS } from '../../constants/endpoints';
import { api } from '../../lib/api';
import type { AdminUser, CreateUserRequest, TeacherOption, UpdateUserRequest } from './admin.types';

export async function getAdminUsers() {
  const { data } = await api.get<AdminUser[]>(API_ENDPOINTS.admin.users);
  return data;
}

export async function postAdminUser(payload: CreateUserRequest) {
  const { data } = await api.post<AdminUser>(API_ENDPOINTS.admin.users, payload);
  return data;
}

export async function putAdminUser(id: number, payload: UpdateUserRequest) {
  const { data } = await api.put<AdminUser>(API_ENDPOINTS.admin.user(id), payload);
  return data;
}

export async function getTeacherOptions() {
  const { data } = await api.get<TeacherOption[]>(API_ENDPOINTS.admin.teacherOptions);
  return data;
}
