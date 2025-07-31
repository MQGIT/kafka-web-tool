import { FC } from 'react'
import { Routes, Route } from 'react-router-dom'
import { Layout } from 'antd'
import AppHeader from './components/layout/AppHeader'
import AppSidebar from './components/layout/AppSidebar'
import Dashboard from './pages/Dashboard'
import Connections from './pages/Connections'
import Topics from './pages/Topics'
import Consumers from './pages/Consumers'
import Producers from './pages/Producers'
import Messages from './pages/Messages'
import Login from './pages/Login'
import { useAuthStore } from './stores/authStore'
import './App.css'

const { Content } = Layout

const App: FC = () => {
  const { isAuthenticated } = useAuthStore()

  if (!isAuthenticated) {
    return <Login />
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <AppHeader />
      <Layout>
        <AppSidebar />
        <Layout style={{ padding: '24px' }}>
          <Content
            style={{
              padding: 24,
              margin: 0,
              minHeight: 280,
              background: '#fff',
              borderRadius: 8,
            }}
          >
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/connections" element={<Connections />} />
              <Route path="/topics" element={<Topics />} />
              <Route path="/consumers" element={<Consumers />} />
              <Route path="/producers" element={<Producers />} />
              <Route path="/messages" element={<Messages />} />
            </Routes>
          </Content>
        </Layout>
      </Layout>
    </Layout>
  )
}

export default App
