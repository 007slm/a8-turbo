import { createFileRoute } from '@tanstack/react-router'
import ReviewManagement from '~/components/shopservice/ReviewManagement'

export const Route = createFileRoute('/shopservice/reviews')({
    component: ReviewManagement,
})
