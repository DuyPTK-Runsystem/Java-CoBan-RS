import type { Meta, StoryObj } from '@storybook/vue3'

import CapacityWarningBanner from './CapacityWarningBanner.vue'

const meta = { title: 'AcademicCatalog/CapacityWarningBanner', component: CapacityWarningBanner, tags: ['autodocs'], parameters: { layout: 'padded' } } satisfies Meta<typeof CapacityWarningBanner>
export default meta
type Story = StoryObj<typeof meta>

export const ContractUnavailable: Story = { args: { available: false } }
export const NonBlockingWarning: Story = { args: { available: true, warningCount: 2, message: '2 lớp lệch sĩ số quá 20% so với trung bình khối.' } }
export const NoWarning: Story = { args: { available: true, warningCount: 0 } }
