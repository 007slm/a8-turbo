import { createFileRoute } from '@tanstack/react-router'
import OrderManagement from '~/components/shopservice/OrderManagement'

export const Route = createFileRoute('/shopservice/orders')({
    component: OrderManagement,
})
