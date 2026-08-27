import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ServerPagination from './ServerPagination.vue'

describe('ServerPagination', () => {
  it('forwards endpoint-specific pagination settings and emits the server page', async () => {
    const wrapper = mount(ServerPagination, {
      props: { page: 2, pageSize: 25, totalRecords: 100, pageSizeOptions: [25, 50] },
      global: {
        stubs: {
          Paginator: {
            props: ['first', 'rows', 'totalRecords', 'rowsPerPageOptions'],
            emits: ['page'],
            template: '<div data-testid="paginator" :data-first="first" :data-rows="rows" :data-options="rowsPerPageOptions.join(\',\')"><button data-testid="next" @click="$emit(\'page\', { page: 3, rows: 50 })">Next</button></div>',
          },
        },
      },
    })

    const paginator = wrapper.get('[data-testid="paginator"]')
    expect(paginator.attributes('data-first')).toBe('50')
    expect(paginator.attributes('data-rows')).toBe('25')
    expect(paginator.attributes('data-options')).toBe('25,50')
    await wrapper.get('[data-testid="next"]').trigger('click')
    expect(wrapper.emitted('pageChange')).toEqual([[3, 50]])
  })
})
