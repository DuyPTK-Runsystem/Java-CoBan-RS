import { mount, shallowMount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ServerPagination from './ServerPagination.vue'
import ScoreGrid from './ScoreGrid.vue'
import type { StudentScoreGrid } from '@/types/scorebook'

const grid: StudentScoreGrid = {
  scorebookId: 1,
  classSubjectId: 2,
  scorebookStatus: 'OPEN',
  columns: [{ columnId: 7, assessmentType: 'KTTT', columnNo: 1, columnName: null }],
  page: 1,
  size: 10,
  totalElements: 25,
  totalPages: 3,
  students: [],
}

describe('ScoreGrid', () => {
  it('distinguishes missing score and zero', () => {
    const wrapper = shallowMount(ScoreGrid, { props: { grid } })
    const view = wrapper.vm as unknown as {
      label: (score: { scoreStatus: 'SCORED'; scoreValue: number | null } | null) => string
    }

    expect(view.label(null)).toBe('Chưa nhập')
    expect(view.label({ scoreStatus: 'SCORED', scoreValue: 0 })).toBe('0.0')
  })

  it('forwards server page changes', () => {
    const wrapper = mount(ScoreGrid, {
      props: { grid },
      global: { stubs: { DataTable: true, Column: true, Paginator: true } },
    })
    const pagination = wrapper.findComponent(ServerPagination).vm as unknown as {
      handlePage: (event: { page: number; rows: number }) => void
    }
    pagination.handlePage({ page: 2, rows: 20 })

    expect(wrapper.emitted('page-change')).toEqual([[2, 20]])
  })
})
