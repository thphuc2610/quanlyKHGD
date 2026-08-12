import { Space, Typography } from 'antd';
import type { ReactNode } from 'react';

type PageHeaderProps = {
  title: string;
  description?: string;
  actions?: ReactNode;
};

export default function PageHeader({ title, description, actions }: PageHeaderProps) {
  return (
    <div className="page-header-panel">
      <div>
        <Typography.Title level={3}>{title}</Typography.Title>
        {description && <Typography.Paragraph type="secondary">{description}</Typography.Paragraph>}
      </div>
      {actions && <Space wrap>{actions}</Space>}
    </div>
  );
}
