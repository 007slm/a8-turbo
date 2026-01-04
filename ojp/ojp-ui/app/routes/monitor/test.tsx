import { createFileRoute } from '@tanstack/react-router'
import PrometheusTest from '~/components/monitor/PrometheusTest'

export const Route = createFileRoute('/monitor/test')({
    component: PrometheusTest,
})
