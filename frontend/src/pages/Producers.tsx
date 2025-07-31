import React, { useState, useEffect } from 'react'
import { Typography, Card, Form, Input, Select, Button, message, Space, Tabs, InputNumber, Upload } from 'antd'
import { SendOutlined, UploadOutlined } from '@ant-design/icons'
// import { useAuthStore } from '../stores/authStore' // Removed for development

const { Title } = Typography
const { TextArea } = Input
const { Option } = Select
const { TabPane } = Tabs

interface Connection {
  id: string
  name: string
  active: boolean
}

const Producers: React.FC = () => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [connections, setConnections] = useState<Connection[]>([])
  const [selectedConnection, setSelectedConnection] = useState<string | undefined>(undefined)
  const [topics, setTopics] = useState<any[]>([])
  const [uploadedFile, setUploadedFile] = useState<File | null>(null)
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
      const response = await fetch(`/api/v1/topics/connections/${connectionId}`, {
        headers: {
          'Content-Type': 'application/json',
        },
      })

      if (response.ok) {
        const data = await response.json()
        setTopics(data)
      }
    } catch (error) {
      console.error('Error fetching topics:', error)
    }
  }

  const handleConnectionChange = (connectionId: string) => {
    setSelectedConnection(connectionId)
    fetchTopics(connectionId)
    form.setFieldsValue({ topic: undefined }) // Reset topic selection
  }

  const handleBulkConnectionChange = (connectionId: string) => {
    fetchTopics(connectionId)
  }

  const handleFileConnectionChange = (connectionId: string) => {
    fetchTopics(connectionId)
  }

  const handleSend = async (values: any) => {
    setLoading(true)
    try {
      const messageData = {
        topic: values.topic,
        key: values.key || null,
        value: values.message,
        partition: values.partition || null,
        headers: values.headers ? JSON.parse(values.headers) : null
      }

      const response = await fetch(`/api/v1/producer/connections/${values.connection}/send`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(messageData),
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const result = await response.json()
      console.log('Message sent successfully:', result)
      message.success('Message sent successfully!')
      form.resetFields()
    } catch (error) {
      console.error('Failed to send message:', error)
      message.error('Failed to send message')
    } finally {
      setLoading(false)
    }
  }

  const handleBulkSend = async (values: any) => {
    setLoading(true)
    try {
      const { bulkConnection, bulkTopic, messageCount, bulkMessages } = values
      const template = bulkMessages

      for (let i = 1; i <= messageCount; i++) {
        const messageContent = template
          .replace(/\{\{index\}\}/g, i.toString())
          .replace(/\{\{timestamp\}\}/g, new Date().toISOString())

        const messageData = {
          topic: bulkTopic,
          key: `bulk-message-${i}`,
          value: messageContent
        }

        const response = await fetch(`/api/v1/producer/connections/${bulkConnection}/send`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(messageData),
        })

        if (!response.ok) {
          throw new Error(`Failed to send message ${i}`)
        }
      }

      message.success(`Successfully sent ${messageCount} messages`)
    } catch (error) {
      console.error('Error sending bulk messages:', error)
      message.error('Failed to send bulk messages')
    } finally {
      setLoading(false)
    }
  }

  const handleFileUpload = async (values: any) => {
    if (!uploadedFile) {
      message.error('Please upload a file first')
      return
    }

    setLoading(true)
    try {
      const { fileConnection, fileTopic } = values
      const fileContent = await uploadedFile.text()
      const lines = fileContent.split('\n').filter(line => line.trim())

      for (let i = 0; i < lines.length; i++) {
        const messageData = {
          topic: fileTopic,
          key: `file-message-${i + 1}`,
          value: lines[i].trim()
        }

        const response = await fetch(`/api/v1/producer/connections/${fileConnection}/send`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(messageData),
        })

        if (!response.ok) {
          throw new Error(`Failed to send message ${i + 1}`)
        }
      }

      message.success(`Successfully sent ${lines.length} messages from file`)
      setUploadedFile(null)
    } catch (error) {
      console.error('Error sending file messages:', error)
      message.error('Failed to send messages from file')
    } finally {
      setLoading(false)
    }
  }

  const handleFileChange = (info: any) => {
    const file = info.file
    if (file) {
      setUploadedFile(file)
      message.success(`${file.name} file uploaded successfully`)
    }
  }

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>Message Producer</Title>
      </div>

      <Card title="Send Message" className="producer-form">
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSend}
        >
          <Form.Item
            name="connection"
            label="Kafka Connection"
            rules={[{ required: true, message: 'Please select a connection' }]}
          >
            <Select
              placeholder="Select Kafka connection"
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
          </Form.Item>

          <Form.Item
            name="topic"
            label="Topic"
            rules={[{ required: true, message: 'Please select a topic' }]}
          >
            <Select
              placeholder="Select topic"
              disabled={!selectedConnection}
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
            name="key"
            label="Message Key (Optional)"
          >
            <Input placeholder="Enter message key" />
          </Form.Item>

          <Form.Item
            name="headers"
            label="Headers (Optional)"
          >
            <TextArea
              placeholder="Enter headers as JSON (e.g., {'userId': '123', 'source': 'web'})"
              rows={3}
            />
          </Form.Item>

          <Form.Item
            name="message"
            label="Message"
            rules={[{ required: true, message: 'Please enter message content' }]}
          >
            <TextArea
              placeholder="Enter your message content (JSON, text, etc.)"
              rows={8}
              className="message-input"
            />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                icon={<SendOutlined />}
              >
                Send Message
              </Button>
              <Button onClick={() => form.resetFields()}>
                Clear
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card title="Bulk Message Production" style={{ marginTop: 24 }}>
        <Tabs defaultActiveKey="1">
          <TabPane tab="Multiple Messages" key="1">
            <Form layout="vertical" onFinish={handleBulkSend}>
              <Form.Item
                name="bulkConnection"
                label="Kafka Connection"
                rules={[{ required: true, message: 'Please select a connection' }]}
              >
                <Select
                  placeholder="Select Kafka connection"
                  onChange={handleBulkConnectionChange}
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
              </Form.Item>

              <Form.Item
                name="bulkTopic"
                label="Topic"
                rules={[{ required: true, message: 'Please select a topic' }]}
              >
                <Select
                  placeholder="Select topic"
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
                name="messageCount"
                label="Number of Messages"
                rules={[{ required: true, message: 'Please enter number of messages' }]}
              >
                <InputNumber min={1} max={1000} defaultValue={10} style={{ width: '100%' }} />
              </Form.Item>

              <Form.Item
                name="bulkMessages"
                label="Message Template (use {{index}} for message number)"
                rules={[{ required: true, message: 'Please enter message template' }]}
              >
                <TextArea
                  placeholder='{"id": {{index}}, "message": "Test message {{index}}", "timestamp": "{{timestamp}}"}'
                  rows={6}
                />
              </Form.Item>

              <Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  icon={<SendOutlined />}
                >
                  Send Bulk Messages
                </Button>
              </Form.Item>
            </Form>
          </TabPane>

          <TabPane tab="File Upload" key="2">
            <Form layout="vertical" onFinish={handleFileUpload}>
              <Form.Item
                name="fileConnection"
                label="Kafka Connection"
                rules={[{ required: true, message: 'Please select a connection' }]}
              >
                <Select
                  placeholder="Select Kafka connection"
                  onChange={handleFileConnectionChange}
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
              </Form.Item>

              <Form.Item
                name="fileTopic"
                label="Topic"
                rules={[{ required: true, message: 'Please select a topic' }]}
              >
                <Select
                  placeholder="Select topic"
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
                name="messageFile"
                label="Upload Messages File"
                rules={[{ required: true, message: 'Please upload a file' }]}
              >
                <Upload.Dragger
                  name="file"
                  multiple={false}
                  accept=".json,.txt,.csv"
                  beforeUpload={() => false}
                  onChange={handleFileChange}
                >
                  <p className="ant-upload-drag-icon">
                    <UploadOutlined />
                  </p>
                  <p className="ant-upload-text">Click or drag file to upload</p>
                  <p className="ant-upload-hint">
                    Support JSON, TXT, or CSV files. Each line should be a separate message.
                  </p>
                </Upload.Dragger>
              </Form.Item>

              <Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  icon={<SendOutlined />}
                  disabled={!uploadedFile}
                >
                  Send Messages from File
                </Button>
              </Form.Item>
            </Form>
          </TabPane>
        </Tabs>
      </Card>
    </div>
  )
}

export default Producers
