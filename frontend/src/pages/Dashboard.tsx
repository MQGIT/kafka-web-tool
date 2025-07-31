import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Row, Col, Card, Statistic, Typography, Space, Tag, Button, Table, message, Modal } from 'antd'
import {
  ApiOutlined,
  UnorderedListOutlined,
  TeamOutlined,
  MessageOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  StopOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import { api } from '../services/api'

const { Title } = Typography

interface DashboardMetrics {
  totalConnections: number
  activeConnections: number
  totalTopics: number
  totalMessages: number
  activeSessions: number
  systemMetrics: {
    status: string
    timestamp: string
  }
}

interface RunningConsumer {
  sessionId: string
  topic: string
  consumerGroup: string
  connectionId: number
  connectionName: string
  status: string
  messagesConsumed: number
  maxMessages: number
  startedAt: string
  isRunning: boolean
  isPaused: boolean
}

interface RecentActivity {
  type: string
  message: string
  color: string
  timestamp: string
  sessionId?: string
  topic?: string
  connectionId?: number
  connectionName?: string
}

interface HealthMetrics {
  status: string
  activeConnections: number
  activeSessions: number
  timestamp: string
}

const Dashboard: React.FC = () => {
  const navigate = useNavigate()
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null)
  const [runningConsumers, setRunningConsumers] = useState<RunningConsumer[]>([])
  const [recentActivity, setRecentActivity] = useState<RecentActivity[]>([])
  const [healthMetrics, setHealthMetrics] = useState<HealthMetrics | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)

  // Fetch dashboard data
  const fetchDashboardData = async () => {
    try {
      setRefreshing(true)

      const [metricsRes, consumersRes, activityRes, healthRes] = await Promise.all([
        api.get('/dashboard/metrics'),
        api.get('/dashboard/running-consumers'),
        api.get('/dashboard/recent-activity'),
        api.get('/dashboard/health')
      ])

      setMetrics(metricsRes.data)
      setRunningConsumers(consumersRes.data)
      setRecentActivity(activityRes.data)
      setHealthMetrics(healthRes.data)
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error)
      message.error('Failed to load dashboard data')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  // Stop a running consumer
  const stopConsumer = async (sessionId: string) => {
    try {
      await api.post(`/dashboard/running-consumers/${sessionId}/stop`)
      message.success('Consumer stopped successfully')
      fetchDashboardData() // Refresh data
    } catch (error) {
      console.error('Failed to stop consumer:', error)
      message.error('Failed to stop consumer')
    }
  }

  // Stop all running consumers
  const stopAllConsumers = async () => {
    Modal.confirm({
      title: 'Stop All Running Consumers',
      content: 'Are you sure you want to stop all running consumers? This action cannot be undone.',
      okText: 'Yes, Stop All',
      okType: 'danger',
      cancelText: 'Cancel',
      onOk: async () => {
        try {
          await api.post('/dashboard/running-consumers/stop-all')
          message.success('All consumers stopped successfully')
          fetchDashboardData() // Refresh data
        } catch (error) {
          console.error('Failed to stop all consumers:', error)
          message.error('Failed to stop all consumers')
        }
      }
    })
  }

  // Format timestamp for display
  const formatTimestamp = (timestamp: string) => {
    const date = new Date(timestamp)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMins = Math.floor(diffMs / 60000)

    if (diffMins < 1) return 'Just now'
    if (diffMins < 60) return `${diffMins} min ago`
    if (diffMins < 1440) return `${Math.floor(diffMins / 60)} hr ago`
    return `${Math.floor(diffMins / 1440)} day ago`
  }

  // Load data on component mount
  useEffect(() => {
    fetchDashboardData()

    // Set up auto-refresh every 30 seconds
    const interval = setInterval(fetchDashboardData, 30000)
    return () => clearInterval(interval)
  }, [])

  // Running consumers table columns
  const consumerColumns = [
    {
      title: 'Topic',
      dataIndex: 'topic',
      key: 'topic',
    },
    {
      title: 'Consumer Group',
      dataIndex: 'consumerGroup',
      key: 'consumerGroup',
    },
    {
      title: 'Connection',
      dataIndex: 'connectionName',
      key: 'connectionName',
    },
    {
      title: 'Messages',
      key: 'messages',
      render: (record: RunningConsumer) => (
        <span>{record.messagesConsumed} / {record.maxMessages || '∞'}</span>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string, record: RunningConsumer) => (
        <Tag color={record.isRunning ? 'green' : record.isPaused ? 'orange' : 'red'}>
          {record.isRunning ? 'RUNNING' : record.isPaused ? 'PAUSED' : status}
        </Tag>
      ),
    },
    {
      title: 'Started',
      dataIndex: 'startedAt',
      key: 'startedAt',
      render: (startedAt: string) => formatTimestamp(startedAt),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (record: RunningConsumer) => (
        <Button
          type="primary"
          danger
          size="small"
          icon={<StopOutlined />}
          onClick={() => stopConsumer(record.sessionId)}
        >
          Stop
        </Button>
      ),
    },
  ]

  if (loading) {
    return (
      <div style={{ padding: '50px', textAlign: 'center' }}>
        <Title level={2}>Loading Dashboard...</Title>
      </div>
    )
  }

  return (
    <div>
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={2}>Dashboard</Title>
        <Button
          type="primary"
          icon={<ReloadOutlined />}
          loading={refreshing}
          onClick={fetchDashboardData}
        >
          Refresh
        </Button>
      </div>

      <Row gutter={[16, 16]} className="dashboard-stats">
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Kafka Connections"
              value={metrics?.totalConnections || 0}
              prefix={<ApiOutlined />}
              suffix={
                <Space>
                  <Tag color="green" icon={<CheckCircleOutlined />}>
                    {metrics?.activeConnections || 0} Active
                  </Tag>
                  <Tag color="orange" icon={<ExclamationCircleOutlined />}>
                    {(metrics?.totalConnections || 0) - (metrics?.activeConnections || 0)} Inactive
                  </Tag>
                </Space>
              }
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Topics"
              value={metrics?.totalTopics || 0}
              prefix={<UnorderedListOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Active Consumers"
              value={metrics?.activeSessions || 0}
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total Messages"
              value={metrics?.totalMessages || 0}
              prefix={<MessageOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Running Consumers Section */}
      {runningConsumers.length > 0 && (
        <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
          <Col span={24}>
            <Card
              title={
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span>Running Consumers ({runningConsumers.length})</span>
                  <Button
                    type="primary"
                    danger
                    size="small"
                    icon={<StopOutlined />}
                    onClick={stopAllConsumers}
                  >
                    Stop All
                  </Button>
                </div>
              }
              size="small"
            >
              <Table
                dataSource={runningConsumers}
                columns={consumerColumns}
                rowKey="sessionId"
                pagination={false}
                size="small"
              />
            </Card>
          </Col>
        </Row>
      )}

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <Card title="Recent Activity" size="small">
            <div style={{ padding: '16px 0' }}>
              <Space direction="vertical" style={{ width: '100%' }}>
                {recentActivity.length > 0 ? (
                  recentActivity.map((activity, index) => (
                    <div key={index}>
                      <Tag color={activity.color}>{activity.type.replace('_', ' ').toUpperCase()}</Tag>
                      <span>{activity.message}</span>
                      <span style={{ float: 'right', color: '#999' }}>
                        {formatTimestamp(activity.timestamp)}
                      </span>
                    </div>
                  ))
                ) : (
                  <div style={{ textAlign: 'center', color: '#999' }}>
                    No recent activity
                  </div>
                )}
              </Space>
            </div>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card title="System Health" size="small">
            <div style={{ padding: '16px 0' }}>
              <Space direction="vertical" style={{ width: '100%' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>Backend API</span>
                  <Tag color={healthMetrics?.status === 'UP' ? 'green' : 'red'}>
                    {healthMetrics?.status === 'UP' ? 'Healthy' : 'Down'}
                  </Tag>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>Active Connections</span>
                  <Tag color="blue">{healthMetrics?.activeConnections || 0}</Tag>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>Active Sessions</span>
                  <Tag color="orange">{healthMetrics?.activeSessions || 0}</Tag>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>Last Updated</span>
                  <span style={{ color: '#999', fontSize: '12px' }}>
                    {healthMetrics?.timestamp ? formatTimestamp(healthMetrics.timestamp) : 'Unknown'}
                  </span>
                </div>
              </Space>
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={24}>
          <Card title="Quick Actions" size="small">
            <Space wrap>
              <Button
                type="primary"
                icon={<ApiOutlined />}
                onClick={() => navigate('/connections')}
              >
                Add Connection
              </Button>
              <Button
                type="default"
                icon={<UnorderedListOutlined />}
                onClick={() => navigate('/topics')}
              >
                Manage Topics
              </Button>
              <Button
                type="default"
                icon={<PlayCircleOutlined />}
                onClick={() => navigate('/consumers')}
              >
                Start Consumer
              </Button>
              <Button
                type="default"
                icon={<MessageOutlined />}
                onClick={() => navigate('/producers')}
              >
                Send Messages
              </Button>
              <Button
                type="default"
                icon={<TeamOutlined />}
                onClick={() => navigate('/messages')}
              >
                Browse Messages
              </Button>
            </Space>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Dashboard
