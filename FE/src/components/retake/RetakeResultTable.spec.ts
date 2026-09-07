import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import RetakeResultTable from './RetakeResultTable.vue'
import type { RetakeRowItem } from '@/types/retake'

const sampleItems: RetakeRowItem[] = [
  {
    retakeId: 101,
    studentId: 1001,
    studentCode: 'HS0001',
    studentName: 'Nguyễn Minh An',
    academicYearId: 1,
    academicYearCode: '2026-2027',
    subjectId: 21,
    subjectName: 'Toán',
    preRetakeScore: 4.0,
    retakeScore: 6.5,
    officialDtbmhCn: 6.5,
    examDate: '2027-06-15',
    status: 'SCORED',
    calculationStatus: 'FINISH',
    lastTaskId: 8801,
    note: 'Đã đối chiếu',
  },
  {
    retakeId: 102,
    studentId: 1002,
    studentCode: 'HS0002',
    studentName: 'Trần Gia Bảo',
    academicYearId: 1,
    academicYearCode: '2026-2027',
    subjectId: 22,
    subjectName: 'Ngữ văn',
    preRetakeScore: 4.5,
    retakeScore: null,
    officialDtbmhCn: null,
    examDate: '2027-06-15',
    status: 'PLANNED',
    calculationStatus: null,
    lastTaskId: null,
    note: null,
  },
  {
    retakeId: 103,
    studentId: 1003,
    studentCode: 'HS0003',
    studentName: 'Lê Hoàng Chi',
    academicYearId: 1,
    academicYearCode: '2026-2027',
    subjectId: 23,
    subjectName: 'Vật lý',
    preRetakeScore: 3.8,
    retakeScore: 5.0,
    officialDtbmhCn: 5.0,
    examDate: '2027-06-15',
    status: 'SCORED',
    calculationStatus: 'IN_PROGRESS',
    lastTaskId: null,
    note: null,
  },
  {
    retakeId: 104,
    studentId: 1004,
    studentCode: 'HS0004',
    studentName: 'Phạm Anh Dũng',
    academicYearId: 1,
    academicYearCode: '2026-2027',
    subjectId: 24,
    subjectName: 'Hóa học',
    preRetakeScore: 4.0,
    retakeScore: null,
    officialDtbmhCn: null,
    examDate: null,
    status: 'CANCELLED',
    calculationStatus: null,
    lastTaskId: null,
    note: null,
  },
]

describe('RetakeResultTable', () => {
  it('renders all rows and displays correct student, subject, and scores', () => {
    const wrapper = mount(RetakeResultTable, {
      props: { items: sampleItems },
    })

    const rows = wrapper.findAll('tbody tr')
    expect(rows.length).toBe(4)

    // Row 1: SCORED with FINISH
    expect(rows[0].text()).toContain('Nguyễn Minh An')
    expect(rows[0].text()).toContain('Toán')
    expect(rows[0].text()).toContain('4.0')
    expect(rows[0].text()).toContain('6.5')
    expect(rows[0].text()).toContain('FINISH')
    expect(rows[0].text()).toContain('SCORED')

    // Row 2: PLANNED with null score
    expect(rows[1].text()).toContain('Trần Gia Bảo')
    expect(rows[1].text()).toContain('Ngữ văn')
    expect(rows[1].text()).toContain('Chưa nhập')
    expect(rows[1].text()).toContain('PLANNED')

    // Row 3: SCORED with IN_PROGRESS
    expect(rows[2].text()).toContain('Lê Hoàng Chi')
    expect(rows[2].text()).toContain('IN_PROGRESS')

    // Row 4: CANCELLED
    expect(rows[3].text()).toContain('Phạm Anh Dũng')
    expect(rows[3].text()).toContain('CANCELLED')
    expect(rows[3].text()).toContain('Read-only')
  })

  it('displays appropriate action buttons based on row status', () => {
    const wrapper = mount(RetakeResultTable, {
      props: { items: sampleItems },
    })

    // Row 1 (SCORED): button "Xem/sửa" and "Hủy"
    const btnScore1 = wrapper.find('[data-testid="btn-score-101"]')
    expect(btnScore1.exists()).toBe(true)
    expect(btnScore1.text()).toBe('Xem/sửa')

    const btnCancel1 = wrapper.find('[data-testid="btn-cancel-101"]')
    expect(btnCancel1.exists()).toBe(true)
    expect(btnCancel1.text()).toBe('Hủy')

    // Row 2 (PLANNED): button "Nhập điểm" and "Hủy"
    const btnScore2 = wrapper.find('[data-testid="btn-score-102"]')
    expect(btnScore2.exists()).toBe(true)
    expect(btnScore2.text()).toBe('Nhập điểm')

    // Row 4 (CANCELLED): no buttons, only "Read-only"
    expect(wrapper.find('[data-testid="btn-score-104"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="btn-cancel-104"]').exists()).toBe(false)
  })

  it('emits editScore and cancel events when clicking buttons', async () => {
    const wrapper = mount(RetakeResultTable, {
      props: { items: sampleItems },
    })

    await wrapper.find('[data-testid="btn-score-101"]').trigger('click')
    expect(wrapper.emitted('editScore')).toBeTruthy()
    expect(wrapper.emitted('editScore')?.[0]?.[0]).toEqual(sampleItems[0])

    await wrapper.find('[data-testid="btn-cancel-102"]').trigger('click')
    expect(wrapper.emitted('cancel')).toBeTruthy()
    expect(wrapper.emitted('cancel')?.[0]?.[0]).toEqual(sampleItems[1])
  })

  it('does not fabricate official score or task ID when missing from transcript', () => {
    const itemWithoutTranscript: RetakeRowItem = {
      retakeId: 105,
      studentId: 1005,
      academicYearId: 1,
      subjectId: 25,
      preRetakeScore: 3.5,
      retakeScore: 7.0,
      officialDtbmhCn: null,
      examDate: '2027-06-15',
      status: 'SCORED',
      calculationStatus: 'FINISH',
      lastTaskId: null,
      note: null,
    }

    const wrapper = mount(RetakeResultTable, {
      props: { items: [itemWithoutTranscript] },
    })

    const row = wrapper.find('tbody tr')
    expect(row.findAll('td')[4]?.text()).toBe('—')
    expect(row.text()).toContain('Đã đồng bộ')
    expect(row.text()).not.toContain('8801')
  })
})
