import React, { useState } from 'react'
import { 
  Card, 
  Tabs, 
  Typography, 
  Space,
  message
} from 'antd'
import { 
  ThunderboltOutlined, 
  SettingOutlined
} from '@ant-design/icons'
import CacheRules from './cache/CacheRules'
import QueryCache from './cache/QueryCache'

const { TabPane } = Tabs
const { Text } = Typography

const CacheManagement = () => {
  const [activeTab, setActiveTab] = useState('rules')

  // 处理标签页切换
  const handleTabChange = (key) => {
    setActiveTab(key)
  }

  return (
    <div className="cache-management">
      <Tabs 
        activeKey={activeTab} 
        onChange={handleTabChange}
        type="card"
        size="large"
        defaultActiveKey="rules"
      >
        <TabPane 
          tab={
            <span>
              <SettingOutlined />
              缓存规则
            </span>
          } 
          key="rules"
        >
          <CacheRules />
        </TabPane>
        
        <TabPane 
          tab={
            <span>
              <ThunderboltOutlined />
              慢查询列表
            </span>
          } 
          key="queries"
        >
          <QueryCache />
        </TabPane>
      </Tabs>
    </div>
  )
}

export default CacheManagement
