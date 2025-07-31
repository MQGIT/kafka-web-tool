import React, { useState, useEffect } from 'react'
import {
  Card,
  Table,
  Button,
  Input,
  Space,
  Typography,
  Tag,
  Modal,
  Form,
  InputNumber,
  Select,
  message,
  Descriptions,
  Tooltip,
  Popconfirm
} from 'antd'
import {
  SearchOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  PlusOutlined,
  ReloadOutlined
} from '@ant-design/icons'

const { Title } = Typography
const { Option } = Select

interface Connection {
  id: number
  name: string
  bootstrapServers: string
  active: boolean
}

interface Topic {
  name: string
  partitionCount: number
  replicationFactor: number
  configs?: Record<string, string>
}

interface TopicStats {
  topic: string
  partitionCount: number
  totalMessages: number
  partitionStats: Record<string, any>
  timestamp: string
}

const Topics: React.FC = () => {
  const [topics, setTopics] = useState<Topic[]>([])
  const [filteredTopics, setFilteredTopics] = useState<Topic[]>([])
  const [connections, setConnections] = useState<Connection[]>([])
  const [selectedConnection, setSelectedConnection] = useState<string>('')
  const [loading, setLoading] = useState(false)
  const [searchText, setSearchText] = useState('')

  // Modal states
  const [viewModalVisible, setViewModalVisible] = useState(false)
  const [editModalVisible, setEditModalVisible] = useState(false)
  const [createModalVisible, setCreateModalVisible] = useState(false)
  const [selectedTopic, setSelectedTopic] = useState<Topic | null>(null)
  const [topicStats, setTopicStats] = useState<TopicStats | null>(null)

  const [form] = Form.useForm()
  const [editForm] = Form.useForm()

  // Fetch connections on component mount
  useEffect(() => {
    fetchConnections()
  }, [])

  // Filter topics when search text changes
  useEffect(() => {
    if (searchText) {
      const filtered = topics.filter(topic =>
        topic.name.toLowerCase().includes(searchText.toLowerCase())
      )
      setFilteredTopics(filtered)
    } else {
      setFilteredTopics(topics)
    }
  }, [searchText, topics])

  const fetchConnections = async () => {
    try {
      const response = await fetch('/api/v1/connections')
      if (response.ok) {
        const data = await response.json()
        setConnections(data.filter((conn: Connection) => conn.active))
      }
    } catch (error) {
      console.error('Error fetching connections:', error)
    }
  }

  const fetchTopics = async (connectionId: string) => {
    setLoading(true)
    try {
      const response = await fetch(`/api/v1/topics/connections/${connectionId}`)
      if (response.ok) {
        const data = await response.json()
        setTopics(data)
        setFilteredTopics(data)
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

  const fetchTopicStats = async (connectionId: string, topicName: string) => {
    try {
      const response = await fetch(`/api/v1/topics/connections/${connectionId}/topics/${topicName}/stats`)
      if (response.ok) {
        const stats = await response.json()
        setTopicStats(stats)
      }
    } catch (error) {
      console.error('Error fetching topic stats:', error)
    }
  }

  const handleConnectionChange = (connectionId: string) => {
    setSelectedConnection(connectionId)
    setTopics([])
    setFilteredTopics([])
    fetchTopics(connectionId)
  }

  const handleViewTopic = async (topic: Topic) => {
    setSelectedTopic(topic)
    setTopicStats(null)
    setViewModalVisible(true)

    if (selectedConnection) {
      await fetchTopicStats(selectedConnection, topic.name)
    }
  }

  const handleEditTopic = (topic: Topic) => {
    setSelectedTopic(topic)
    editForm.setFieldsValue({
      name: topic.name,
      partitionCount: topic.partitionCount,
      replicationFactor: topic.replicationFactor
    })
    setEditModalVisible(true)
  }

  const handleCreateTopic = async (values: any) => {
    try {
      const response = await fetch(`/api/v1/topics/connections/${selectedConnection}/topics`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: values.name,
          partitions: values.partitions,
          replicationFactor: values.replicationFactor,
          configs: values.configs || {}
        }),
      })

      if (response.ok) {
        message.success('Topic created successfully')
        setCreateModalVisible(false)
        form.resetFields()
        fetchTopics(selectedConnection)
      } else {
        const error = await response.text()
        message.error(`Failed to create topic: ${error}`)
      }
    } catch (error) {
      console.error('Error creating topic:', error)
      message.error('Failed to create topic')
    }
  }

  const handleDeleteTopic = async (topicName: string) => {
    try {
      const response = await fetch(`/api/v1/topics/connections/${selectedConnection}/topics/${topicName}`, {
        method: 'DELETE',
      })

      if (response.ok) {
        message.success('Topic deleted successfully')
        fetchTopics(selectedConnection)
      } else {
        const error = await response.text()
        message.error(`Failed to delete topic: ${error}`)
      }
    } catch (error) {
      console.error('Error deleting topic:', error)
      message.error('Failed to delete topic')
    }
  }

  const columns = [
    {
      title: 'Topic Name',
      dataIndex: 'name',
      key: 'name',
      sorter: (a: Topic, b: Topic) => a.name.localeCompare(b.name),
      render: (name: string) => (
        <span style={{ fontWeight: 'bold', color: '#1890ff' }}>{name}</span>
      ),
    },
    {
      title: 'Partitions',
      dataIndex: 'partitionCount',
      key: 'partitionCount',
      width: 120,
      sorter: (a: Topic, b: Topic) => a.partitionCount - b.partitionCount,
      render: (count: number) => (
        <Tag color="blue">{count}</Tag>
      ),
    },
    {
      title: 'Replication Factor',
      dataIndex: 'replicationFactor',
      key: 'replicationFactor',
      width: 150,
      sorter: (a: Topic, b: Topic) => a.replicationFactor - b.replicationFactor,
      render: (factor: number) => (
        <Tag color="green">{factor}</Tag>
      ),
    },

    {
      title: 'Actions',
      key: 'actions',
      width: 200,
      render: (_: any, record: Topic) => (
        <Space>
          <Tooltip title="View Details">
            <Button
              type="link"
              icon={<EyeOutlined />}
              size="small"
              onClick={() => handleViewTopic(record)}
            />
          </Tooltip>
          <Tooltip title="Edit Topic">
            <Button
              type="link"
              icon={<EditOutlined />}
              size="small"
              onClick={() => handleEditTopic(record)}
            />
          </Tooltip>
          <Tooltip title="Delete Topic">
            <Popconfirm
              title="Are you sure you want to delete this topic?"
              description="This action cannot be undone and will permanently delete all messages."
              onConfirm={() => handleDeleteTopic(record.name)}
              okText="Yes, Delete"
              cancelText="Cancel"
              okType="danger"
            >
              <Button
                type="link"
                icon={<DeleteOutlined />}
                size="small"
                danger
              />
            </Popconfirm>
          </Tooltip>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>Topics Management</Title>
      </div>

      <Card>
        <div style={{ marginBottom: 16, display: 'flex', gap: 16, alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ flex: 1, minWidth: 200 }}>
            <Select
              placeholder="Select Kafka connection"
              value={selectedConnection}
              onChange={handleConnectionChange}
              style={{ width: '100%' }}
              showSearch
              filterOption={(input, option) => {
                const label = option?.label?.toString() || ''
                const children = option?.children?.toString() || ''
                return label.toLowerCase().includes(input.toLowerCase()) ||
                       children.toLowerCase().includes(input.toLowerCase())
              }}
            >
              {connections.map(connection => (
                <Option key={connection.id} value={connection.id.toString()}>
                  {connection.name} ({connection.bootstrapServers})
                </Option>
              ))}
            </Select>
          </div>

          <div style={{ flex: 1, minWidth: 200 }}>
            <Input
              placeholder="Search topics..."
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              allowClear
            />
          </div>

          <Space>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setCreateModalVisible(true)}
              disabled={!selectedConnection}
            >
              Create Topic
            </Button>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => selectedConnection && fetchTopics(selectedConnection)}
              disabled={!selectedConnection}
            >
              Refresh
            </Button>
          </Space>
        </div>

        <Table
          columns={columns}
          dataSource={filteredTopics}
          rowKey="name"
          loading={loading}
          pagination={{
            pageSize: 20,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} topics`,
          }}
          scroll={{ x: 800 }}
        />
      </Card>

      {/* View Topic Modal */}
      <Modal
        title="Topic Details"
        open={viewModalVisible}
        onCancel={() => setViewModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setViewModalVisible(false)}>
            Close
          </Button>
        ]}
        width={800}
      >
        {selectedTopic && (
          <div>
            <Descriptions bordered column={2}>
              <Descriptions.Item label="Topic Name">{selectedTopic.name}</Descriptions.Item>
              <Descriptions.Item label="Partitions">{selectedTopic.partitionCount}</Descriptions.Item>
              <Descriptions.Item label="Replication Factor">{selectedTopic.replicationFactor}</Descriptions.Item>
              <Descriptions.Item label="Total Messages">
                {topicStats ? topicStats.totalMessages.toLocaleString() : 'Loading...'}
              </Descriptions.Item>
            </Descriptions>

            {topicStats && (
              <div style={{ marginTop: 16 }}>
                <Title level={4}>Partition Details</Title>
                <Table
                  dataSource={Object.entries(topicStats.partitionStats).map(([partition, stats]: [string, any]) => ({
                    partition: parseInt(partition),
                    earliestOffset: stats.earliestOffset,
                    latestOffset: stats.latestOffset,
                    messageCount: stats.messageCount
                  }))}
                  columns={[
                    { title: 'Partition', dataIndex: 'partition', key: 'partition' },
                    { title: 'Earliest Offset', dataIndex: 'earliestOffset', key: 'earliestOffset' },
                    { title: 'Latest Offset', dataIndex: 'latestOffset', key: 'latestOffset' },
                    { title: 'Message Count', dataIndex: 'messageCount', key: 'messageCount', render: (count: number) => count.toLocaleString() }
                  ]}
                  pagination={false}
                  size="small"
                />
              </div>
            )}
          </div>
        )}
      </Modal>

      {/* Create Topic Modal */}
      <Modal
        title="Create New Topic"
        open={createModalVisible}
        onCancel={() => {
          setCreateModalVisible(false)
          form.resetFields()
        }}
        onOk={() => form.submit()}
        confirmLoading={loading}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleCreateTopic}
        >
          <Form.Item
            name="name"
            label="Topic Name"
            rules={[{ required: true, message: 'Please enter topic name' }]}
          >
            <Input placeholder="Enter topic name" />
          </Form.Item>

          <Form.Item
            name="partitions"
            label="Number of Partitions"
            rules={[{ required: true, message: 'Please enter number of partitions' }]}
            initialValue={3}
          >
            <InputNumber min={1} max={1000} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="replicationFactor"
            label="Replication Factor"
            rules={[{ required: true, message: 'Please enter replication factor' }]}
            initialValue={1}
          >
            <InputNumber min={1} max={10} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit Topic Modal */}
      <Modal
        title="Edit Topic"
        open={editModalVisible}
        onCancel={() => setEditModalVisible(false)}
        footer={[
          <Button key="cancel" onClick={() => setEditModalVisible(false)}>
            Cancel
          </Button>,
          <Button key="save" type="primary" disabled>
            Save Changes (Coming Soon)
          </Button>
        ]}
      >
        <Form
          form={editForm}
          layout="vertical"
        >
          <Form.Item name="name" label="Topic Name">
            <Input disabled />
          </Form.Item>

          <Form.Item name="partitionCount" label="Partitions">
            <InputNumber disabled style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="replicationFactor" label="Replication Factor">
            <InputNumber disabled style={{ width: '100%' }} />
          </Form.Item>

          <div style={{ padding: 16, backgroundColor: '#f0f0f0', borderRadius: 4 }}>
            <strong>Note:</strong> Topic editing functionality will be available when authentication is implemented.
          </div>
        </Form>
      </Modal>
    </div>
  )
}

export default Topics
