import React from 'react'
import clsx from 'clsx'
import './styles.css'

const StatusPill = ({ label, status = 'default', icon = null, subtle = false }) => (
  <span className={clsx('magic-status-pill', `status-${status}`, subtle && 'magic-status-pill-subtle')}>
    {icon && <span className="pill-icon">{icon}</span>}
    {label}
  </span>
)

export default StatusPill
