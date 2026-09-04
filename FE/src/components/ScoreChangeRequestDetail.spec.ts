import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import ScoreChangeRequestDetail from './ScoreChangeRequestDetail.vue'

const detail = {
  requestId: 9,
  assessmentColumnId: 4,
  studentId: 21,
  studentCode: 'HS-001',
  studentName: 'Nguyễn Minh An',
  studentScoreId: 31,
  beforeStatus: 'SCORED' as const,
  beforeValue: 6.5,
  proposedStatus: 'SCORED' as const,
  proposedValue: 8,
  reason: 'Nhập nhầm điểm sau khi đối chiếu bài kiểm tra.',
  requestedBy: 5,
  requestedAt: '2026-09-03 09:15',
  status: 'PENDING' as const,
  reviewedBy: null,
  reviewedAt: null,
  rejectionReason: null,
  appliedAt: null,
}

function mountDetail(options: { canReview?: boolean; canCancel?: boolean } = {}) {
  return mount(ScoreChangeRequestDetail, {
    props: { detail, canReview: options.canReview ?? true, canCancel: options.canCancel ?? false },
    global: { stubs: { Button, Tag } },
  })
}

describe('ScoreChangeRequestDetail', () => {
  it('renders aligned card items for current score, proposed score, reason, and request time', () => {
    const wrapper = mountDetail()

    expect(wrapper.find('.detail-card-grid').element.tagName).toBe('DL')
    expect(wrapper.findAll('.detail-card')).toHaveLength(4)
    expect(wrapper.find('.detail-card:nth-child(1) .detail-value').text()).toBe('6.5')
    expect(wrapper.find('.detail-card:nth-child(2) .detail-value').text()).toBe('8')
    expect(wrapper.find('.detail-card--wide .detail-value').text()).toContain(detail.reason)
    expect(wrapper.find('.detail-actions').classes()).toContain('detail-actions')
  })

  it('keeps all available actions in one right-aligned action container', () => {
    const wrapper = mountDetail({ canReview: true, canCancel: true })

    expect(wrapper.findAll('.detail-actions .detail-action')).toHaveLength(3)
    expect(wrapper.find('.detail-actions').exists()).toBe(true)
  })

  it('does not render actions for a completed request', () => {
    const wrapper = mount(ScoreChangeRequestDetail, {
      props: { detail: { ...detail, status: 'APPLIED' as const }, canReview: true, canCancel: true },
      global: { stubs: { Button, Tag } },
    })

    expect(wrapper.find('.detail-actions').exists()).toBe(false)
  })
})
