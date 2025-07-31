import React, { useState, useEffect } from 'react'
import {
  Typography,
  Button,
  Table,
  Card,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  message,
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons'
// import { useAuthStore } from '../stores/authStore' // Removed for development

const { Title } = Typography
const { Option } = Select

interface Connection {
  id: string
  name: string
  bootstrapServers: string
  securityProtocol: string
  active: boolean
  createdAt: string
  status?: 'connected' | 'disconnected' | 'error' // Optional field for UI state
}

const Connections: React.FC = () => {
  const [connections, setConnections] = useState<Connection[]>([])
  const [loading, setLoading] = useState(true)
  const [isModalVisible, setIsModalVisible] = useState(false)
  const [editingConnection, setEditingConnection] = useState<Connection | null>(null)
  const [testingConnection, setTestingConnection] = useState<string | null>(null)
  const [form] = Form.useForm()
  // const { token } = useAuthStore() // Removed for development

  // Fetch connections from backend
  useEffect(() => {
    fetchConnections()
  }, [])

  const fetchConnections = async () => {
    try {
      setLoading(true)
      const response = await fetch('/api/v1/connections', {
        headers: {
          'Content-Type': 'application/json',
        },
      })

      if (response.ok) {
        const data = await response.json()
        setConnections(data)
      } else {
        message.error('Failed to fetch connections')
      }
    } catch (error) {
      console.error('Error fetching connections:', error)
      message.error('Failed to fetch connections')
    } finally {
      setLoading(false)
    }
  }

  const handleTestConnection = async (connection: Connection) => {
    try {
      setTestingConnection(connection.id)
      const response = await fetch(`/api/v1/connections/${connection.id}/test`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      })

      const result = await response.json()

      if (response.ok && result.success) {
        message.success(`Connection "${connection.name}" tested successfully!`)
        // Update connection status to connected
        setConnections(connections.map(conn =>
          conn.id === connection.id
            ? { ...conn, status: 'connected' }
            : conn
        ))
      } else {
        const errorMsg = result.message || result.error || 'Connection test failed'
        message.error(`Connection test failed: ${errorMsg}`)
        // Update connection status to disconnected
        setConnections(connections.map(conn =>
          conn.id === connection.id
            ? { ...conn, status: 'disconnected' }
            : conn
        ))
      }
    } catch (error) {
      console.error('Error testing connection:', error)
      message.error('Failed to test connection')
      setConnections(connections.map(conn =>
        conn.id === connection.id
          ? { ...conn, status: 'disconnected' }
          : conn
      ))
    } finally {
      setTestingConnection(null)
    }
  }

  const columns = [
    {
      title: 'Name',
      dataIndex: 'name',
      key: 'name',
      render: (text: string) => <strong>{text}</strong>,
    },
    {
      title: 'Bootstrap Servers',
      dataIndex: 'bootstrapServers',
      key: 'bootstrapServers',
      ellipsis: true,
    },
    {
      title: 'Security Protocol',
      dataIndex: 'securityProtocol',
      key: 'securityProtocol',
      render: (protocol: string) => (
        <Tag color={protocol === 'PLAINTEXT' ? 'orange' : 'blue'}>
          {protocol}
        </Tag>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string | undefined, record: Connection) => {
        // Use status if available, otherwise derive from active field
        const actualStatus = status || (record.active ? 'active' : 'inactive')
        const config = {
          connected: { color: 'green', icon: <CheckCircleOutlined />, text: 'Connected' },
          disconnected: { color: 'orange', icon: <ExclamationCircleOutlined />, text: 'Disconnected' },
          error: { color: 'red', icon: <ExclamationCircleOutlined />, text: 'Error' },
          active: { color: 'blue', icon: <CheckCircleOutlined />, text: 'Active' },
          inactive: { color: 'default', icon: <ExclamationCircleOutlined />, text: 'Inactive' },
        }[actualStatus] || { color: 'default', icon: null, text: actualStatus }

        return (
          <Tag color={config.color} icon={config.icon}>
            {config.text}
          </Tag>
        )
      },
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => new Date(date).toLocaleDateString(),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: Connection) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            Edit
          </Button>
          <Button
            type="link"
            loading={testingConnection === record.id}
            onClick={() => handleTestConnection(record)}
          >
            Test
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          >
            Delete
          </Button>
        </Space>
      ),
    },
  ]

  const handleAdd = () => {
    setEditingConnection(null)
    form.resetFields()
    setIsModalVisible(true)
  }

  const handleEdit = (connection: Connection) => {
    setEditingConnection(connection)
    form.setFieldsValue(connection)
    setIsModalVisible(true)
  }

  const handleDelete = (id: string) => {
    Modal.confirm({
      title: 'Delete Connection',
      content: 'Are you sure you want to delete this connection?',
      onOk: async () => {
        try {
          const response = await fetch(`/api/v1/connections/${id}`, {
            method: 'DELETE',
            headers: {
              'Content-Type': 'application/json',
            },
          })

          if (response.ok) {
            message.success('Connection deleted successfully')
            fetchConnections() // Refresh the list
          } else {
            message.error('Failed to delete connection')
          }
        } catch (error) {
          console.error('Error deleting connection:', error)
          message.error('Failed to delete connection')
        }
      },
    })
  }

  const handleSubmit = async (values: any) => {
    try {
      if (editingConnection) {
        // Update existing connection
        const response = await fetch(`/api/v1/connections/${editingConnection.id}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(values),
        })

        if (response.ok) {
          message.success('Connection updated successfully')
          fetchConnections() // Refresh the list
        } else {
          message.error('Failed to update connection')
        }
      } else {
        // Add new connection
        const response = await fetch('/api/v1/connections', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(values),
        })

        if (response.ok) {
          message.success('Connection added successfully')
          fetchConnections() // Refresh the list
        } else {
          message.error('Failed to add connection')
        }
      }
      setIsModalVisible(false)
      form.resetFields()
    } catch (error) {
      console.error('Error saving connection:', error)
      message.error('Failed to save connection')
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <Title level={2}>Kafka Connections</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          Add Connection
        </Button>
      </div>

      <Card>
        <Table
          columns={columns}
          dataSource={connections}
          rowKey="id"
          pagination={{ pageSize: 10 }}
          loading={loading}
        />
      </Card>

      <Modal
        title={editingConnection ? 'Edit Connection' : 'Add Connection'}
        open={isModalVisible}
        onCancel={() => setIsModalVisible(false)}
        onOk={() => form.submit()}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
        >
          <Form.Item
            name="name"
            label="Connection Name"
            rules={[{ required: true, message: 'Please enter connection name' }]}
          >
            <Input placeholder="e.g., Production Cluster" />
          </Form.Item>

          <Form.Item
            name="bootstrapServers"
            label="Bootstrap Servers"
            rules={[{ required: true, message: 'Please enter bootstrap servers' }]}
          >
            <Input placeholder="e.g., kafka1:9092,kafka2:9092" />
          </Form.Item>

          <Form.Item
            name="securityProtocol"
            label="Security Protocol"
            rules={[{ required: true, message: 'Please select security protocol' }]}
          >
            <Select placeholder="Select security protocol">
              <Option value="PLAINTEXT">PLAINTEXT</Option>
              <Option value="SSL">SSL</Option>
              <Option value="SASL_PLAINTEXT">SASL_PLAINTEXT</Option>
              <Option value="SASL_SSL">SASL_SSL</Option>
            </Select>
          </Form.Item>

          <Form.Item>
            <Button type="default" style={{ marginRight: 8 }}>
              Test Connection
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default Connections
