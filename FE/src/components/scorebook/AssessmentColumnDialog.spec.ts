import { shallowMount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AssessmentColumnDialog from './AssessmentColumnDialog.vue'

describe('AssessmentColumnDialog', () => {
  it('submits assessmentType and optional columnName without columnNo in create mode', () => {
    const wrapper = shallowMount(AssessmentColumnDialog, {
      props: { visible: true, mode: 'create', column: null },
    })
    const view = wrapper.vm as unknown as {
      type: string
      columnName: string
      save: () => void
    }
    view.type = 'KTĐK'
    view.columnName = 'Giữa kỳ'
    view.save()

    expect(wrapper.emitted('save')?.[0]).toEqual([{
      assessmentType: 'KTĐK',
      columnName: 'Giữa kỳ',
    }])
  })

  it('updates only the column name in edit mode', () => {
    const wrapper = shallowMount(AssessmentColumnDialog, {
      props: {
        visible: true,
        mode: 'edit',
        column: {
          id: 7,
          scorebookId: 1,
          assessmentType: 'KTTT',
          columnNo: 1,
          columnName: 'Cũ',
          weightFactor: null,
          required: false,
          status: 'ACTIVE',
        },
      },
    })
    const view = wrapper.vm as unknown as { columnName: string; save: () => void }
    view.columnName = 'Tên mới'
    view.save()

    expect(wrapper.emitted('save')?.[0]).toEqual([{ columnName: 'Tên mới' }])
  })
})

