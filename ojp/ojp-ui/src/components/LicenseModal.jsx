import React, { useState, useEffect } from 'react';
import { Modal, Typography, Button, Input, message, Spin, Descriptions, Badge, theme, Tabs, Space, Tag } from 'antd';
import {
    SafetyCertificateFilled,
    CheckCircleFilled,
    CloseCircleFilled,
    KeyOutlined,
    RocketOutlined,
    SafetyOutlined,
    ClockCircleOutlined,
    UserOutlined
} from '@ant-design/icons';
import { licenseApi } from '../services/api';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;
const { useToken } = theme;

const LicenseModal = ({ open, onClose }) => {
    const { token } = useToken();
    const [loading, setLoading] = useState(false);
    const [submitLoading, setSubmitLoading] = useState(false);
    const [licenseInfo, setLicenseInfo] = useState(null);
    const [activeTab, setActiveTab] = useState('info'); // 'info' or 'update'
    const [formLicenseCode, setFormLicenseCode] = useState('');

    const fetchLicenseInfo = async () => {
        setLoading(true);
        try {
            const data = await licenseApi.getLicense();
            setLicenseInfo(data);
        } catch (error) {
            console.error('Failed to fetch license info:', error);
            message.error('无法获取授权状态');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (open) {
            fetchLicenseInfo();
            setActiveTab('info');
            setFormLicenseCode('');
        }
    }, [open]);

    const handleUpdate = async () => {
        if (!formLicenseCode.trim()) {
            message.warning('请输入授权码');
            return;
        }
        setSubmitLoading(true);
        try {
            const data = await licenseApi.updateLicense(formLicenseCode);
            if (data.valid) {
                message.success({
                    content: '授权激活成功！',
                    icon: <CheckCircleFilled style={{ color: token.colorSuccess }} />,
                });
                setLicenseInfo(data);
                setActiveTab('info');
                setFormLicenseCode('');
            } else {
                message.error(data.message || '授权验证失败，请核对授权码');
            }
        } catch (error) {
            message.error('激活失败: ' + error.message);
        } finally {
            setSubmitLoading(false);
        }
    };

    const isValid = licenseInfo?.valid;

    const renderInfo = () => (
        <div style={{ padding: '0 8px' }}>
            <div style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                marginBottom: 24,
                padding: '16px 0',
                background: isValid ? `linear-gradient(180deg, ${token.colorSuccessBg} 0%, rgba(255,255,255,0) 100%)` : `linear-gradient(180deg, ${token.colorErrorBg} 0%, rgba(255,255,255,0) 100%)`,
                borderRadius: token.borderRadiusLG,
            }}>
                {isValid ? (
                    <Badge count={<CheckCircleFilled style={{ color: token.colorSuccess, fontSize: 16 }} />} offset={[-10, 50]}>
                        <div style={{
                            width: 80, height: 80,
                            background: token.colorBgContainer,
                            borderRadius: '50%',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            boxShadow: '0 4px 12px rgba(0,0,0,0.08)'
                        }}>
                            <SafetyCertificateFilled style={{ fontSize: 48, color: token.colorPrimary }} />
                        </div>
                    </Badge>
                ) : (
                    <Badge count={<CloseCircleFilled style={{ color: token.colorError, fontSize: 16 }} />} offset={[-10, 50]}>
                        <div style={{
                            width: 80, height: 80,
                            background: token.colorBgContainer,
                            borderRadius: '50%',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            boxShadow: '0 4px 12px rgba(0,0,0,0.08)'
                        }}>
                            <SafetyOutlined style={{ fontSize: 48, color: token.colorTextDisabled }} />
                        </div>
                    </Badge>
                )}

                <Title level={4} style={{ marginTop: 16, marginBottom: 4 }}>
                    {isValid ? 'A8 平台 · Turbo 企业版' : '未激活版本'}
                </Title>
                <Text type={isValid ? 'success' : 'secondary'}>
                    {isValid ? 'Commercial License Active' : 'No Valid License Found'}
                </Text>
            </div>

            <Descriptions column={1} bordered size="small">
                <Descriptions.Item label={<Space><UserOutlined />授权客户</Space>}>
                    {licenseInfo?.customer || <Text type="secondary">待授权</Text>}
                </Descriptions.Item>
                <Descriptions.Item label={<Space><ClockCircleOutlined />有效期至</Space>}>
                    {licenseInfo?.expiryDate || <Text type="secondary">-</Text>}
                </Descriptions.Item>
                <Descriptions.Item label={<Space><RocketOutlined />功能模块</Space>}>
                    <Space size={[0, 8]} wrap>
                        <Tag color="blue">Turbo Engine</Tag>
                        <Tag color="cyan">Smart Cache</Tag>
                        <Tag color="geekblue">SQL Translator</Tag>
                    </Space>
                </Descriptions.Item>
            </Descriptions>

            <div style={{ marginTop: 24, textAlign: 'center' }}>
                <Button type={isValid ? 'default' : 'primary'} icon={<KeyOutlined />} onClick={() => setActiveTab('update')}>
                    {isValid ? '更新授权许可' : '立即激活授权'}
                </Button>
            </div>
        </div>
    );

    const renderUpdate = () => (
        <div style={{ padding: '8px' }}>
            <div style={{ marginBottom: 24 }}>
                <Title level={5}>{isValid ? '更新商业授权' : '激活商业授权'}</Title>
                <Paragraph type="secondary" style={{ fontSize: 13 }}>
                    {isValid ? '请输入新的授权码以更新您的商业许可。' : '欢迎使用 A8 平台 · Turbo 模块。请输入您的商业授权码以激活企业版功能。'}
                </Paragraph>
            </div>

            <TextArea
                value={formLicenseCode}
                onChange={(e) => setFormLicenseCode(e.target.value)}
                rows={6}
                placeholder="在此处粘贴 License Key..."
                style={{
                    fontFamily: 'monospace',
                    marginBottom: 24,
                    resize: 'none',
                    background: token.colorFillAlter
                }}
            />

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12 }}>
                <Button onClick={() => setActiveTab('info')}>返回</Button>
                <Button
                    type="primary"
                    onClick={handleUpdate}
                    loading={submitLoading}
                >
                    确认激活
                </Button>
            </div>
        </div>
    );

    return (
        <Modal
            open={open}
            onCancel={onClose}
            footer={null}
            width={480}
            centered
            maskClosable={false}
            title={
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <SafetyCertificateFilled style={{ color: token.colorPrimary }} />
                    <span>商业授权管理</span>
                </div>
            }
            styles={{
                body: { padding: '20px 0 0 0' }
            }}
        >
            <Spin spinning={loading}>
                {activeTab === 'info' ? renderInfo() : renderUpdate()}
            </Spin>
        </Modal>
    );
};

export default LicenseModal;
