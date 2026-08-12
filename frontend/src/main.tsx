import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider } from 'antd';
import viVN from 'antd/locale/vi_VN';
import App from './app';
import './styles.css';

const queryClient = new QueryClient();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider
      locale={viVN}
      theme={{
        token: {
          colorPrimary: '#0b6258',
          colorInfo: '#168477',
          colorSuccess: '#2f7d55',
          colorWarning: '#b98218',
          colorError: '#b84d3d',
          colorText: '#0b6258',
          colorTextSecondary: 'rgba(11, 98, 88, 0.66)',
          colorBgLayout: '#ffefb3',
          colorBorder: 'rgba(11, 98, 88, 0.18)',
          borderRadius: 14,
          fontFamily: '"Segoe UI Variable", "Segoe UI", Inter, Arial, sans-serif'
        },
        components: {
          Button: {
            controlHeight: 38,
            borderRadius: 12
          },
          Card: {
            borderRadiusLG: 20,
            headerFontSize: 16
          },
          Table: {
            headerBg: '#0b6258',
            headerColor: '#ffefb3',
            rowHoverBg: 'rgba(255, 239, 179, 0.32)'
          },
          Menu: {
            itemSelectedBg: '#ffefb3',
            itemSelectedColor: '#0b6258'
          }
        }
      }}
    >
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </ConfigProvider>
  </React.StrictMode>
);
