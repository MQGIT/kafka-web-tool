import React from 'react'
import { Layout, Dropdown, Avatar, Space, Typography } from 'antd'
import { UserOutlined, LogoutOutlined, SettingOutlined, ApiOutlined } from '@ant-design/icons'
import { useAuthStore } from '../../stores/authStore'

const { Header } = Layout
const { Text } = Typography

const AppHeader: React.FC = () => {
  const { user, logout } = useAuthStore()

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: 'Profile',
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: 'Settings',
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Logout',
      onClick: logout,
    },
  ]

  return (
    <Header className="app-header">
      <div className="app-logo">
        <ApiOutlined className="app-logo-icon" />
        <span>Kafka Web Tool v2</span>
      </div>
      
      <div className="app-user-menu">
        <Space>
          <Text style={{ color: 'white' }}>
            Welcome, {user?.username || 'User'}
          </Text>
          <Dropdown
            menu={{ items: userMenuItems }}
            placement="bottomRight"
            trigger={['click']}
          >
            <Avatar
              style={{ cursor: 'pointer' }}
              icon={<UserOutlined />}
            />
          </Dropdown>
        </Space>
      </div>
    </Header>
  )
}

export default AppHeader
