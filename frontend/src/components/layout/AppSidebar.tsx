import React from 'react'
import { Layout, Menu } from 'antd'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  DashboardOutlined,
  ApiOutlined,
  UnorderedListOutlined,
  TeamOutlined,
  SendOutlined,
  MessageOutlined,
} from '@ant-design/icons'

const { Sider } = Layout

const AppSidebar: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()

  const menuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: 'Dashboard',
    },
    {
      key: '/connections',
      icon: <ApiOutlined />,
      label: 'Connections',
    },
    {
      key: '/topics',
      icon: <UnorderedListOutlined />,
      label: 'Topics',
    },
    {
      key: '/consumers',
      icon: <TeamOutlined />,
      label: 'Consumers',
    },
    {
      key: '/producers',
      icon: <SendOutlined />,
      label: 'Producers',
    },
    {
      key: '/messages',
      icon: <MessageOutlined />,
      label: 'Messages',
    },
  ]

  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key)
  }

  const selectedKey = location.pathname === '/' ? '/dashboard' : location.pathname

  return (
    <Sider
      width={200}
      className="app-sidebar"
      breakpoint="lg"
      collapsedWidth="0"
    >
      <Menu
        mode="inline"
        theme="dark"
        selectedKeys={[selectedKey]}
        items={menuItems}
        onClick={handleMenuClick}
        style={{ height: '100%', borderRight: 0 }}
      />
    </Sider>
  )
}

export default AppSidebar
