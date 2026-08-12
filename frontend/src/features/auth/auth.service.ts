import { getCurrentUser, postChangePassword, postForgotPassword, postLogin, postResetPassword, putCurrentUser } from './auth.api';
import { clearStoredSession, saveStoredSession } from './auth.storage';
import type { AuthSession, ChangePasswordRequest, ForgotPasswordRequest, LoginRequest, ResetPasswordRequest, UpdateProfileRequest } from './auth.types';

export const authService = {
  async login(payload: LoginRequest) {
    const response = await postLogin(payload);
    const session = { token: response.accessToken, user: response.user };
    saveStoredSession(session);
    return session;
  },
  forgotPassword(payload: ForgotPasswordRequest) {
    return postForgotPassword(payload);
  },
  resetPassword(payload: ResetPasswordRequest) {
    return postResetPassword(payload);
  },
  changePassword(payload: ChangePasswordRequest) {
    return postChangePassword(payload);
  },
  getCurrentUser,
  async updateProfile(session: AuthSession, payload: UpdateProfileRequest) {
    const user = await putCurrentUser(payload);
    const nextSession = { ...session, user };
    saveStoredSession(nextSession);
    return nextSession;
  },
  logout() {
    clearStoredSession();
  }
};
