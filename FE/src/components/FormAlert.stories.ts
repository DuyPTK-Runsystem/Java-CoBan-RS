import type { Meta, StoryObj } from '@storybook/vue3'

import FormAlert from './FormAlert.vue'

const meta = { title: 'Foundation/FormAlert', component: FormAlert, tags: ['autodocs'] } satisfies Meta<typeof FormAlert>
export default meta
type Story = StoryObj<typeof meta>

export const Error: Story = { args: { tone: 'error', message: 'Unable to save the record.' } }
export const WarningWithValidation: Story = { args: { tone: 'warning', message: 'Review these fields.', validationErrors: [{ field: 'name', messages: ['Name is required.'] }] } }
export const Success: Story = { args: { tone: 'success', message: 'Changes saved.' } }
