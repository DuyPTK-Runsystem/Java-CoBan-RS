import { shallowMount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ScoreEntryDialog from './ScoreEntryDialog.vue'

describe('ScoreEntryDialog', () => {
  it('requires a scored value and rejects more than one decimal', () => {
    const wrapper = shallowMount(ScoreEntryDialog, {
      props: { visible: true, score: null },
    })
    const view = wrapper.vm as unknown as {
      value: number | null
      save: () => void
      validationMessage: string
    }

    view.value = null
    view.save()
    expect(view.validationMessage).toContain('Vui lòng nhập điểm')
    expect(wrapper.emitted('save')).toBeUndefined()

    view.value = 1.25
    view.save()
    expect(view.validationMessage).toContain('tối đa một chữ số')
  })

  it('keeps zero and optimistic version in the request', async () => {
    const wrapper = shallowMount(ScoreEntryDialog, {
      props: {
        visible: false,
        score: {
          scoreId: 1,
          assessmentColumnId: 7,
          studentId: 11,
          studentCode: 'HS001',
          studentName: 'An',
          scoreStatus: 'SCORED',
          scoreValue: 0,
          note: null,
          enteredBy: null,
          enteredAt: null,
          updatedBy: null,
          updatedAt: null,
          version: 4,
        },
      },
    })
    await wrapper.setProps({ visible: true })
      ; (wrapper.vm as unknown as { save: () => void }).save()

    expect(wrapper.emitted('save')?.[0]).toEqual([{
      scoreStatus: 'SCORED',
      scoreValue: 0,
      note: null,
      expectedVersion: 4,
    }])
  })

  it('formats with one decimal only after the score input loses focus', () => {
    const wrapper = shallowMount(ScoreEntryDialog, {
      props: { visible: true, score: null },
    })
    const view = wrapper.vm as unknown as {
      scoreFocused: boolean
      minFractionDigits: () => number
    }

    view.scoreFocused = true
    expect(view.minFractionDigits()).toBe(0)

    view.scoreFocused = false
    expect(view.minFractionDigits()).toBe(1)
  })

  it('rounds score to one decimal on blur', () => {
    const wrapper = shallowMount(ScoreEntryDialog, {
      props: { visible: true, score: null },
    })
    const view = wrapper.vm as unknown as {
      value: number | null
      scoreFocused: boolean
      handleScoreBlur: () => void
    }

    view.scoreFocused = true
    view.value = 8.47
    view.handleScoreBlur()
    expect(view.scoreFocused).toBe(false)
    expect(view.value).toBe(8.5)
  })

  it('emits the proposed score and note when opening a correction request', () => {
    const score = {
      scoreId: 1,
      assessmentColumnId: 7,
      studentId: 11,
      studentCode: 'HS001',
      studentName: 'Nguyễn Minh An',
      scoreStatus: 'SCORED' as const,
      scoreValue: 7.5,
      note: null,
      enteredBy: 5,
      enteredAt: '2026-09-02T08:00:00',
      updatedBy: null,
      updatedAt: null,
      version: 3,
    }
    const wrapper = shallowMount(ScoreEntryDialog, {
      props: { visible: true, studentName: score.studentName, score },
    })
    const view = wrapper.vm as unknown as {
      value: number | null
      note: string
      requestChange: () => void
    }
    view.value = 8
    view.note = 'Điều chỉnh theo phiếu chấm.'

    view.requestChange()

    expect(wrapper.emitted('request-change')?.[0]).toEqual([{
      studentName: 'Nguyễn Minh An',
      score,
      proposedStatus: 'SCORED',
      proposedValue: 8,
      reason: 'Điều chỉnh theo phiếu chấm.',
    }])
    expect(wrapper.emitted('save')).toBeUndefined()
  })

  it('keeps the current score visible and disables direct save in read-only mode', async () => {
    const wrapper = shallowMount(ScoreEntryDialog, {
      props: {
        visible: false,
        readOnly: true,
        score: {
          scoreId: 1,
          assessmentColumnId: 7,
          studentId: 11,
          studentCode: 'HS001',
          studentName: 'Nguyễn Minh An',
          scoreStatus: 'SCORED',
          scoreValue: 7.5,
          note: 'Điểm hiện tại',
          enteredBy: 5,
          enteredAt: '2026-09-02T08:00:00',
          updatedBy: null,
          updatedAt: null,
          version: 3,
        },
      },
    })
    await wrapper.setProps({ visible: true })
    const view = wrapper.vm as unknown as { value: number | null; save: () => void }

    expect(view.value).toBe(7.5)
    view.save()

    expect(wrapper.emitted('save')).toBeUndefined()
  })
})
