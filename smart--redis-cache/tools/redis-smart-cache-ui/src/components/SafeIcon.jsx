import React, { useState, useEffect } from 'react';
import * as Icons from '@ant-design/icons';

const SafeIcon = ({ name, fallback = 'QuestionOutlined', ...props }) => {
  const [IconComponent, setIconComponent] = useState(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    try {
      const iconName = name || fallback;
      const Icon = Icons[iconName];
      
      if (Icon) {
        setIconComponent(() => Icon);
        setError(false);
      } else {
        console.warn(`Icon ${iconName} not found, using fallback`);
        const FallbackIcon = Icons[fallback] || Icons.QuestionOutlined;
        setIconComponent(() => FallbackIcon);
        setError(true);
      }
    } catch (err) {
      console.error(`Error loading icon ${name}:`, err);
      const FallbackIcon = Icons[fallback] || Icons.QuestionOutlined;
      setIconComponent(() => FallbackIcon);
      setError(true);
    }
  }, [name, fallback]);

  if (!IconComponent) {
    return <span>...</span>;
  }

  return <IconComponent {...props} />;
};

export default SafeIcon;

// 预定义的图标映射
export const IconMap = {
  // 数据库相关
  database: 'DatabaseOutlined',
  table: 'TableOutlined',
  
  // 操作相关
  search: 'SearchOutlined',
  settings: 'SettingOutlined',
  reload: 'ReloadOutlined',
  add: 'PlusOutlined',
  edit: 'EditOutlined',
  delete: 'DeleteOutlined',
  save: 'SaveOutlined',
  close: 'CloseOutlined',
  
  // 查看相关
  view: 'EyeOutlined',
  
  // 状态相关
  check: 'CheckCircleOutlined',
  error: 'CloseCircleOutlined',
  
  // 趋势相关
  trend: 'ArrowUpOutlined',
  
  // 时间相关
  time: 'ClockCircleOutlined',
  
  // 其他
  disconnect: 'DisconnectOutlined'
};

// 便捷的图标组件
export const DatabaseIcon = (props) => <SafeIcon name={IconMap.database} {...props} />;
export const TableIcon = (props) => <SafeIcon name={IconMap.table} {...props} />;
export const SearchIcon = (props) => <SafeIcon name={IconMap.search} {...props} />;
export const SettingsIcon = (props) => <SafeIcon name={IconMap.settings} {...props} />;
export const ReloadIcon = (props) => <SafeIcon name={IconMap.reload} {...props} />;
export const AddIcon = (props) => <SafeIcon name={IconMap.add} {...props} />;
export const EditIcon = (props) => <SafeIcon name={IconMap.edit} {...props} />;
export const DeleteIcon = (props) => <SafeIcon name={IconMap.delete} {...props} />;
export const SaveIcon = (props) => <SafeIcon name={IconMap.save} {...props} />;
export const CloseIcon = (props) => <SafeIcon name={IconMap.close} {...props} />;
export const ViewIcon = (props) => <SafeIcon name={IconMap.view} {...props} />;
export const CheckIcon = (props) => <SafeIcon name={IconMap.check} {...props} />;
export const ErrorIcon = (props) => <SafeIcon name={IconMap.error} {...props} />;
export const TrendIcon = (props) => <SafeIcon name={IconMap.trend} {...props} />;
export const TimeIcon = (props) => <SafeIcon name={IconMap.time} {...props} />;
export const DisconnectIcon = (props) => <SafeIcon name={IconMap.disconnect} {...props} />;
