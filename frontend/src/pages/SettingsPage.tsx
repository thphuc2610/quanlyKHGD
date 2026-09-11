import { ReloadOutlined, SaveOutlined } from '@ant-design/icons';
import { Button, Card, Form, Input, InputNumber, Select, Space, Switch, message } from 'antd';
import { useState } from 'react';
import PageHeader from '../components/PageHeader';
import {
  DEFAULT_SYSTEM_SETTINGS,
  readSystemSettings,
  saveSystemSettings,
  type SystemSetting
} from '../constants/workloadConfig';

type SettingsFormValues = {
  academicYear: string;
  semester: string;
  detectGuestByPosition: boolean;
  graduationInternshipWeeks: number;
  defaultInternshipDays: number;
  surveyingInternshipDays: number;
  materialTheoryHours: number;
  materialLabHours: number;
  soilTheoryHours: number;
  soilLabHours: number;
  geotechnicalTheoryHours: number;
  geotechnicalLabHours: number;
  labGroupSize: number;
  labBaseK: number;
  labBaseStudents: number;
  labIncrementPerStudent: number;
  labMinK: number;
  labMaxK: number;
  theoryBaseK: number;
  theoryBaseStudents: number;
  theoryIncrementPerStudent: number;
  theoryMinK: number;
  theoryMaxK: number;
};

const semesterOptions = [
  { value: 'Kỳ 1', label: 'Kỳ 1' },
  { value: 'Kỳ 2', label: 'Kỳ 2' },
  { value: 'Cả năm', label: 'Cả năm' }
];

const readValue = (settings: SystemSetting[], key: string, fallback: string) =>
  settings.find((item) => item.key === key)?.value ?? fallback;

const toFormValues = (settings: SystemSetting[]): SettingsFormValues => ({
  academicYear: readValue(settings, 'academicYear', '2025-2026'),
  semester: readValue(settings, 'semester', 'Kỳ 1'),
  detectGuestByPosition: readValue(settings, 'detectGuestByPosition', 'false') === 'true',
  graduationInternshipWeeks: Number(readValue(settings, 'graduationInternshipWeeks', '28')),
  defaultInternshipDays: Number(readValue(settings, 'defaultInternshipDays', '2')),
  surveyingInternshipDays: Number(readValue(settings, 'surveyingInternshipDays', '7')),
  materialTheoryHours: Number(readValue(settings, 'materialTheoryHours', '42')),
  materialLabHours: Number(readValue(settings, 'materialLabHours', '9')),
  soilTheoryHours: Number(readValue(settings, 'soilTheoryHours', '39')),
  soilLabHours: Number(readValue(settings, 'soilLabHours', '6')),
  geotechnicalTheoryHours: Number(readValue(settings, 'geotechnicalTheoryHours', '54')),
  geotechnicalLabHours: Number(readValue(settings, 'geotechnicalLabHours', '6')),
  labGroupSize: Number(readValue(settings, 'labGroupSize', '25')),
  labBaseK: Number(readValue(settings, 'labBaseK', '0.6')),
  labBaseStudents: Number(readValue(settings, 'labBaseStudents', '25')),
  labIncrementPerStudent: Number(readValue(settings, 'labIncrementPerStudent', '0.015')),
  labMinK: Number(readValue(settings, 'labMinK', '0.5')),
  labMaxK: Number(readValue(settings, 'labMaxK', '999')),
  theoryBaseK: Number(readValue(settings, 'theoryBaseK', '1')),
  theoryBaseStudents: Number(readValue(settings, 'theoryBaseStudents', '40')),
  theoryIncrementPerStudent: Number(readValue(settings, 'theoryIncrementPerStudent', '0.01')),
  theoryMinK: Number(readValue(settings, 'theoryMinK', '0.9')),
  theoryMaxK: Number(readValue(settings, 'theoryMaxK', '1.5'))
});

const mergeSettings = (values: SettingsFormValues) =>
  DEFAULT_SYSTEM_SETTINGS.map((item) => {
    const nextValue = {
      academicYear: values.academicYear,
      semester: values.semester,
      detectGuestByPosition: String(values.detectGuestByPosition),
      graduationInternshipWeeks: String(values.graduationInternshipWeeks),
      defaultInternshipDays: String(values.defaultInternshipDays),
      surveyingInternshipDays: String(values.surveyingInternshipDays),
      materialTheoryHours: String(values.materialTheoryHours),
      materialLabHours: String(values.materialLabHours),
      soilTheoryHours: String(values.soilTheoryHours),
      soilLabHours: String(values.soilLabHours),
      geotechnicalTheoryHours: String(values.geotechnicalTheoryHours),
      geotechnicalLabHours: String(values.geotechnicalLabHours),
      labGroupSize: String(values.labGroupSize),
      labBaseK: String(values.labBaseK),
      labBaseStudents: String(values.labBaseStudents),
      labIncrementPerStudent: String(values.labIncrementPerStudent),
      labMinK: String(values.labMinK),
      labMaxK: String(values.labMaxK),
      theoryBaseK: String(values.theoryBaseK),
      theoryBaseStudents: String(values.theoryBaseStudents),
      theoryIncrementPerStudent: String(values.theoryIncrementPerStudent),
      theoryMinK: String(values.theoryMinK),
      theoryMaxK: String(values.theoryMaxK)
    }[item.key];
    return nextValue === undefined ? item : { ...item, value: nextValue };
  });

export default function SettingsPage() {
  const [form] = Form.useForm<SettingsFormValues>();
  const [settings, setSettings] = useState(() => readSystemSettings());

  const saveSettings = async () => {
    const values = await form.validateFields();
    const nextSettings = mergeSettings(values);
    setSettings(nextSettings);
    saveSystemSettings(nextSettings);
    message.success('Đã lưu cấu hình');
  };

  const resetSettings = () => {
    setSettings(DEFAULT_SYSTEM_SETTINGS);
    saveSystemSettings(DEFAULT_SYSTEM_SETTINGS);
    form.setFieldsValue(toFormValues(DEFAULT_SYSTEM_SETTINGS));
    message.success('Đã khôi phục mặc định');
  };

  return (
    <div className="page-stack settings-page">
      <PageHeader
        title="Cấu hình"
        actions={
          <Space wrap>
            <Button icon={<ReloadOutlined />} onClick={resetSettings}>Khôi phục mặc định</Button>
            <Button type="primary" icon={<SaveOutlined />} onClick={saveSettings}>Lưu cấu hình</Button>
          </Space>
        }
      />

      <Form form={form} layout="vertical" initialValues={toFormValues(settings)}>
        <Card title="Mặc định nhập dữ liệu">
          <div className="settings-grid">
            <Form.Item name="academicYear" label="Năm học" rules={[{ required: true, message: 'Nhập năm học' }]}>
              <Input placeholder="2025-2026" />
            </Form.Item>
            <Form.Item name="semester" label="Học kỳ" rules={[{ required: true, message: 'Chọn học kỳ' }]}>
              <Select options={semesterOptions} />
            </Form.Item>
          </div>
        </Card>

        <Card title="Tham số tính khối lượng">
          <div className="settings-grid">
            <Form.Item name="graduationInternshipWeeks" label="Số tuần thực tập tốt nghiệp" rules={[{ required: true, message: 'Nhập số tuần' }]}>
              <InputNumber min={0} className="full-width" />
            </Form.Item>
            <Form.Item name="defaultInternshipDays" label="Số ngày thực tập mặc định" rules={[{ required: true, message: 'Nhập số ngày' }]}>
              <InputNumber min={0} className="full-width" />
            </Form.Item>
            <Form.Item name="surveyingInternshipDays" label="Số ngày thực tập trắc địa" rules={[{ required: true, message: 'Nhập số ngày' }]}>
              <InputNumber min={0} className="full-width" />
            </Form.Item>
            <Form.Item name="detectGuestByPosition" label="Nhận diện thỉnh giảng theo chức vụ" valuePropName="checked">
              <Switch checkedChildren="Bật" unCheckedChildren="Tắt" />
            </Form.Item>
          </div>
        </Card>

        <Card title="Công thức K lý thuyết và thí nghiệm">
          <div className="settings-grid">
            <Form.Item name="theoryBaseK" label="Klt gốc" rules={[{ required: true, message: 'Nhập Klt gốc' }]}>
              <InputNumber min={0} step={0.01} className="full-width" />
            </Form.Item>
            <Form.Item name="theoryBaseStudents" label="SV gốc của Klt" rules={[{ required: true, message: 'Nhập SV gốc' }]}>
              <InputNumber min={1} className="full-width" />
            </Form.Item>
            <Form.Item name="theoryIncrementPerStudent" label="Bước tăng Klt mỗi SV" rules={[{ required: true, message: 'Nhập bước tăng' }]}>
              <InputNumber min={0} step={0.001} className="full-width" />
            </Form.Item>
            <Form.Item name="theoryMinK" label="Klt tối thiểu" rules={[{ required: true, message: 'Nhập Klt tối thiểu' }]}>
              <InputNumber min={0} step={0.01} className="full-width" />
            </Form.Item>
            <Form.Item name="theoryMaxK" label="Klt tối đa" rules={[{ required: true, message: 'Nhập Klt tối đa' }]}>
              <InputNumber min={0} step={0.01} className="full-width" />
            </Form.Item>
            <Form.Item name="labGroupSize" label="Số SV mỗi nhóm thí nghiệm" rules={[{ required: true, message: 'Nhập số SV mỗi nhóm' }]}>
              <InputNumber min={1} className="full-width" />
            </Form.Item>
            <Form.Item name="labBaseK" label="Ktn gốc" rules={[{ required: true, message: 'Nhập Ktn gốc' }]}>
              <InputNumber min={0} step={0.01} className="full-width" />
            </Form.Item>
            <Form.Item name="labBaseStudents" label="SV gốc của Ktn" rules={[{ required: true, message: 'Nhập SV gốc' }]}>
              <InputNumber min={1} className="full-width" />
            </Form.Item>
            <Form.Item name="labIncrementPerStudent" label="Bước tăng Ktn mỗi SV" rules={[{ required: true, message: 'Nhập bước tăng' }]}>
              <InputNumber min={0} step={0.001} className="full-width" />
            </Form.Item>
            <Form.Item name="labMinK" label="Ktn tối thiểu" rules={[{ required: true, message: 'Nhập Ktn tối thiểu' }]}>
              <InputNumber min={0} step={0.01} className="full-width" />
            </Form.Item>
            <Form.Item name="labMaxK" label="Ktn tối đa" rules={[{ required: true, message: 'Nhập Ktn tối đa' }]}>
              <InputNumber min={0} step={0.01} className="full-width" />
            </Form.Item>
          </div>
        </Card>

        <Card title="Số tiết các môn có thí nghiệm riêng">
          <div className="settings-grid">
            <Form.Item name="materialTheoryHours" label="VLXD: tiết lý thuyết/bài tập" rules={[{ required: true, message: 'Nhập số tiết' }]}>
              <InputNumber min={0} className="full-width" />
            </Form.Item>
            <Form.Item name="materialLabHours" label="VLXD: tiết thí nghiệm mỗi nhóm" rules={[{ required: true, message: 'Nhập số tiết' }]}>
              <InputNumber min={0} className="full-width" />
            </Form.Item>
            <Form.Item name="soilTheoryHours" label="Cơ học đất: tiết lý thuyết/bài tập" rules={[{ required: true, message: 'Nhập số tiết' }]}>
              <InputNumber min={0} className="full-width" />
            </Form.Item>
            <Form.Item name="soilLabHours" label="Cơ học đất: tiết thí nghiệm mỗi nhóm" rules={[{ required: true, message: 'Nhập số tiết' }]}>
              <InputNumber min={0} className="full-width" />
            </Form.Item>
            <Form.Item name="geotechnicalTheoryHours" label="Địa kỹ thuật: tiết lý thuyết/bài tập" rules={[{ required: true, message: 'Nhập số tiết' }]}>
              <InputNumber min={0} className="full-width" />
            </Form.Item>
            <Form.Item name="geotechnicalLabHours" label="Địa kỹ thuật: tiết thí nghiệm mỗi nhóm" rules={[{ required: true, message: 'Nhập số tiết' }]}>
              <InputNumber min={0} className="full-width" />
            </Form.Item>
          </div>
        </Card>
      </Form>
    </div>
  );
}
