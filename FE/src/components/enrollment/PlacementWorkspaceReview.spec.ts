import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import { placementReviewFixture } from '@/fixtures/placementFixture'
import PlacementWorkspaceReview from './PlacementWorkspaceReview.vue'

describe('PlacementWorkspaceReview', () => {
  it('renders class metadata and a result needing manual handling without blocking confirm', () => {
    const wrapper = mount(PlacementWorkspaceReview, {
      props: {
        session: placementReviewFixture,
        studentNamesById: {
          1: { studentCode: 'HS001', studentName: 'Nguyễn Văn A' },
          2: { studentCode: 'HS002', studentName: 'Trần Thị B' },
        },
      },
    })
    expect(wrapper.text()).toContain('Nâng cao')
    expect(wrapper.text()).toContain('17 nam')
    expect(wrapper.text()).toContain('Cần xếp thủ công')
    expect(wrapper.text()).toContain('HS001')
    expect(wrapper.text()).toContain('Nguyễn Văn A')
    expect(wrapper.text()).not.toContain('V3 · PLACEMENT')
    expect(wrapper.text()).not.toContain('snapshot')
    expect(wrapper.text()).not.toContain('capacity')
    expect(wrapper.text()).not.toContain('AUTO_ASSIGNED')
    expect(wrapper.text()).not.toContain('MANUAL_REQUIRED')

    // Find the exact confirm button by text
    const confirmBtn = wrapper.findAll('button').find((b) => b.text().includes('Xác nhận phần tự động'))
    expect(confirmBtn).toBeDefined()
    expect(confirmBtn?.attributes('disabled')).toBeUndefined()
  })

  it('disables confirmation when session status is SIMULATED with blocking issues', () => {
    const session = {
      ...placementReviewFixture,
      status: 'SIMULATED' as const,
      results: [
        {
          id: 1,
          studentId: 1,
          targetClassId: 81,
          resultStatus: 'AUTO_ASSIGNED' as const,
          score: 8.5,
          issueCode: 'CAPACITY_EXCEEDED',
          issueSeverity: 'BLOCKING' as const,
          explanation: 'Lớp 8A1 đã vượt sĩ số cho phép khi xếp lớp tự động.',
        },
      ],
    }
    const wrapper = mount(PlacementWorkspaceReview, { props: { session } })
    const confirmBtn = wrapper.findAll('button').find((b) => b.text().includes('Xác nhận phần tự động'))
    expect(confirmBtn?.attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('Phiên có lỗi chặn xếp lớp (vượt sĩ số cho phép)')
    expect(wrapper.text()).not.toContain('capacity')
  })

  it('preserves a forbidden review state', () => {
    const wrapper = mount(PlacementWorkspaceReview, { props: { session: null, reviewState: 'forbidden' } })
    expect(wrapper.text()).toContain('không có quyền')
  })

  it('offers approved class options while editing a draft session', () => {
    const selectStub = {
      template: '<select><option v-for="option in options" :key="option.value">{{ option.label }}</option></select>',
      props: ['options', 'modelValue'],
    }
    const wrapper = mount(PlacementWorkspaceReview, {
      props: { session: { ...placementReviewFixture, status: 'DRAFT' }, editable: true },
      global: { stubs: { Select: selectStub } },
    })
    expect(wrapper.find('select').exists()).toBe(true)
  })

  it('emits page-change event when paginator triggers page navigation', async () => {
    const paginatorStub = {
      template: '<div data-testid="paginator"><button class="next-page" @click="$emit(\'page\', { page: 1, rows: 20 })">Next</button></div>',
      emits: ['page'],
    }
    const wrapper = mount(PlacementWorkspaceReview, {
      props: {
        session: placementReviewFixture,
        resultsMeta: { page: 0, pageSize: 20, totalPages: 2, totalItems: 35 },
      },
      global: { stubs: { Paginator: paginatorStub } },
    })

    const nextBtn = wrapper.find('.next-page')
    await nextBtn.trigger('click')
    expect(wrapper.emitted('page-change')).toEqual([[1, 20]])
  })
})
