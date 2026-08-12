import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getAdminUsers, getTeacherOptions, postAdminUser, putAdminUser } from './admin.api';
import type { CreateUserRequest, UpdateUserRequest } from './admin.types';

export const adminQueryKeys = {
  users: ['admin', 'users'] as const,
  teacherOptions: ['admin', 'teacher-options'] as const
};

export function useAdminUsersQuery() {
  return useQuery({ queryKey: adminQueryKeys.users, queryFn: getAdminUsers });
}

export function useTeacherOptionsQuery() {
  return useQuery({ queryKey: adminQueryKeys.teacherOptions, queryFn: getTeacherOptions });
}

export function useCreateAdminUserMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateUserRequest) => postAdminUser(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminQueryKeys.users })
  });
}

export function useUpdateAdminUserMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: UpdateUserRequest }) => putAdminUser(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminQueryKeys.users })
  });
}
