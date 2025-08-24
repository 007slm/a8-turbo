import React from 'react';
import { 
  DatabaseOutlined, 
  SearchOutlined, 
  SettingOutlined, 
  TableOutlined,
  ReloadOutlined,
  ArrowUpOutlined,
  ClockCircleOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SaveOutlined,
  CloseOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  DisconnectOutlined
} from '@ant-design/icons';

const IconTest = () => {
  const icons = [
    { name: 'DatabaseOutlined', icon: DatabaseOutlined },
    { name: 'SearchOutlined', icon: SearchOutlined },
    { name: 'SettingOutlined', icon: SettingOutlined },
    { name: 'TableOutlined', icon: TableOutlined },
    { name: 'ReloadOutlined', icon: ReloadOutlined },
    { name: 'ArrowUpOutlined', icon: ArrowUpOutlined },
    { name: 'ClockCircleOutlined', icon: ClockCircleOutlined },
    { name: 'PlusOutlined', icon: PlusOutlined },
    { name: 'EditOutlined', icon: EditOutlined },
    { name: 'DeleteOutlined', icon: DeleteOutlined },
    { name: 'SaveOutlined', icon: SaveOutlined },
    { name: 'CloseOutlined', icon: CloseOutlined },
    { name: 'EyeOutlined', icon: EyeOutlined },
    { name: 'CheckCircleOutlined', icon: CheckCircleOutlined },
    { name: 'CloseCircleOutlined', icon: CloseCircleOutlined },
    { name: 'DisconnectOutlined', icon: DisconnectOutlined }
  ];

  return (
    <div style={{ padding: '20px' }}>
      <h2>图标测试</h2>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: '10px' }}>
        {icons.map(({ name, icon: IconComponent }) => (
          <div key={name} style={{ 
            display: 'flex', 
            flexDirection: 'column', 
            alignItems: 'center', 
            padding: '10px',
            border: '1px solid #d9d9d9',
            borderRadius: '6px'
          }}>
            <IconComponent style={{ fontSize: '24px', marginBottom: '5px' }} />
            <span style={{ fontSize: '12px', textAlign: 'center' }}>{name}</span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default IconTest;
