import { EditOutlined, SearchOutlined, UserAddOutlined } from '@ant-design/icons';
import { Button, Card, Checkbox, Empty, Form, Input, Modal, Select, Space, Switch, Table, Tag, message } from 'antd';
import { useMemo, useState } from 'react';
import PageHeader from '../components/PageHeader';
import { sttColumn, widePagination } from '../constants/table';
import {
  useAdminUsersQuery,
  useCreateAdminUserMutation,
  useTeacherOptionsQuery,
  useUpdateAdminUserMutation
} from '../features/admin/admin.query';
import type { AdminUser, CreateUserRequest, UpdateUserRequest } from '../features/admin/admin.types';
import type { UserRole } from '../features/auth/auth.types';

type AccountFormValues = CreateUserRequest & {
  id?: number;
};

const roleOptions = [
  { label: 'Admin', value: 'ADMIN' },
  { label: 'Giảng viên', value: 'GIANG_VIEN' }
];

const roleColor: Record<UserRole, string> = {
  ADMIN: 'cyan',
  GIANG_VIEN: 'green'
};

export default function AdminAccountsPage() {
  const users = useAdminUsersQuery();
  const teacherOptions = useTeacherOptionsQuery();
  const createUser = useCreateAdminUserMutation();
  const updateUser = useUpdateAdminUserMutation();
  const [keyword, setKeyword] = useState('');
  const [editingUser, setEditingUser] = useState<AdminUser | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm<AccountFormValues>();
  const watchedRoles = Form.useWatch('roles', form) ?? [];
  const isEditing = Boolean(editingUser);

  const selectTeacherOptions = useMemo(() => {
    const grouped = new Map<string, { teacherName: string; departments: Set<string> }>();
    (teacherOptions.data ?? []).forEach((item) => {
      const teacherName = item.teacherName.trim();
      const key = teacherName.toLowerCase();
      const current = grouped.get(key) ?? { teacherName, departments: new Set<string>() };
      if (item.departmentPh?.trim()) {
        current.departments.add(item.departmentPh.trim());
      }
      grouped.set(key, current);
    });
    return Array.from(grouped.values())
      .sort((left, right) => left.teacherName.localeCompare(right.teacherName, 'vi'))
      .map((item) => {
        const departments = Array.from(item.departments).sort((left, right) => left.localeCompare(right, 'vi'));
        return {
          label: departments.length > 0 ? `${item.teacherName} - ${departments.join(', ')}` : item.teacherName,
          value: item.teacherName
        };
      });
  }, [teacherOptions.data]);

  const filteredUsers = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    const rows = users.data ?? [];
    if (!normalizedKeyword) {
      return rows;
    }
    return rows.filter((item) =>
      `${item.username} ${item.fullName} ${item.teacherName ?? ''}`.toLowerCase().includes(normalizedKeyword)
    );
  }, [keyword, users.data]);

  const openCreate = () => {
    setEditingUser(null);
    form.setFieldsValue({
      username: '',
      password: '',
      fullName: '',
      teacherName: null,
      roles: ['GIANG_VIEN'],
      enabled: true
    });
    setModalOpen(true);
  };

  const openEdit = (user: AdminUser) => {
    setEditingUser(user);
    form.setFieldsValue({
      id: user.id,
      username: user.username,
      fullName: user.fullName,
      teacherName: user.teacherName,
      roles: user.roles,
      enabled: user.enabled
    });
    setModalOpen(true);
  };

  const handleSubmit = async (values: AccountFormValues) => {
    try {
      if (isEditing && editingUser) {
        const payload: UpdateUserRequest = {
          fullName: values.fullName,
          teacherName: values.teacherName,
          roles: values.roles,
          enabled: values.enabled
        };
        await updateUser.mutateAsync({ id: editingUser.id, payload });
        message.success('Đã cập nhật tài khoản');
      } else {
        await createUser.mutateAsync(values);
        message.success('Đã cấp tài khoản');
      }
      setModalOpen(false);
    } catch {
      message.error('Không thể lưu tài khoản. Kiểm tra tên đăng nhập hoặc liên kết giảng viên.');
    }
  };

  return (
    <div className="page-stack">
      <PageHeader
        title="Tài khoản giảng viên"
        actions={<Button type="primary" icon={<UserAddOutlined />} onClick={openCreate}>Cấp tài khoản</Button>}
      />

      <Card title="Danh sách tài khoản">
        <Space direction="vertical" size={16} className="full-width">
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder="Tìm theo tài khoản, họ tên hoặc giảng viên liên kết"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            className="search-input"
          />
          {!users.isLoading && filteredUsers.length === 0 ? (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có tài khoản" />
          ) : (
            <Table
              rowKey="id"
              tableLayout="fixed"
              size="small"
              loading={users.isLoading}
              dataSource={filteredUsers}
              pagination={widePagination}
              columns={[
                sttColumn,
                { title: 'Tài khoản', dataIndex: 'username', width: 150, className: 'text-left' },
                { title: 'Họ tên', dataIndex: 'fullName', className: 'text-left' },
                { title: 'Giảng viên liên kết', dataIndex: 'teacherName', className: 'text-left', render: (value) => value || 'Chưa gắn' },
                {
                  title: 'Vai trò',
                  width: 180,
                  render: (_, record: AdminUser) => (
                    <Space wrap size={4}>
                      {record.roles.map((role) => <Tag key={role} color={roleColor[role]}>{role}</Tag>)}
                    </Space>
                  )
                },
                {
                  title: 'Trạng thái',
                  width: 120,
                  render: (_, record: AdminUser) => <Tag color={record.enabled ? 'green' : 'red'}>{record.enabled ? 'Đang dùng' : 'Đã khóa'}</Tag>
                },
                {
                  title: '',
                  width: 90,
                  render: (_, record: AdminUser) => (
                    <Button icon={<EditOutlined />} onClick={() => openEdit(record)} />
                  )
                }
              ]}
            />
          )}
        </Space>
      </Card>

      <Modal
        title={isEditing ? 'Cập nhật tài khoản' : 'Cấp tài khoản giảng viên'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => form.submit()}
        okText="Lưu"
        cancelText="Hủy"
        confirmLoading={createUser.isPending || updateUser.isPending}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="username" label="Tên đăng nhập" rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}>
            <Input disabled={isEditing} prefix={<UserAddOutlined />} />
          </Form.Item>
          {!isEditing && (
            <Form.Item name="password" label="Mật khẩu tạm" rules={[{ required: true, min: 6, message: 'Mật khẩu tối thiểu 6 ký tự' }]}>
              <Input.Password />
            </Form.Item>
          )}
          <Form.Item name="fullName" label="Họ và tên" rules={[{ required: true, message: 'Nhập họ và tên' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="roles" label="Vai trò" rules={[{ required: true, message: 'Chọn vai trò' }]}>
            <Checkbox.Group options={roleOptions} />
          </Form.Item>
          <Form.Item
            name="teacherName"
            label="Giảng viên liên kết"
            rules={[{ required: watchedRoles.includes('GIANG_VIEN'), message: 'Chọn giảng viên liên kết' }]}
          >
            <Select
              allowClear
              showSearch
              loading={teacherOptions.isLoading}
              options={selectTeacherOptions}
              optionFilterProp="label"
              placeholder="Chọn đúng giảng viên trong danh sách"
            />
          </Form.Item>
          <Form.Item name="enabled" label="Kích hoạt" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
