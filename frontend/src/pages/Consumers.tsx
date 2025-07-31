import React, { useState, useEffect } from 'react'
import { Typography, Card, Select, message, Form, Input, Button, Space, Table, Tag, InputNumber, Tooltip } from 'antd'
import { PlayCircleOutlined, PauseCircleOutlined, StopOutlined, CopyOutlined, DownloadOutlined } from '@ant-design/icons'
// import { useAuthStore } from '../stores/authStore' // Removed for development

const { Title } = Typography
const { Option } = Select

interface Connection {
  id: string
  name: string
  active: boolean
}

const Consumers: React.FC = () => {
  const [connections, setConnections] = useState<Connection[]>([])
  const [topics, setTopics] = useState<any[]>([])
  const [selectedConnection, setSelectedConnection] = useState<string | undefined>(undefined)
  const [selectedTopic, setSelectedTopic] = useState<string>('')
  const [loading, setLoading] = useState(false)
  const [consumerMessages, setConsumerMessages] = useState<any[]>([])
  const [consumerStatus, setConsumerStatus] = useState<'stopped' | 'running' | 'paused'>('stopped')
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null)
  const [pollingInterval, setPollingInterval] = useState<NodeJS.Timeout | null>(null)
  // const { token } = useAuthStore() // Removed for development

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
      }
    } catch (error) {
      console.error('Error fetching connections:', error)
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
    setSelectedTopic('')
    setTopics([])
    fetchTopics(connectionId)
  }

  const handleStartConsumer = async (values: any) => {
    setLoading(true)
    try {
      const consumerData = {
        connectionId: selectedConnection,
        topic: selectedTopic,
        consumerGroup: values.consumerGroup,
        partition: values.partition,
        startOffset: values.startOffset === 'specific' ? values.specificOffset : null,
        maxMessages: values.maxMessages,
        autoCommit: true,
        pollTimeoutMs: 1000
      }

      // Create consumer session
      const response = await fetch('/api/v1/consumer/sessions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(consumerData),
      })

      if (response.ok) {
        const session = await response.json()
        setCurrentSessionId(session.sessionId)

        // Start the consumer
        const startResponse = await fetch(`/api/v1/consumer/sessions/${session.sessionId}/start`, {
          method: 'POST',
        })

        if (startResponse.ok) {
          setConsumerStatus('running')
          message.success('Consumer started successfully')
          // Start polling for messages
          startMessagePolling(session.sessionId)
        } else {
          message.error('Failed to start consumer')
        }
      } else {
        message.error('Failed to create consumer session')
      }
    } catch (error) {
      console.error('Error starting consumer:', error)
      message.error('Failed to start consumer')
    } finally {
      setLoading(false)
    }
  }

  const handlePauseConsumer = async () => {
    if (!currentSessionId) return

    try {
      const response = await fetch(`/api/v1/consumer/sessions/${currentSessionId}/pause`, {
        method: 'POST',
      })

      if (response.ok) {
        setConsumerStatus('paused')
        message.success('Consumer paused')
      } else {
        message.error('Failed to pause consumer')
      }
    } catch (error) {
      console.error('Error pausing consumer:', error)
      message.error('Failed to pause consumer')
    }
  }

  const handleStopConsumer = async () => {
    if (!currentSessionId) return

    try {
      // Stop polling
      stopMessagePolling()

      const response = await fetch(`/api/v1/consumer/sessions/${currentSessionId}/stop`, {
        method: 'POST',
      })

      if (response.ok) {
        setConsumerStatus('stopped')
        setCurrentSessionId(null)
        message.success('Consumer stopped')
      } else {
        message.error('Failed to stop consumer')
      }
    } catch (error) {
      console.error('Error stopping consumer:', error)
      message.error('Failed to stop consumer')
    }
  }

  const startMessagePolling = (sessionId: string) => {
    // Clear any existing polling
    stopMessagePolling()

    const interval = setInterval(async () => {
      try {
        // Check session status first
        const sessionResponse = await fetch(`/api/v1/consumer/sessions/${sessionId}`)
        if (sessionResponse.ok) {
          const session = await sessionResponse.json()

          // If session is stopped or finished, get final messages and stop polling
          if (session.status === 'STOPPED' || session.finished) {
            const messagesResponse = await fetch(`/api/v1/consumer/sessions/${sessionId}/messages`)
            if (messagesResponse.ok) {
              const messages = await messagesResponse.json()
              setConsumerMessages(messages)
            }
            setConsumerStatus('stopped')
            stopMessagePolling()
            return
          }

          // Otherwise get current messages
          const messagesResponse = await fetch(`/api/v1/consumer/sessions/${sessionId}/messages`)
          if (messagesResponse.ok) {
            const messages = await messagesResponse.json()
            setConsumerMessages(messages)
          }
        }
      } catch (error) {
        console.error('Error polling messages:', error)
      }
    }, 1000) // Poll every 1 second for more responsive updates

    setPollingInterval(interval)
  }

  const stopMessagePolling = () => {
    if (pollingInterval) {
      clearInterval(pollingInterval)
      setPollingInterval(null)
    }
  }

  // Cleanup polling on component unmount
  useEffect(() => {
    return () => {
      stopMessagePolling()
    }
  }, [])

  const handleCopyMessages = () => {
    const messagesText = consumerMessages.map(msg =>
      `Topic: ${msg.topic}\nPartition: ${msg.partition}\nOffset: ${msg.offset}\nKey: ${msg.key || 'null'}\nValue: ${msg.value}\nTimestamp: ${msg.timestamp}\n---`
    ).join('\n')

    navigator.clipboard.writeText(messagesText)
    message.success(`Copied ${consumerMessages.length} messages to clipboard`)
  }

  const handleDownloadMessages = () => {
    const dataStr = JSON.stringify(consumerMessages, null, 2)
    const dataBlob = new Blob([dataStr], { type: 'application/json' })
    const url = URL.createObjectURL(dataBlob)
    const link = document.createElement('a')
    link.href = url
    link.download = `kafka-messages-${selectedTopic}-${new Date().toISOString().split('T')[0]}.json`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    message.success(`Downloaded ${consumerMessages.length} messages`)
  }

  const messageColumns = [
    {
      title: 'Partition',
      dataIndex: 'partition',
      key: 'partition',
      width: 80,
      render: (partition: number) => <Tag color="blue">{partition}</Tag>,
    },
    {
      title: 'Offset',
      dataIndex: 'offset',
      key: 'offset',
      width: 100,
      render: (offset: number) => offset?.toLocaleString(),
    },
    {
      title: 'Key',
      dataIndex: 'key',
      key: 'key',
      width: 120,
      ellipsis: true,
      render: (key: string) => key || '<null>',
    },
    {
      title: 'Value',
      dataIndex: 'value',
      key: 'value',
      ellipsis: true,
      render: (value: string) => (
        <span style={{ fontFamily: 'monospace', fontSize: '12px' }}>
          {value && value.length > 100 ? `${value.substring(0, 100)}...` : value || '<null>'}
        </span>
      ),
    },
    {
      title: 'Timestamp',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 160,
      render: (timestamp: string) => timestamp ? new Date(timestamp).toLocaleString() : 'N/A',
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 80,
      render: (_: any, record: any) => (
        <Tooltip title="Copy message">
          <Button
            type="link"
            icon={<CopyOutlined />}
            size="small"
            onClick={() => {
              const messageText = `Topic: ${record.topic}\nPartition: ${record.partition}\nOffset: ${record.offset}\nKey: ${record.key || 'null'}\nValue: ${record.value}\nTimestamp: ${record.timestamp}`
              navigator.clipboard.writeText(messageText)
              message.success('Message copied to clipboard!')
            }}
          />
        </Tooltip>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>Message Consumer</Title>
      </div>

      <Card>
        <div style={{ marginBottom: 16, display: 'flex', gap: 16 }}>
          <Select
            placeholder="Select a Kafka connection"
            style={{ width: 300 }}
            value={selectedConnection}
            onChange={handleConnectionChange}
            showSearch
            filterOption={(input, option) => {
              const label = option?.label?.toString() || ''
              const children = option?.children?.toString() || ''
              return label.toLowerCase().includes(input.toLowerCase()) ||
                     children.toLowerCase().includes(input.toLowerCase())
            }}
          >
            {connections.map(connection => (
              <Option key={connection.id} value={connection.id}>
                {connection.name}
              </Option>
            ))}
          </Select>

          <Select
            placeholder="Select a topic"
            style={{ width: 300 }}
            value={selectedTopic}
            onChange={setSelectedTopic}
            disabled={!selectedConnection || topics.length === 0}
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
        </div>

        {selectedTopic && (
          <div style={{ marginTop: 24 }}>
            <Title level={4}>Consumer Configuration for: {selectedTopic}</Title>
            <Form
              layout="vertical"
              style={{ maxWidth: 600 }}
              onFinish={handleStartConsumer}
              initialValues={{
                consumerGroup: 'kafka-web-app-consumer',
                startOffset: 'latest',
                maxMessages: 100
              }}
            >
              <Form.Item
                label="Consumer Group ID"
                name="consumerGroup"
                rules={[{ required: true, message: 'Please enter consumer group ID' }]}
              >
                <Input placeholder="kafka-web-app-consumer" />
              </Form.Item>

              <Form.Item label="Partition (optional)" name="partition">
                <Select placeholder="Select partition (leave empty for all partitions)" allowClear>
                  {Array.from({ length: topics.find(t => t.name === selectedTopic)?.partitionCount || 0 }, (_, i) => (
                    <Option key={i} value={i}>Partition {i}</Option>
                  ))}
                </Select>
              </Form.Item>

              <Form.Item label="Start Offset" name="startOffset">
                <Select defaultValue="latest" placeholder="Select start offset">
                  <Option value="earliest">Earliest</Option>
                  <Option value="latest">Latest</Option>
                  <Option value="specific">Specific Offset</Option>
                </Select>
              </Form.Item>

              <Form.Item label="Max Messages" name="maxMessages">
                <InputNumber
                  min={1}
                  max={10000}
                  placeholder="Maximum messages to consume"
                  style={{ width: '100%' }}
                />
              </Form.Item>

              <Form.Item>
                <Space>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={loading}
                    disabled={consumerStatus === 'running'}
                    icon={<PlayCircleOutlined />}
                  >
                    Start Consumer
                  </Button>
                  <Button
                    onClick={handlePauseConsumer}
                    disabled={consumerStatus !== 'running'}
                    icon={<PauseCircleOutlined />}
                  >
                    Pause
                  </Button>
                  <Button
                    onClick={handleStopConsumer}
                    disabled={consumerStatus === 'stopped'}
                    icon={<StopOutlined />}
                    danger
                  >
                    Stop
                  </Button>
                </Space>
              </Form.Item>

              <Form.Item>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span>Status:</span>
                  <Tag color={consumerStatus === 'running' ? 'green' : consumerStatus === 'paused' ? 'orange' : 'default'}>
                    {consumerStatus.toUpperCase()}
                  </Tag>
                  {currentSessionId && (
                    <span style={{ fontSize: '12px', color: '#666' }}>
                      Session: {currentSessionId.substring(0, 8)}...
                    </span>
                  )}
                </div>
              </Form.Item>
            </Form>

            {consumerMessages.length > 0 && (
              <div style={{ marginTop: 24 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                  <Title level={4}>Consumed Messages ({consumerMessages.length})</Title>
                  <Space>
                    <Tooltip title="Copy all messages to clipboard">
                      <Button
                        icon={<CopyOutlined />}
                        onClick={handleCopyMessages}
                        size="small"
                      >
                        Copy All
                      </Button>
                    </Tooltip>
                    <Tooltip title="Download messages as JSON file">
                      <Button
                        icon={<DownloadOutlined />}
                        onClick={handleDownloadMessages}
                        size="small"
                      >
                        Download
                      </Button>
                    </Tooltip>
                    <Button
                      onClick={() => setConsumerMessages([])}
                      size="small"
                      danger
                    >
                      Clear
                    </Button>
                  </Space>
                </div>

                <Table
                  dataSource={consumerMessages}
                  columns={messageColumns}
                  rowKey={(record) => `${record.partition}-${record.offset}`}
                  pagination={{ pageSize: 20 }}
                  scroll={{ x: 1000 }}
                  size="small"
                />
              </div>
            )}
          </div>
        )}
      </Card>
    </div>
  )
}

export default Consumers
