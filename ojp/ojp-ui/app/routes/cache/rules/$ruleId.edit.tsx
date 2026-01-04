import { createFileRoute } from '@tanstack/react-router'
import CacheRuleEditor from '~/components/cache/CacheRuleEditor'

export const Route = createFileRoute('/cache/rules/$ruleId/edit')({
    component: CacheRuleEditor,
})
