import React, { useState, useEffect } from 'react'
import { Typography, Card, Form, Select, Button, Table, Tag, Space, message, Modal, Popconfirm, Input, Tooltip } from 'antd'
import { SearchOutlined, EyeOutlined, EditOutlined, DeleteOutlined, CopyOutlined } from '@ant-design/icons'

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
  const [editModalVisible, setEditModalVisible] = useState(false)
  const [editingMessage, setEditingMessage] = useState<any>(null)
  const [editForm] = Form.useForm()
  const [editLoading, setEditLoading] = useState(false)
  const [deleteLoading, setDeleteLoading] = useState<string | null>(null)

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

  const handleEditMessage = (record: any) => {
    setEditingMessage(record)
    editForm.setFieldsValue({
      key: record.key,
      value: record.value,
      headers: record.headers ? JSON.stringify(record.headers, null, 2) : ''
    })
    setEditModalVisible(true)
  }

  const handleSaveEdit = async (values: any) => {
    setEditLoading(true)
    try {
      // Validate headers JSON if provided
      let headers = undefined
      if (values.headers && values.headers.trim()) {
        try {
          headers = JSON.parse(values.headers)
        } catch (e) {
          message.error('Invalid JSON format in headers')
          setEditLoading(false)
          return
        }
      }

      const updatedMessage = {
        key: values.key?.trim(),
        value: values.value,
        headers
      }

      // Validate required fields
      if (!updatedMessage.key) {
        message.error('Message key is required')
        setEditLoading(false)
        return
      }

      const response = await fetch(`/api/v1/topics/connections/${selectedConnection}/topics/${form.getFieldValue('topic')}/messages`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(updatedMessage),
      })

      if (response.ok) {
        const result = await response.json()
        message.success('Message updated successfully')
        setEditModalVisible(false)
        setEditingMessage(null)
        editForm.resetFields()
      } else {
        const errorText = await response.text()
        console.error('Server error:', errorText)
        message.error(`Failed to update message: ${response.status} ${response.statusText}`)
      }
    } catch (error) {
      console.error('Error updating message:', error)
      message.error('Network error: Failed to update message')
    } finally {
      setEditLoading(false)
    }
  }

  const handleDeleteMessage = async (record: any) => {
    const messageKey = `${record.partition}-${record.offset}`
    setDeleteLoading(messageKey)

    try {
      if (!record.key) {
        message.error('Cannot delete message without a key')
        setDeleteLoading(null)
        return
      }

      const params = new URLSearchParams()
      params.append('key', record.key)
      if (record.partition !== undefined) {
        params.append('partition', record.partition.toString())
      }

      const response = await fetch(`/api/v1/topics/connections/${selectedConnection}/topics/${form.getFieldValue('topic')}/messages?${params.toString()}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
        },
      })

      if (response.ok) {
        const result = await response.json()
        message.success('Message deleted successfully (tombstone sent)')
      } else {
        const errorText = await response.text()
        console.error('Server error:', errorText)
        message.error(`Failed to delete message: ${response.status} ${response.statusText}`)
      }
    } catch (error) {
      console.error('Error deleting message:', error)
      message.error('Network error: Failed to delete message')
    } finally {
      setDeleteLoading(null)
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
      width: 140,
      render: (_: any, record: any) => (
        <Space size="small">
          <Tooltip title="Copy message">
            <Button
              type="link"
              icon={<CopyOutlined />}
              size="small"
              onClick={() => {
                const messageText = `Topic: ${record.topic}\nPartition: ${record.partition}\nOffset: ${record.offset}\nKey: ${record.key || 'null'}\nValue: ${record.value || 'null'}\nTimestamp: ${record.timestamp}`
                navigator.clipboard.writeText(messageText)
                message.success('Message copied to clipboard!')
              }}
            />
          </Tooltip>
          {record.key && (
            <>
              <Tooltip title="Edit message">
                <Button
                  type="link"
                  icon={<EditOutlined />}
                  size="small"
                  onClick={() => handleEditMessage(record)}
                />
              </Tooltip>
              <Tooltip title="Delete message (send tombstone)">
                <Popconfirm
                  title="Delete message"
                  description="This will send a tombstone message. Are you sure?"
                  onConfirm={() => handleDeleteMessage(record)}
                  okText="Yes"
                  cancelText="No"
                >
                  <Button
                    type="link"
                    icon={<DeleteOutlined />}
                    size="small"
                    danger
                    loading={deleteLoading === `${record.partition}-${record.offset}`}
                  />
                </Popconfirm>
              </Tooltip>
            </>
          )}
        </Space>
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

      {/* Edit Message Modal */}
      <Modal
        title="Edit Message"
        open={editModalVisible}
        onCancel={() => {
          setEditModalVisible(false)
          setEditingMessage(null)
          editForm.resetFields()
        }}
        footer={null}
        width={600}
      >
        <Form
          form={editForm}
          layout="vertical"
          onFinish={handleSaveEdit}
        >
          <Form.Item
            label="Message Key"
            name="key"
            rules={[{ required: true, message: 'Please enter message key' }]}
          >
            <Input placeholder="Message key" />
          </Form.Item>

          <Form.Item
            label="Message Value"
            name="value"
            rules={[{ required: true, message: 'Please enter message value' }]}
          >
            <Input.TextArea
              rows={6}
              placeholder="Message value"
            />
          </Form.Item>

          <Form.Item
            label="Headers (JSON format)"
            name="headers"
          >
            <Input.TextArea
              rows={4}
              placeholder='{"header1": "value1", "header2": "value2"}'
            />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={editLoading}>
                Save Changes
              </Button>
              <Button
                onClick={() => {
                  setEditModalVisible(false)
                  setEditingMessage(null)
                  editForm.resetFields()
                }}
                disabled={editLoading}
              >
                Cancel
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default Messages
