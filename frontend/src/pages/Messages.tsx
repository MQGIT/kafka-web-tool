import React, { useState, useEffect } from 'react'
import { Typography, Card, Form, Select, Button, Table, Tag, Space, message } from 'antd'
import { SearchOutlined, EyeOutlined } from '@ant-design/icons'

const { Title } = Typography
const { Option } = Select

interface Connection {
  id: string
  name: string
  active: boolean
}

const Messages: React.FC = () => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [messages, setMessages] = useState<any[]>([])
  const [connections, setConnections] = useState<Connection[]>([])
  const [topics, setTopics] = useState<any[]>([])
  const [selectedConnection, setSelectedConnection] = useState<string>('')
  const [selectedTopicStats, setSelectedTopicStats] = useState<any>(null)

  // Fetch connections on component mount
  useEffect(() => {
    fetchConnections()
  }, [])

  const fetchConnections = async () => {
    try {
      const response = await fetch('/api/v1/connections', {
        headers: {
          'Content-Type': 'application/json',
        },
      })

      if (response.ok) {
        const data = await response.json()
        setConnections(data.filter((conn: Connection) => conn.active))
      } else {
        message.error('Failed to fetch connections')
      }
    } catch (error) {
      console.error('Error fetching connections:', error)
      message.error('Failed to fetch connections')
    }
  }

  const fetchTopics = async (connectionId: string) => {
    try {
      setLoading(true)
      const response = await fetch(`/api/v1/topics/connections/${connectionId}`, {
        headers: {
          'Content-Type': 'application/json',
        },
      })

      if (response.ok) {
        const data = await response.json()
        setTopics(data)
      } else {
        message.error('Failed to fetch topics')
      }
    } catch (error) {
      console.error('Error fetching topics:', error)
      message.error('Failed to fetch topics')
    } finally {
      setLoading(false)
    }
  }

  const handleConnectionChange = (connectionId: string) => {
    setSelectedConnection(connectionId)
    setTopics([])
    setSelectedTopicStats(null)
    form.setFieldsValue({ topic: undefined })
    fetchTopics(connectionId)
  }

  const fetchTopicStats = async (connectionId: string, topicName: string) => {
    try {
      const response = await fetch(`/api/v1/topics/connections/${connectionId}/topics/${topicName}/stats`)
      if (response.ok) {
        const stats = await response.json()
        setSelectedTopicStats(stats)
      }
    } catch (error) {
      console.error('Error fetching topic stats:', error)
    }
  }

  const handleTopicChange = (topicName: string) => {
    if (selectedConnection && topicName) {
      fetchTopicStats(selectedConnection, topicName)
    }
  }

  const columns = [
    {
      title: 'Offset',
      dataIndex: 'offset',
      key: 'offset',
      width: 100,
    },
    {
      title: 'Partition',
      dataIndex: 'partition',
      key: 'partition',
      width: 100,
      render: (partition: number) => (
        <Tag color="blue">{partition}</Tag>
      ),
    },
    {
      title: 'Key',
      dataIndex: 'key',
      key: 'key',
      width: 120,
      ellipsis: true,
    },
    {
      title: 'Timestamp',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 180,
      render: (timestamp: string) => new Date(timestamp).toLocaleString(),
    },
    {
      title: 'Value',
      dataIndex: 'value',
      key: 'value',
      ellipsis: true,
      render: (value: string) => (
        <span className="message-value">
          {value && value.length > 100 ? `${value.substring(0, 100)}...` : value || '<null>'}
        </span>
      ),
    },
    {
      title: 'Size',
      dataIndex: 'serializedValueSize',
      key: 'serializedValueSize',
      width: 80,
      render: (size: number) => (
        <Tag color="green">{size ? `${size}B` : 'N/A'}</Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 100,
      render: (_: any, record: any) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          size="small"
          onClick={() => {
            // Copy message to clipboard
            const messageText = `Topic: ${record.topic}\nPartition: ${record.partition}\nOffset: ${record.offset}\nKey: ${record.key || 'null'}\nValue: ${record.value || 'null'}\nTimestamp: ${record.timestamp}`
            navigator.clipboard.writeText(messageText)
            message.success('Message copied to clipboard!')
          }}
        >
          Copy
        </Button>
      ),
    },
  ]

  const handleSearch = async (values: any) => {
    setLoading(true)
    try {
      console.log('Searching messages:', values)

      const params = new URLSearchParams()
      if (values.partition !== undefined) params.append('partition', values.partition)
      if (values.offset) params.append('startOffset', values.offset)
      else params.append('startOffset', 'earliest') // Default to earliest
      if (values.limit) params.append('limit', values.limit)
      else params.append('limit', '100') // Default limit

      const queryString = params.toString() ? `?${params.toString()}` : ''
      const response = await fetch(`/api/v1/topics/connections/${values.connection}/topics/${values.topic}/messages${queryString}`, {
        headers: {
          'Content-Type': 'application/json',
        },
      })

      if (response.ok) {
        const data = await response.json()
        setMessages(data)
        message.success(`Found ${data.length} messages`)
      } else {
        message.error('Failed to search messages')
      }
    } catch (error) {
      console.error('Error searching messages:', error)
      message.error('Failed to search messages')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>Message Browser</Title>
      </div>

      <Card title="Search Messages" style={{ marginBottom: 24 }}>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSearch}
        >
          <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
            <Form.Item
              name="connection"
              label="Connection"
              rules={[{ required: true, message: 'Please select a connection' }]}
              style={{ minWidth: 200 }}
            >
              <Select
                placeholder="Select connection"
                onChange={handleConnectionChange}
                value={selectedConnection}
              >
                {connections.map(connection => (
                  <Option key={connection.id} value={connection.id}>
                    {connection.name}
                  </Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item
              name="topic"
              label="Topic"
              rules={[{ required: true, message: 'Please select a topic' }]}
              style={{ minWidth: 200 }}
            >
              <Select
                placeholder="Select topic"
                disabled={!selectedConnection || topics.length === 0}
                onChange={handleTopicChange}
                showSearch
                filterOption={(input, option) => {
                  const label = option?.label?.toString() || ''
                  const children = option?.children?.toString() || ''
                  return label.toLowerCase().includes(input.toLowerCase()) ||
                         children.toLowerCase().includes(input.toLowerCase())
                }}
              >
                {topics.map(topic => (
                  <Option key={topic.name} value={topic.name}>
                    {topic.name} ({topic.partitionCount} partitions)
                  </Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item
              name="partition"
              label="Partition"
              style={{ minWidth: 120 }}
            >
              <Select placeholder="All partitions" allowClear>
                {selectedConnection && topics.length > 0 && form.getFieldValue('topic') &&
                  Array.from({ length: topics.find(t => t.name === form.getFieldValue('topic'))?.partitionCount || 0 }, (_, i) => (
                    <Option key={i} value={i}>Partition {i}</Option>
                  ))
                }
              </Select>
            </Form.Item>

            <Form.Item
              name="offset"
              label="Start Offset"
              style={{ minWidth: 150 }}
            >
              <Select placeholder="Earliest" allowClear defaultValue="earliest">
                <Option value="earliest">Earliest</Option>
                <Option value="latest">Latest</Option>
                <Option value="custom">Custom</Option>
              </Select>
            </Form.Item>

            <Form.Item
              name="limit"
              label="Max Messages"
              style={{ minWidth: 120 }}
            >
              <Select placeholder="100" defaultValue="100">
                <Option value="10">10</Option>
                <Option value="50">50</Option>
                <Option value="100">100</Option>
                <Option value="500">500</Option>
                <Option value="1000">1000</Option>
              </Select>
            </Form.Item>

            <Form.Item
              label=" "
              style={{ minWidth: 200 }}
            >
              <Space>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  icon={<SearchOutlined />}
                  disabled={!selectedConnection || topics.length === 0}
                >
                  Search Messages
                </Button>
                <Button onClick={() => form.resetFields()}>
                  Clear
                </Button>
              </Space>
            </Form.Item>
          </div>
        </Form>
      </Card>

      {selectedTopicStats && (
        <Card title="Topic Statistics" style={{ marginTop: 16 }}>
          <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap' }}>
            <div>
              <strong>Total Messages:</strong> {selectedTopicStats.totalMessages?.toLocaleString() || 'N/A'}
            </div>
            <div>
              <strong>Partitions:</strong> {selectedTopicStats.partitionCount || 'N/A'}
            </div>
            <div>
              <strong>Topic:</strong> {selectedTopicStats.topic || 'N/A'}
            </div>
            <div>
              <strong>Last Updated:</strong> {selectedTopicStats.timestamp ? new Date(selectedTopicStats.timestamp).toLocaleString() : 'N/A'}
            </div>
          </div>
        </Card>
      )}

      {messages.length > 0 && (
        <Card title={`Messages (${messages.length} found)`}>
          <Table
            columns={columns}
            dataSource={messages}
            rowKey={(record) => `${record.partition}-${record.offset}`}
            pagination={{ pageSize: 20 }}
            scroll={{ x: 1000 }}
          />
        </Card>
      )}
    </div>
  )
}

export default Messages
