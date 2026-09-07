import type { Meta, StoryObj } from '@storybook/vue3'

import ConfirmAction from './ConfirmAction.vue'

const meta = { title: 'Foundation/ConfirmAction', component: ConfirmAction, tags: ['autodocs'], args: { label: 'Delete record', message: 'Delete this record?' } } satisfies Meta<typeof ConfirmAction>
export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}
export const Disabled: Story = { args: { disabled: true } }
