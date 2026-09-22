import type { Meta, StoryObj } from '@storybook/vue3'

import PageState from './PageState.vue'

const meta = { title: 'Foundation/PageState', component: PageState, tags: ['autodocs'], args: { state: 'success' } } satisfies Meta<typeof PageState>
export default meta
type Story = StoryObj<typeof meta>

export const Success: Story = { render: (args) => ({ components: { PageState }, setup: () => ({ args }), template: '<PageState v-bind="args"><p>Loaded module content.</p></PageState>' }) }
export const Loading: Story = { args: { state: 'loading' } }
export const Empty: Story = { args: { state: 'empty', emptyHeading: 'No records', emptyMessage: 'There are no records in this view.' } }
export const Error: Story = { args: { state: 'error', errorMessage: 'The records could not be loaded.' } }
export const Forbidden: Story = { args: { forbidden: true, forbiddenMessage: 'Access denied for this module.' } }
