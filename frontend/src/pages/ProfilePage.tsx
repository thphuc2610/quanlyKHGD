import {
  EditOutlined,
  IdcardOutlined,
  LockOutlined,
  MailOutlined,
  PhoneOutlined,
  RightOutlined,
  SafetyCertificateOutlined,
  UploadOutlined,
  UserOutlined
} from '@ant-design/icons';
import {
  Alert,
  Avatar,
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Form,
  Input,
  Modal,
  Row,
  Space,
  Statistic,
  Typography,
  Upload,
  message
} from 'antd';
import { useState } from 'react';
import PageHeader from '../components/PageHeader';
import { ACADEMIC_TITLE_UPDATED_EVENT, SIDEBAR_TITLE_KEY } from '../components/AppShell';
import { authService } from '../features/auth/auth.service';
import type { AuthSession, ChangePasswordRequest, UpdateProfileRequest } from '../features/auth/auth.types';
import { useTeacherWorkloadsQuery } from '../features/workloads/workloads.query';

type ProfilePageProps = {
  session: AuthSession;
  onSessionChange: (session: AuthSession) => void;
};

type ProfileSection = 'personal' | 'account' | 'teaching';

type ExtendedProfile = {
  englishName?: string;
  birthDate?: string;
  gender?: string;
  birthDateGender?: string;
  hometown?: string;
  academicRank?: string;
  jobTitle?: string;
  position?: string;
  workplace?: string;
  officeAddress?: string;
};

type ProfileFormValues = UpdateProfileRequest & {
  academicTitle?: string;
} & ExtendedProfile;

const formatNumber = (value?: number) => Number(value ?? 0).toLocaleString('vi-VN', { maximumFractionDigits: 2 });
const sameName = (left?: string | null, right?: string | null) =>
  (left ?? '').trim().toLowerCase() === (right ?? '').trim().toLowerCase();
const emptyValue = 'Chưa cập nhật';
type ProfileInfoRow = [string, string];

const profileRows = (user: AuthSession['user'], academicTitle: string, displayTeacherName: string, profile: ExtendedProfile): ProfileInfoRow[] => [
  ['Họ và tên', user.fullName],
  ['Tên tiếng Anh', profile.englishName || emptyValue],
  ['Ngày sinh', profile.birthDate || profile.birthDateGender || emptyValue],
  ['Giới tính', profile.gender || emptyValue],
  ['Quê quán', profile.hometown || emptyValue],
  ['Học hàm', profile.academicRank || emptyValue],
  ['Học vị', academicTitle],
  ['Chức danh nghề nghiệp', profile.jobTitle || 'Giảng viên'],
  ['Chức vụ', profile.position || emptyValue],
  ['Nơi công tác', profile.workplace || displayTeacherName],
  ['Địa chỉ cơ quan', profile.officeAddress || emptyValue],
  ['Số điện thoại', user.phone || emptyValue],
  ['Email', user.email || emptyValue]
];

const accountRows = (user: AuthSession['user'], roleLabel: string): ProfileInfoRow[] => [
  ['Tên đăng nhập', user.username],
  ['Tên hiển thị', user.fullName],
  ['Email đăng nhập', user.email || emptyValue],
  ['Số điện thoại', user.phone || emptyValue],
  ['Vai trò', roleLabel],
  ['Trạng thái', 'Đang hoạt động']
];

function ProfileInfoTable({ rows }: { rows: ProfileInfoRow[] }) {
  return (
    <div className="profile-info-table">
      {rows.map(([label, value]) => (
        <div className="profile-info-row" key={label}>
          <div className="profile-info-label">{label}</div>
          <div className="profile-info-value">{value}</div>
        </div>
      ))}
    </div>
  );
}

const profileStorageKey = (userId: number) => `klgd.extendedProfile.${userId}`;

function readExtendedProfile(userId: number): ExtendedProfile {
  const raw = window.localStorage.getItem(profileStorageKey(userId));
  if (!raw) {
    return {};
  }
  try {
    return JSON.parse(raw) as ExtendedProfile;
  } catch {
    window.localStorage.removeItem(profileStorageKey(userId));
    return {};
  }
}

function saveExtendedProfile(userId: number, profile: ExtendedProfile) {
  window.localStorage.setItem(profileStorageKey(userId), JSON.stringify(profile));
}

const readFileAsDataUrl = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });

export default function ProfilePage({ session, onSessionChange }: ProfilePageProps) {
  const { user } = session;
  const teachers = useTeacherWorkloadsQuery();
  const [activeSection, setActiveSection] = useState<ProfileSection>('personal');
  const [saving, setSaving] = useState(false);
  const [changingPassword, setChangingPassword] = useState(false);
  const [editing, setEditing] = useState(false);
  const [showPasswordForm, setShowPasswordForm] = useState(false);
  const [extendedProfile, setExtendedProfile] = useState<ExtendedProfile>(() => readExtendedProfile(session.user.id));
  const [form] = Form.useForm<ProfileFormValues>();
  const [passwordForm] = Form.useForm<ChangePasswordRequest>();
  const teacherProfile = (teachers.data ?? []).find(
    (item) => sameName(item.teacherName, user.teacherName) || sameName(item.teacherName, user.fullName)
  );
  const displayTeacherName = user.teacherName || teacherProfile?.teacherName || user.fullName;
  const roleLabel = user.roles.includes('ADMIN') ? 'Quản trị viên' : 'Giảng viên';
  const academicTitle = window.localStorage.getItem(SIDEBAR_TITLE_KEY) || 'Tiến sĩ';

  const openEdit = () => {
    form.setFieldsValue({
      fullName: user.fullName,
      email: user.email,
      phone: user.phone,
      avatarUrl: user.avatarUrl,
      academicTitle,
      ...extendedProfile,
      birthDate: extendedProfile.birthDate || extendedProfile.birthDateGender,
      gender: extendedProfile.gender
    });
    setEditing(true);
  };

  const handleSave = async (values: ProfileFormValues) => {
    setSaving(true);
    try {
      const {
        academicTitle: nextAcademicTitle,
        englishName,
        birthDate,
        gender,
        birthDateGender,
        hometown,
        academicRank,
        jobTitle,
        position,
        workplace,
        officeAddress,
        ...profileValues
      } = values;
      const nextExtendedProfile: ExtendedProfile = {
        englishName: englishName?.trim(),
        birthDate: birthDate?.trim(),
        gender: gender?.trim(),
        birthDateGender: birthDateGender?.trim(),
        hometown: hometown?.trim(),
        academicRank: academicRank?.trim(),
        jobTitle: jobTitle?.trim(),
        position: position?.trim(),
        workplace: workplace?.trim(),
        officeAddress: officeAddress?.trim()
      };
      const nextSession = await authService.updateProfile(session, profileValues);
      window.localStorage.setItem(SIDEBAR_TITLE_KEY, nextAcademicTitle?.trim() || 'Tiến sĩ');
      saveExtendedProfile(user.id, nextExtendedProfile);
      setExtendedProfile(nextExtendedProfile);
      window.dispatchEvent(new Event(ACADEMIC_TITLE_UPDATED_EVENT));
      onSessionChange(nextSession);
      setEditing(false);
      message.success('Đã cập nhật hồ sơ');
    } finally {
      setSaving(false);
    }
  };

  const handleChangePassword = async (values: ChangePasswordRequest) => {
    setChangingPassword(true);
    try {
      await authService.changePassword(values);
      passwordForm.resetFields();
      setShowPasswordForm(false);
      message.success('Đã đổi mật khẩu');
    } catch {
      message.error('Không thể đổi mật khẩu. Kiểm tra mật khẩu hiện tại và xác nhận mật khẩu mới.');
    } finally {
      setChangingPassword(false);
    }
  };

  return (
    <div className="page-stack">
      <Card>
        <Space size={18} align="center" wrap>
          <Avatar size={88} src={user.avatarUrl || undefined} icon={<UserOutlined />} />
          <div>
            <Typography.Title level={3}>{user.fullName}</Typography.Title>
            <Typography.Text type="secondary">{academicTitle} · {user.email || user.username}</Typography.Text>
          </div>
        </Space>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} md={7} xl={6}>
          <Card className="profile-settings-nav">
            <button
              type="button"
              className={activeSection === 'personal' ? 'profile-settings-item is-active' : 'profile-settings-item'}
              onClick={() => setActiveSection('personal')}
            >
              <span className="profile-settings-item-label">
                <UserOutlined />
                <span>Thông tin cá nhân</span>
              </span>
              <RightOutlined />
            </button>
            <button
              type="button"
              className={activeSection === 'account' ? 'profile-settings-item is-active' : 'profile-settings-item'}
              onClick={() => setActiveSection('account')}
            >
              <span className="profile-settings-item-label">
                <SafetyCertificateOutlined />
                <span>Thông tin tài khoản</span>
              </span>
              <RightOutlined />
            </button>
          </Card>
        </Col>

        <Col xs={24} md={17} xl={18}>
          {activeSection === 'personal' && (
            <Card
              title="Thông tin cá nhân"
              extra={<Button type="primary" icon={<EditOutlined />} onClick={openEdit}>Chỉnh sửa</Button>}
            >
              <ProfileInfoTable rows={profileRows(user, academicTitle, displayTeacherName, extendedProfile)} />
            </Card>
          )}

          {activeSection === 'account' && (
            <Space direction="vertical" size={16} className="full-width">
              <Card title="Thông tin tài khoản">
                <ProfileInfoTable rows={accountRows(user, roleLabel)} />
              </Card>

              <Card
                title="Đổi mật khẩu"
                extra={!showPasswordForm && <Button type="primary" icon={<LockOutlined />} onClick={() => setShowPasswordForm(true)}>Đổi mật khẩu</Button>}
              >
                {showPasswordForm ? (
                  <>
                    <Alert
                      type="info"
                      showIcon
                      message="Nhập mật khẩu hiện tại trước khi đặt mật khẩu mới."
                      className="profile-account-alert"
                    />
                    <Form form={passwordForm} layout="vertical" onFinish={handleChangePassword} className="profile-password-form">
                      <Form.Item name="currentPassword" label="Mật khẩu hiện tại" rules={[{ required: true, message: 'Nhập mật khẩu hiện tại' }]}>
                        <Input.Password prefix={<LockOutlined />} />
                      </Form.Item>
                      <Form.Item name="newPassword" label="Mật khẩu mới" rules={[{ required: true, min: 6, message: 'Mật khẩu tối thiểu 6 ký tự' }]}>
                        <Input.Password prefix={<LockOutlined />} />
                      </Form.Item>
                      <Form.Item
                        name="confirmPassword"
                        label="Xác nhận mật khẩu mới"
                        dependencies={['newPassword']}
                        rules={[
                          { required: true, message: 'Xác nhận mật khẩu mới' },
                          ({ getFieldValue }) => ({
                            validator(_, value) {
                              if (!value || getFieldValue('newPassword') === value) {
                                return Promise.resolve();
                              }
                              return Promise.reject(new Error('Mật khẩu xác nhận không khớp'));
                            }
                          })
                        ]}
                      >
                        <Input.Password prefix={<LockOutlined />} />
                      </Form.Item>
                      <Space wrap>
                        <Button type="primary" htmlType="submit" loading={changingPassword} icon={<LockOutlined />}>
                          Lưu mật khẩu
                        </Button>
                        <Button onClick={() => {
                          passwordForm.resetFields();
                          setShowPasswordForm(false);
                        }}>
                          Hủy
                        </Button>
                      </Space>
                    </Form>
                  </>
                ) : (
                  <Typography.Text type="secondary">Form đổi mật khẩu sẽ hiển thị khi chọn đổi mật khẩu.</Typography.Text>
                )}
              </Card>
            </Space>
          )}

          {activeSection === 'teaching' && (
            <Space direction="vertical" size={16} className="full-width">
              <Card title="Thống kê giảng dạy">
                {teacherProfile ? (
                  <Descriptions bordered column={1} size="small">
                    <Descriptions.Item label="Bộ môn">{teacherProfile.departmentPh}</Descriptions.Item>
                    <Descriptions.Item label="Số lớp">{teacherProfile.classCount}</Descriptions.Item>
                    <Descriptions.Item label="Tổng tín chỉ">{formatNumber(teacherProfile.totalCredits)}</Descriptions.Item>
                    <Descriptions.Item label="Tổng sinh viên">{formatNumber(teacherProfile.totalStudents)}</Descriptions.Item>
                    <Descriptions.Item label="Giờ chuẩn">{formatNumber(teacherProfile.totalStandardHours)}</Descriptions.Item>
                  </Descriptions>
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có dữ liệu giảng dạy" />
                )}
              </Card>

              {teacherProfile && (
                <Row gutter={[16, 16]}>
                  <Col xs={24} md={6}><Card className="stat-card"><Statistic title="Số lớp" value={teacherProfile.classCount} /></Card></Col>
                  <Col xs={24} md={6}><Card className="stat-card"><Statistic title="Tổng tín chỉ" value={teacherProfile.totalCredits} precision={2} /></Card></Col>
                  <Col xs={24} md={6}><Card className="stat-card"><Statistic title="Tổng sinh viên" value={teacherProfile.totalStudents} /></Card></Col>
                  <Col xs={24} md={6}><Card className="stat-card"><Statistic title="Giờ chuẩn" value={teacherProfile.totalStandardHours} precision={2} /></Card></Col>
                </Row>
              )}
            </Space>
          )}
        </Col>
      </Row>

      <Modal
        title="Chỉnh sửa hồ sơ"
        open={editing}
        onCancel={() => setEditing(false)}
        onOk={() => form.submit()}
        okText="Lưu"
        cancelText="Hủy"
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleSave}>
          <Form.Item shouldUpdate noStyle>
            {() => (
              <Space align="center" size={16} className="profile-avatar-editor">
                <Avatar size={72} src={form.getFieldValue('avatarUrl') || undefined} icon={<UserOutlined />} />
                <Upload
                  maxCount={1}
                  accept="image/png,image/jpeg"
                  showUploadList={false}
                  beforeUpload={async (file) => {
                    const dataUrl = await readFileAsDataUrl(file);
                    form.setFieldValue('avatarUrl', dataUrl);
                    return false;
                  }}
                >
                  <Button icon={<UploadOutlined />}>Tải ảnh đại diện</Button>
                </Upload>
              </Space>
            )}
          </Form.Item>
          <Form.Item name="avatarUrl" hidden><Input /></Form.Item>
          <Form.Item name="fullName" label="Họ và tên" rules={[{ required: true, message: 'Nhập họ và tên' }]}>
            <Input prefix={<UserOutlined />} />
          </Form.Item>
          <Form.Item name="englishName" label="Tên tiếng Anh">
            <Input placeholder="Nhập tên tiếng Anh" />
          </Form.Item>
          <Form.Item name="birthDate" label="Ngày sinh">
            <Input placeholder="Ví dụ: 01/01/1980" />
          </Form.Item>
          <Form.Item name="gender" label="Giới tính">
            <Input placeholder="Ví dụ: Nam" />
          </Form.Item>
          <Form.Item name="hometown" label="Quê quán">
            <Input placeholder="Nhập quê quán" />
          </Form.Item>
          <Form.Item name="academicRank" label="Học hàm">
            <Input prefix={<IdcardOutlined />} placeholder="Ví dụ: Phó giáo sư, Giáo sư" />
          </Form.Item>
          <Form.Item name="academicTitle" label="Học vị / chức danh hiển thị">
            <Input prefix={<IdcardOutlined />} placeholder="Ví dụ: Tiến sĩ, Thạc sĩ, PGS.TS" />
          </Form.Item>
          <Form.Item name="jobTitle" label="Chức danh nghề nghiệp">
            <Input placeholder="Ví dụ: Giảng viên, Giảng viên chính" />
          </Form.Item>
          <Form.Item name="position" label="Chức vụ">
            <Input placeholder="Nhập chức vụ" />
          </Form.Item>
          <Form.Item name="workplace" label="Nơi công tác">
            <Input placeholder="Nhập nơi công tác" />
          </Form.Item>
          <Form.Item name="officeAddress" label="Địa chỉ cơ quan">
            <Input placeholder="Nhập địa chỉ cơ quan" />
          </Form.Item>
          <Form.Item name="email" label="Email" rules={[{ type: 'email', message: 'Email không đúng định dạng' }]}>
            <Input prefix={<MailOutlined />} />
          </Form.Item>
          <Form.Item name="phone" label="Số điện thoại">
            <Input prefix={<PhoneOutlined />} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
