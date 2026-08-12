import type { AuthUser } from './auth.types';

export const authMock = {
  admin: {
    id: 1,
    username: 'admin',
    fullName: 'Quản trị hệ thống',
    teacherName: null,
    roles: ['ADMIN']
  } satisfies AuthUser
};
