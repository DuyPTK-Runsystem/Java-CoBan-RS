import type { Meta, StoryObj } from '@storybook/vue3'

import ServerPagination from './ServerPagination.vue'

const meta = { title: 'Foundation/ServerPagination', component: ServerPagination, tags: ['autodocs'], args: { page: 1, pageSize: 25, totalRecords: 125, pageSizeOptions: [25, 50] } } satisfies Meta<typeof ServerPagination>
export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}
