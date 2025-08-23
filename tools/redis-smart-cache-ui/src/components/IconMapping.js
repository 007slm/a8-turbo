// 图标映射文件 - 确保所有图标都是有效的
export const Icons = {
  // 数据库相关
  DatabaseOutlined: 'DatabaseOutlined',
  TableOutlined: 'TableOutlined',
  
  // 操作相关
  SearchOutlined: 'SearchOutlined',
  SettingOutlined: 'SettingOutlined',
  ReloadOutlined: 'ReloadOutlined',
  PlusOutlined: 'PlusOutlined',
  EditOutlined: 'EditOutlined',
  DeleteOutlined: 'DeleteOutlined',
  SaveOutlined: 'SaveOutlined',
  CloseOutlined: 'CloseOutlined',
  
  // 查看相关
  EyeOutlined: 'EyeOutlined',
  
  // 状态相关
  CheckCircleOutlined: 'CheckCircleOutlined',
  CloseCircleOutlined: 'CloseCircleOutlined',
  
  // 趋势相关 - 使用确定存在的图标
  ArrowUpOutlined: 'ArrowUpOutlined',
  
  // 时间相关
  ClockCircleOutlined: 'ClockCircleOutlined',
  
  // 其他
  DisconnectOutlined: 'DisconnectOutlined'
};

// 验证图标是否存在
export const validateIcon = (iconName) => {
  return Icons[iconName] ? iconName : 'QuestionOutlined';
};

// 常用图标组合
export const CommonIcons = {
  // 统计卡片图标
  stats: {
    total: 'DatabaseOutlined',
    cached: 'CheckCircleOutlined',
    hitRate: 'ArrowUpOutlined',
    time: 'ClockCircleOutlined'
  },
  
  // 操作图标
  actions: {
    add: 'PlusOutlined',
    edit: 'EditOutlined',
    delete: 'DeleteOutlined',
    save: 'SaveOutlined',
    close: 'CloseOutlined',
    view: 'EyeOutlined',
    refresh: 'ReloadOutlined',
    settings: 'SettingOutlined'
  }
};
