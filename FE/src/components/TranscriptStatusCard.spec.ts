import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import TranscriptStatusCard from './TranscriptStatusCard.vue'
import type { ResTranscriptCalculationStatusDTO } from '@/types/transcript'

const sampleInProgressStatus: ResTranscriptCalculationStatusDTO = {
  studentId: 101,
  studentCode: 'HS0001',
  academicYearId: 2,
  semesterId: null,
  calculationStatus: 'IN_PROGRESS',
  sourceVersion: 12,
  calculatedVersion: 11,
  isUpToDate: false,
  calculatedAt: '2026-09-03T08:45:00',
  lastError: null,
}

const sampleFinishedStatus: ResTranscriptCalculationStatusDTO = {
  studentId: 102,
  studentCode: 'HS0002',
  academicYearId: 2,
  semesterId: null,
  calculationStatus: 'FINISH',
  sourceVersion: 8,
  calculatedVersion: 8,
  isUpToDate: true,
  calculatedAt: '2026-09-03T08:20:00',
  lastError: null,
}

const sampleErrorStatus: ResTranscriptCalculationStatusDTO = {
  ...sampleFinishedStatus,
  isUpToDate: false,
  lastError: 'Thiếu điểm hệ số môn Toán',
}

describe('TranscriptStatusCard.vue', () => {
  it('renders IN_PROGRESS status with warning badge and progress bar', () => {
    const wrapper = mount(TranscriptStatusCard, {
      props: {
        status: sampleInProgressStatus,
        studentName: 'Nguyễn Minh An',
      },
      global: {
        stubs: {
          ProgressBar: { template: '<div class="mock-progress" />' },
        },
      },
    })

    expect(wrapper.text()).toContain('Nguyễn Minh An · HS0001')
    expect(wrapper.text()).toContain('IN_PROGRESS')
    expect(wrapper.text()).toContain('Source version:')
    expect(wrapper.text()).toContain('12')
    expect(wrapper.find('.mock-progress').exists()).toBe(true)
  })

  it('renders FINISH status with up-to-date success badge', () => {
    const wrapper = mount(TranscriptStatusCard, {
      props: {
        status: sampleFinishedStatus,
        studentName: 'Trần Gia Bảo',
      },
      global: {
        stubs: {
          ProgressBar: true,
        },
      },
    })

    expect(wrapper.text()).toContain('Trần Gia Bảo · HS0002')
    expect(wrapper.text()).toContain('FINISH · up-to-date')
    expect(wrapper.find('.mock-progress').exists()).toBe(false)
  })

  it('renders calculation error when lastError is present', () => {
    const wrapper = mount(TranscriptStatusCard, {
      props: {
        status: sampleErrorStatus,
      },
      global: {
        stubs: {
          ProgressBar: true,
        },
      },
    })

    expect(wrapper.text()).toContain('Thiếu điểm hệ số môn Toán')
    expect(wrapper.text()).toContain('out-of-date')
  })

  it('emits refresh when refresh button is clicked', async () => {
    const wrapper = mount(TranscriptStatusCard, {
      props: {
        status: sampleFinishedStatus,
      },
      global: {
        stubs: {
          ProgressBar: true,
        },
      },
    })

    const refreshBtn = wrapper.findAll('button').find((b) => b.text() === 'Làm mới')
    await refreshBtn?.trigger('click')

    expect(wrapper.emitted('refresh')).toBeTruthy()
  })
})
