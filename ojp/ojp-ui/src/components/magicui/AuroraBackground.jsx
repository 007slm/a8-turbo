import React from 'react'
import { motion } from 'framer-motion'
import clsx from 'clsx'
import './styles.css'

const auroraLayers = [
  {
    className: 'aurora-one',
    animate: {
      x: [0, 25, -25, 0],
      y: [0, -30, 10, 0],
      rotate: [0, 4, -6, 0],
    },
  },
  {
    className: 'aurora-two',
    animate: {
      x: [0, -35, 20, 0],
      y: [0, 25, -25, 0],
      rotate: [0, -8, 6, 0],
    },
  },
  {
    className: 'aurora-three',
    animate: {
      x: [0, 30, -10, 0],
      y: [0, -15, 30, 0],
      rotate: [0, 3, -4, 0],
    },
  },
]

const AuroraBackground = ({ children, className = '', blur = false }) => {
  return (
    <div className={clsx('magic-aurora', className, { 'magic-aurora-blur': blur })}>
      <div className="magic-aurora-layers">
        {auroraLayers.map((layer) => (
          <motion.div
            key={layer.className}
            className={clsx('magic-aurora-layer', layer.className)}
            animate={layer.animate}
            transition={{
              repeat: Infinity,
              repeatType: 'mirror',
              duration: 18,
              ease: 'easeInOut',
            }}
          />
        ))}
      </div>
      <div className="magic-aurora-content">{children}</div>
    </div>
  )
}

export default AuroraBackground
