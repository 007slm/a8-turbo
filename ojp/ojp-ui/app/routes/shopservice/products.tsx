import { createFileRoute } from '@tanstack/react-router'
import ProductManagement from '~/components/shopservice/ProductManagement'

export const Route = createFileRoute('/shopservice/products')({
    component: ProductManagement,
})
