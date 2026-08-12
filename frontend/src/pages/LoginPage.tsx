import { ArrowLeftOutlined, KeyOutlined, LockOutlined, UserOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Form, Input, Space, Typography } from 'antd';
import { useState } from 'react';
import { useForgotPasswordMutation, useLoginMutation, useResetPasswordMutation } from '../features/auth/auth.query';
import type {
  AuthSession,
  ForgotPasswordRequest,
  LoginRequest,
  ResetPasswordRequest
} from '../features/auth/auth.types';

type LoginPageProps = {
  onLoggedIn: (session: AuthSession) => void;
};

type LoginMode = 'login' | 'forgot' | 'reset';

export default function LoginPage({ onLoggedIn }: LoginPageProps) {
  const [mode, setMode] = useState<LoginMode>('login');
  const [resetToken, setResetToken] = useState('');
  const loginMutation = useLoginMutation();
  const forgotPasswordMutation = useForgotPasswordMutation();
  const resetPasswordMutation = useResetPasswordMutation();

  const handleSubmit = (values: LoginRequest) => {
    loginMutation.mutate(values, { onSuccess: onLoggedIn });
  };

  const handleForgotPassword = (values: ForgotPasswordRequest) => {
    forgotPasswordMutation.mutate(values, {
      onSuccess: (response) => {
        setResetToken(response.resetToken);
        setMode('reset');
      }
    });
  };

  const handleResetPassword = (values: Omit<ResetPasswordRequest, 'resetToken'> & { resetToken?: string }) => {
    resetPasswordMutation.mutate(
      {
        resetToken: values.resetToken || resetToken,
        newPassword: values.newPassword,
        confirmPassword: values.confirmPassword
      },
      {
        onSuccess: () => {
          setMode('login');
          setResetToken('');
        }
      }
    );
  };

  return (
    <div className="login-page">
      <Card className="login-card">
        {mode !== 'login' && (
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            className="login-back-button"
            onClick={() => setMode('login')}
          >
            Quay lại đăng nhập
          </Button>
        )}

        {mode === 'login' && (
          <>
            <Typography.Title level={3}>Đăng nhập KLGD</Typography.Title>
            <Typography.Paragraph type="secondary">
              Sử dụng tài khoản quản trị hoặc giảng viên để truy cập hệ thống.
            </Typography.Paragraph>
            {loginMutation.isError && (
              <Alert type="error" showIcon message="Tên đăng nhập hoặc mật khẩu không đúng." className="login-alert" />
            )}
            {resetPasswordMutation.isSuccess && (
              <Alert type="success" showIcon message="Mật khẩu đã được đặt lại. Bạn có thể đăng nhập bằng mật khẩu mới." className="login-alert" />
            )}
            <Form layout="vertical" initialValues={{ username: 'admin', password: 'admin123' }} onFinish={handleSubmit}>
              <Form.Item name="username" label="Tên đăng nhập" rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}>
                <Input prefix={<UserOutlined />} autoComplete="username" />
              </Form.Item>
              <Form.Item name="password" label="Mật khẩu" rules={[{ required: true, message: 'Nhập mật khẩu' }]}>
                <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
              </Form.Item>
              <Space direction="vertical" size={12} className="full-width">
                <Button type="primary" htmlType="submit" block loading={loginMutation.isPending}>
                  Đăng nhập
                </Button>
                <Button type="link" block onClick={() => setMode('forgot')}>
                  Quên mật khẩu?
                </Button>
              </Space>
            </Form>
          </>
        )}

        {mode === 'forgot' && (
          <>
            <Typography.Title level={3}>Quên mật khẩu</Typography.Title>
            <Typography.Paragraph type="secondary">
              Nhập tên đăng nhập để tạo mã đặt lại mật khẩu. Bản demo sẽ hiển thị mã trực tiếp trên màn hình.
            </Typography.Paragraph>
            {forgotPasswordMutation.isError && (
              <Alert type="error" showIcon message="Không tìm thấy tài khoản phù hợp." className="login-alert" />
            )}
            <Form layout="vertical" onFinish={handleForgotPassword}>
              <Form.Item name="username" label="Tên đăng nhập" rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}>
                <Input prefix={<UserOutlined />} autoComplete="username" />
              </Form.Item>
              <Button type="primary" htmlType="submit" block loading={forgotPasswordMutation.isPending}>
                Tạo mã đặt lại
              </Button>
            </Form>
          </>
        )}

        {mode === 'reset' && (
          <>
            <Typography.Title level={3}>Đặt lại mật khẩu</Typography.Title>
            <Typography.Paragraph type="secondary">
              Nhập mã đặt lại và mật khẩu mới. Mã demo hiện tại là:
            </Typography.Paragraph>
            <Alert type="info" showIcon message={resetToken} className="login-alert reset-token-alert" />
            {resetPasswordMutation.isError && (
              <Alert type="error" showIcon message="Mã đặt lại không hợp lệ, đã hết hạn hoặc mật khẩu xác nhận không khớp." className="login-alert" />
            )}
            <Form layout="vertical" initialValues={{ resetToken }} onFinish={handleResetPassword}>
              <Form.Item name="resetToken" label="Mã đặt lại" rules={[{ required: true, message: 'Nhập mã đặt lại' }]}>
                <Input prefix={<KeyOutlined />} />
              </Form.Item>
              <Form.Item name="newPassword" label="Mật khẩu mới" rules={[{ required: true, message: 'Nhập mật khẩu mới' }]}>
                <Input.Password prefix={<LockOutlined />} autoComplete="new-password" />
              </Form.Item>
              <Form.Item name="confirmPassword" label="Xác nhận mật khẩu" rules={[{ required: true, message: 'Nhập lại mật khẩu mới' }]}>
                <Input.Password prefix={<LockOutlined />} autoComplete="new-password" />
              </Form.Item>
              <Button type="primary" htmlType="submit" block loading={resetPasswordMutation.isPending}>
                Đặt lại mật khẩu
              </Button>
            </Form>
          </>
        )}
      </Card>
    </div>
  );
}
