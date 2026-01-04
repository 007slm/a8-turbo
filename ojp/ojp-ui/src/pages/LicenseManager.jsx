import React, { useState, useEffect } from 'react';
import { Card, Form, Input, Button, Badge, Typography, Space, message, Result, Spin, theme, Modal } from 'antd';
import { SafetyCertificateOutlined, CheckCircleFilled, CloseCircleFilled, KeyOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import { licenseApi } from '../services/api';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;
const { useToken } = theme;

const LicenseManager = () => {
    const [licenseInfo, setLicenseInfo] = useState(null);
    const [loading, setLoading] = useState(true);
    const [submitLoading, setSubmitLoading] = useState(false);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [form] = Form.useForm();
    const { token } = useToken();

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
        fetchLicenseInfo();
    }, []);

    const handleUpdate = async (values) => {
        setSubmitLoading(true);
        try {
            const data = await licenseApi.updateLicense(values.licenseCode);
            if (data.valid) {
                message.success('授权已激活');
                setLicenseInfo(data);
                setIsModalOpen(false);
                form.resetFields();
            } else {
                message.warning(data.message || '授权验证未通过，请核对后重试');
                setLicenseInfo(data);
            }
        } catch (error) {
            message.error('更新失败: ' + error.message);
        } finally {
            setSubmitLoading(false);
        }
    };

    const StatusCard = ({ info }) => {
        const isValid = info?.valid;
        const statusColor = isValid ? token.colorSuccess : token.colorError;
        const bgColor = isValid ? token.colorSuccessBg : token.colorErrorBg;
        const borderColor = isValid ? token.colorSuccessBorder : token.colorErrorBorder;

        return (
            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
            >
                <div style={{
                    background: bgColor,
                    border: `1px solid ${borderColor}`,
                    borderRadius: token.borderRadiusLG,
                    padding: '32px 24px',
                    textAlign: 'center',
                    marginBottom: '24px',
                    position: 'relative',
                    overflow: 'hidden',
                    boxShadow: '0 6px 30px rgba(0,0,0,0.05)'
                }}>
                    <div style={{ position: 'relative', zIndex: 1 }}>
                        <div style={{ marginBottom: '16px' }}>
                            {isValid ? (
                                <CheckCircleFilled style={{ fontSize: '56px', color: statusColor }} />
                            ) : (
                                <CloseCircleFilled style={{ fontSize: '56px', color: statusColor }} />
                            )}
                        </div>
                        <Title level={3} style={{ margin: '0 0 8px 0', fontSize: '20px' }}>
                            {isValid ? '商业授权已激活' : '当前版本未检测到有效的商业授权'}
                        </Title>
                        <Paragraph type="secondary" style={{ fontSize: '14px', margin: '0 0 24px 0', maxWidth: '500px', marginLeft: 'auto', marginRight: 'auto' }}>
                            {isValid ? '您正在使用 OJP 企业版，享受完整的企业级功能与技术支持服务。' : '请激活正式授权以确保系统正常运行，解除所有功能限制。'}
                        </Paragraph>

                        <Button
                            type={isValid ? "default" : "primary"}
                            size="large"
                            icon={<KeyOutlined />}
                            onClick={() => setIsModalOpen(true)}
                            style={{
                                padding: '0 32px',
                                height: '40px',
                                fontSize: '14px',
                                borderRadius: '20px'
                            }}
                        >
                            {isValid ? '更新授权许可' : '立即激活授权'}
                        </Button>

                        {isValid && info && (
                            <div style={{ marginTop: '24px', borderTop: `1px dashed ${borderColor}`, paddingTop: '20px' }}>
                                <div style={{ display: 'flex', justifyContent: 'center', gap: '48px' }}>
                                    <div>
                                        <Text type="secondary" style={{ fontSize: '12px', display: 'block', marginBottom: '4px' }}>授权客户</Text>
                                        <div style={{ fontSize: '15px', fontWeight: 600, color: token.colorTextHeading }}>{info.customer}</div>
                                    </div>
                                    <div>
                                        <Text type="secondary" style={{ fontSize: '12px', display: 'block', marginBottom: '4px' }}>有效期至</Text>
                                        <div style={{ fontSize: '15px', fontWeight: 600, color: token.colorTextHeading }}>{info.expiryDate}</div>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </motion.div>
        );
    };

    return (
        <div style={{
            height: 'calc(100vh - 200px)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            alignItems: 'center',
            padding: '0 24px'
        }}>
            <div style={{ width: '100%', maxWidth: '700px' }}>
                <motion.div
                    initial={{ opacity: 0, y: -20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5 }}
                >
                    <div style={{ marginBottom: '24px', textAlign: 'center' }}>
                        <Title level={2} style={{ marginBottom: '8px', fontSize: '24px' }}>
                            <SafetyCertificateOutlined style={{ marginRight: '10px', color: token.colorPrimary }} />
                            商业授权管理
                        </Title>
                        <Text type="secondary" style={{ fontSize: '13px' }}>Enterprise License Management</Text>
                    </div>
                </motion.div>

                {loading ? (
                    <div style={{ textAlign: 'center', padding: '40px' }}>
                        <Spin size="large" />
                    </div>
                ) : (
                    <StatusCard info={licenseInfo} />
                )}

                <div style={{ marginTop: '24px', textAlign: 'center' }}>
                    <Text type="secondary" style={{ fontSize: '12px' }}>
                        © A8 Enterprise Edition
                    </Text>
                </div>
            </div>

            <Modal
                title={
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '16px' }}>
                        <KeyOutlined style={{ color: token.colorPrimary }} />
                        <span>更新商业授权</span>
                    </div>
                }
                open={isModalOpen}
                onCancel={() => setIsModalOpen(false)}
                footer={null}
                destroyOnClose
            >
                <div style={{ paddingTop: '16px' }}>
                    <Paragraph type="secondary" style={{ marginBottom: '24px' }}>
                        请在下方输入框中粘贴您的商业授权码（License Key）。
                    </Paragraph>
                    <Form form={form} layout="vertical" onFinish={handleUpdate}>
                        <Form.Item
                            name="licenseCode"
                            rules={[{ required: true, message: '请完整输入商业授权码' }]}
                        >
                            <TextArea
                                rows={6}
                                placeholder="Base64EncodedString.Signature..."
                                style={{
                                    resize: 'none',
                                    padding: '12px',
                                    borderRadius: token.borderRadius,
                                    fontFamily: 'monospace',
                                    fontSize: '14px'
                                }}
                            />
                        </Form.Item>
                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
                            <Button onClick={() => setIsModalOpen(false)}>
                                取消
                            </Button>
                            <Button
                                type="primary"
                                htmlType="submit"
                                loading={submitLoading}
                            >
                                确认激活
                            </Button>
                        </div>
                    </Form>
                </div>
            </Modal>
        </div>
    );
};

export default LicenseManager;
