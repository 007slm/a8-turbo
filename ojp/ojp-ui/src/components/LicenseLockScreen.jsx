import React, { useState } from 'react';
import { Card, Form, Input, Button, Typography, message, theme } from 'antd';
import { KeyOutlined, SafetyCertificateFilled, ArrowRightOutlined } from '@ant-design/icons';
import { licenseApi } from '../services/api';
import { useQueryClient } from 'react-query';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;
const { useToken } = theme;

const LicenseLockScreen = () => {
    const { token } = useToken();
    const [loading, setLoading] = useState(false);
    const queryClient = useQueryClient();

    const handleActivate = async (values) => {
        setLoading(true);
        try {
            const data = await licenseApi.updateLicense(values.licenseCode);
            if (data.valid) {
                message.success('授权激活成功，欢迎使用！');
                // Invalidate query to trigger App re-render
                await queryClient.invalidateQueries('licenseInfo');
            } else {
                message.error(data.message || '授权验证失败，请核对后重试');
            }
        } catch (error) {
            message.error('激活失败: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{
            height: '100vh',
            width: '100vw',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: '#f0f2f5',
            backgroundImage: 'url("https://gw.alipayobjects.com/zos/rmsportal/TVYTbAXWheQpRcWDaDMu.svg")',
            backgroundRepeat: 'no-repeat',
            backgroundPosition: 'center 110%',
            backgroundSize: '100%',
        }}>
            <Card
                bordered={false}
                style={{
                    width: 480,
                    boxShadow: '0 8px 24px rgba(0,0,0,0.05)',
                    borderRadius: 16,
                }}
                styles={{
                    body: { padding: '32px 24px' }
                }}
            >
                <div style={{ textAlign: 'center', marginBottom: 32 }}>
                    <div style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        width: 64, height: 64,
                        background: '#e6f4ff',
                        borderRadius: '50%',
                        marginBottom: 16
                    }}>
                        <SafetyCertificateFilled style={{ fontSize: 32, color: token.colorPrimary }} />
                    </div>
                    <Title level={3} style={{ marginBottom: 8 }}>A8 平台 · Turbo 商业授权</Title>
                    <Text type="secondary">Enterprise License Activation</Text>
                </div>

                <Paragraph type="secondary" style={{ marginBottom: 24, textAlign: 'center' }}>
                    当前系统未检测到有效的商业授权许可。为了保障系统的正常运行和功能完整性，请在下方激活您的企业版授权。
                </Paragraph>

                <Form layout="vertical" onFinish={handleActivate}>
                    <Form.Item
                        name="licenseCode"
                        rules={[{ required: true, message: '请输入商业授权码' }]}
                    >
                        <TextArea
                            placeholder="请粘贴您的 License Key..."
                            rows={5}
                            style={{
                                resize: 'none',
                                borderRadius: 8,
                                fontFamily: 'monospace',
                                fontSize: 14
                            }}
                        />
                    </Form.Item>

                    <Form.Item style={{ marginBottom: 0 }}>
                        <Button
                            type="primary"
                            htmlType="submit"
                            size="large"
                            block
                            loading={loading}
                            icon={<KeyOutlined />}
                            style={{ height: 48, borderRadius: 8, fontSize: 16 }}
                        >
                            立即激活
                        </Button>
                    </Form.Item>
                </Form>

                <div style={{ marginTop: 32, textAlign: 'center' }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                        如果遇到问题，请联系系统管理员或技术支持团队
                    </Text>
                </div>
            </Card>

            <div style={{ position: 'absolute', bottom: 24, textAlign: 'center', width: '100%' }}>
                <Text type="secondary" style={{ fontSize: 12 }}>© 2026 A8 Platform Enterprise Edition</Text>
            </div>
        </div>
    );
};

export default LicenseLockScreen;
