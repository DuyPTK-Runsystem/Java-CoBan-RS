import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import type { PlacementResult } from '@/types/placement'
import PlacementResultDetailDialog from './PlacementResultDetailDialog.vue'

const autoResult: PlacementResult = {
  id: 1,
  studentId: 101,
  targetClassId: 81,
  resultStatus: 'AUTO_ASSIGNED',
  score: 8.8,
  issueCode: null,
  issueSeverity: null,
  explanation: 'Phân bổ cân bằng theo điểm học tập và tỷ lệ nam, nữ.',
}

const manualResult: PlacementResult = {
  id: 2,
  studentId: 102,
  targetClassId: null,
  resultStatus: 'MANUAL_REQUIRED',
  score: null,
  issueCode: 'SCORE_TIE',
  issueSeverity: 'WARNING',
  explanation: 'Có nhiều học sinh bằng điểm tại ranh giới chỉ tiêu; giáo vụ cần xếp lớp thủ công.',
}

describe('PlacementResultDetailDialog', () => {
  it('renders an automatically assigned result and closes dialog', async () => {
    const wrapper = mount(PlacementResultDetailDialog, {
      props: {
        visible: true,
        result: autoResult,
        studentLabel: 'HS101 - Nguyễn Văn A',
        targetClassLabel: 'Lớp 8A1',
      },
      global: {
        stubs: {
          Dialog: {
            props: ['visible', 'header'],
            template: '<div v-if="visible" data-testid="detail-dialog"><slot /><slot name="footer" /></div>',
          },
          Button: {
            props: ['label'],
            emits: ['click'],
            template: '<button @click="$emit(\'click\')">{{ label }}</button>',
          },
          Tag: { props: ['value'], template: '<span>{{ value }}</span>' },
        },
      },
    })

    expect(wrapper.text()).toContain('HS101 - Nguyễn Văn A')
    expect(wrapper.text()).toContain('Lớp 8A1')
    expect(wrapper.text()).toContain('Tự động')
    expect(wrapper.text()).toContain('8.8')
    expect(wrapper.text()).toContain('Phân bổ cân bằng theo điểm học tập và tỷ lệ nam, nữ.')
    expect(wrapper.text()).not.toContain('snapshot')
    expect(wrapper.text()).not.toContain('AUTO_ASSIGNED')

    const closeBtn = wrapper.findAll('button').find((b) => b.text() === 'Đóng')
    await closeBtn?.trigger('click')
    expect(wrapper.emitted('update:visible')).toEqual([[false]])
  })

  it('renders the tie explanation without a continue-manual action', async () => {
    const wrapper = mount(PlacementResultDetailDialog, {
      props: {
        visible: true,
        result: manualResult,
        studentLabel: 'HS102 - Trần Thị B',
        targetClassLabel: '—',
      },
      global: {
        stubs: {
          Dialog: {
            props: ['visible'],
            template: '<div v-if="visible"><slot /><slot name="footer" /></div>',
          },
          Button: {
            props: ['label'],
            emits: ['click'],
            template: '<button @click="$emit(\'click\')">{{ label }}</button>',
          },
          Tag: { props: ['value'], template: '<span>{{ value }}</span>' },
        },
      },
    })

    expect(wrapper.text()).toContain('Cần xếp thủ công')
    expect(wrapper.text()).not.toContain('SCORE_TIE')
    expect(wrapper.text()).toContain('Cảnh báo')
    expect(wrapper.text()).toContain('Có nhiều học sinh bằng điểm tại ranh giới chỉ tiêu; giáo vụ cần xếp lớp thủ công.')
    expect(wrapper.text()).toContain('Học sinh này chưa được xếp lớp tự động.')
    expect(wrapper.text()).not.toContain('Tiếp tục xếp thủ công')
    expect(wrapper.findAll('button').map((button) => button.text())).toEqual(['Đóng'])
    expect(wrapper.emitted('continue-manual')).toBeUndefined()

    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('update:visible')).toEqual([[false]])
  })
})
