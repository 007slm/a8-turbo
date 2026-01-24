import React from 'react'
import { Card, Skeleton } from 'antd'
import clsx from 'clsx'
import './styles.css'

const MagicCard = ({
  title,
  description,
  icon,
  status,
  extra,
  children,
  loading = false,
  bodyPadding = 16,
  className = '',
  actions,
  size = 'default',
  ...cardProps
}) => {
  const header = (
    <div className="magic-card-heading">
      <div className="magic-card-heading-left">
        {icon && <div className="magic-card-icon">{icon}</div>}
        <div>
          <div className="magic-card-title">{title}</div>
          {description && <div className="magic-card-description">{description}</div>}
        </div>
      </div>
      <div className="magic-card-heading-right">
        {status && <span className={clsx('magic-card-status', `status-${status}`)}>{status}</span>}
        {extra}
      </div>
    </div>
  )

  return (
    <Card
      {...cardProps}
      title={header}
      extra={null}
      bodyStyle={{ padding: bodyPadding }}
      className={clsx('magic-card', className, size === 'small' && 'magic-card-compact')}
    >
      {loading ? <Skeleton active paragraph={{ rows: 3 }} /> : children}
      {actions && <div className="magic-card-actions">{actions}</div>}
    </Card>
  )
}

export default MagicCard
