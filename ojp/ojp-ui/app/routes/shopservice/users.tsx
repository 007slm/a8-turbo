import { createFileRoute } from '@tanstack/react-router'
import UserManagement from '~/components/shopservice/UserManagement'

export const Route = createFileRoute('/shopservice/users')({
    component: UserManagement,
})
