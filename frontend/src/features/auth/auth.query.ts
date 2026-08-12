import { useMutation, useQuery } from '@tanstack/react-query';
import { authService } from './auth.service';

export const authQueryKeys = {
  me: ['auth', 'me'] as const
};

export function useCurrentUserQuery(enabled: boolean) {
  return useQuery({ queryKey: authQueryKeys.me, queryFn: authService.getCurrentUser, enabled });
}

export function useLoginMutation() {
  return useMutation({ mutationFn: authService.login });
}

export function useForgotPasswordMutation() {
  return useMutation({ mutationFn: authService.forgotPassword });
}

export function useResetPasswordMutation() {
  return useMutation({ mutationFn: authService.resetPassword });
}

export function useChangePasswordMutation() {
  return useMutation({ mutationFn: authService.changePassword });
}
