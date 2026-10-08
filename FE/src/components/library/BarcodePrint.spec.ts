import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import BarcodePrint from './BarcodePrint.vue'
import type { BookCopy } from '@/types/library/catalog'

function makeCopies(count: number): BookCopy[] {
  return Array.from({ length: count }, (_, i) => ({
    id: i + 1,
    bookId: 10,
    barcode: `LIB-${String(i + 1).padStart(9, '0')}`,
    status: 'AVAILABLE',
    shelfLocation: 'A-01',
    referenceOnly: false,
    version: 1,
  }))
}

describe('BarcodePrint Component', () => {
  beforeEach(() => {
    HTMLCanvasElement.prototype.getContext = vi.fn().mockReturnValue({
      measureText: (text: string) => ({ width: text.length * 7 }),
      font: '',
    })
    vi.spyOn(window, 'print').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders exactly 1 A4 page when given 50 copies', async () => {
    const copies = makeCopies(50)
    const wrapper = mount(BarcodePrint, {
      props: { visible: true, copies, bookTitle: 'Lập trình Java' },
      global: {
        stubs: {
          Dialog: {
            props: ['visible', 'header'],
            template: '<div v-if="visible" data-testid="dialog"><slot /></div>',
          },
        },
      },
    })
    await flushPromises()

    const pages = wrapper.findAll('.a4-page')
    expect(pages).toHaveLength(1)
    expect(pages[0]?.findAll('.barcode-label')).toHaveLength(50)
    expect(wrapper.text()).toContain('50 bản sao · 1 trang A4')
  })

  it('renders exactly 2 A4 pages when given 80 copies (50 on page 1, 30 on page 2)', async () => {
    const copies = makeCopies(80)
    const wrapper = mount(BarcodePrint, {
      props: { visible: true, copies, bookTitle: 'Cấu trúc dữ liệu' },
      global: {
        stubs: {
          Dialog: {
            props: ['visible', 'header'],
            template: '<div v-if="visible" data-testid="dialog"><slot /></div>',
          },
        },
      },
    })
    await flushPromises()

    const pages = wrapper.findAll('.a4-page')
    expect(pages).toHaveLength(2)
    expect(pages[0]?.findAll('.barcode-label')).toHaveLength(50)
    expect(pages[1]?.findAll('.barcode-label')).toHaveLength(30)
    expect(wrapper.text()).toContain('80 bản sao · 2 trang A4')
  })

  it('renders SVG barcode with matching barcode attribute for each book copy', async () => {
    const copies = makeCopies(3)
    const wrapper = mount(BarcodePrint, {
      props: { visible: true, copies },
      global: {
        stubs: {
          Dialog: {
            props: ['visible'],
            template: '<div v-if="visible"><slot /></div>',
          },
        },
      },
    })
    await flushPromises()

    const svgs = wrapper.findAll('svg.barcode-svg')
    expect(svgs).toHaveLength(3)
    expect(svgs[0]?.attributes('data-barcode')).toBe('LIB-000000001')
    expect(svgs[1]?.attributes('data-barcode')).toBe('LIB-000000002')
    expect(svgs[2]?.attributes('data-barcode')).toBe('LIB-000000003')
  })

  it('renders book title and shelf location for each barcode label', async () => {
    const copies: BookCopy[] = [
      { id: 1, bookId: 10, barcode: 'LIB-000000001', shelfLocation: 'A-03-05-02', status: 'AVAILABLE', referenceOnly: false, version: 1 },
      { id: 2, bookId: 10, barcode: 'LIB-000000002', shelfLocation: null, status: 'AVAILABLE', referenceOnly: false, version: 1 },
    ]
    const wrapper = mount(BarcodePrint, {
      props: { visible: true, copies, bookTitle: 'Dế mèn phiêu lưu kí' },
      global: {
        stubs: {
          Dialog: {
            props: ['visible'],
            template: '<div v-if="visible"><slot /></div>',
          },
        },
      },
    })
    await flushPromises()

    const titles = wrapper.findAll('.barcode-label-title')
    expect(titles).toHaveLength(2)
    expect(titles[0]?.text()).toBe('Dế mèn phiêu lưu kí')
    expect(titles[1]?.text()).toBe('Dế mèn phiêu lưu kí')

    const shelves = wrapper.findAll('.barcode-label-shelf')
    expect(shelves).toHaveLength(2)
    expect(shelves[0]?.text()).toBe('A-03-05-02')
    expect(shelves[1]?.text()).toBe('Chưa xếp kệ')
  })

  it('scales title font size class based on title length', async () => {
    const copies: BookCopy[] = [
      { id: 1, bookId: 10, barcode: 'LIB-000000001', shelfLocation: 'A-01', status: 'AVAILABLE', referenceOnly: false, version: 1 },
    ]
    const wrapperShort = mount(BarcodePrint, {
      props: { visible: true, copies, bookTitle: 'Dế mèn phiêu lưu kí' },
      global: { stubs: { Dialog: { props: ['visible'], template: '<div v-if="visible"><slot /></div>' } } },
    })
    const wrapperMed = mount(BarcodePrint, {
      props: { visible: true, copies, bookTitle: 'Thiên thần và ác quỷ - Tiểu thuyết' },
      global: { stubs: { Dialog: { props: ['visible'], template: '<div v-if="visible"><slot /></div>' } } },
    })
    const wrapperLong = mount(BarcodePrint, {
      props: { visible: true, copies, bookTitle: 'Lập trình hướng đối tượng với ngôn ngữ Java từ cơ bản đến nâng cao toàn tập' },
      global: { stubs: { Dialog: { props: ['visible'], template: '<div v-if="visible"><slot /></div>' } } },
    })
    await flushPromises()

    expect(wrapperShort.find('.barcode-label-title').classes()).toContain('title-normal')
    expect(wrapperMed.find('.barcode-label-title').classes()).toContain('title-medium')
    expect(wrapperLong.find('.barcode-label-title').classes()).toContain('title-compact')
  })

  it('triggers print flow with isolated iframe when the In ngay button is clicked', async () => {
    const copies = makeCopies(5)
    let printedHtml = ''
    const printSpy = vi.fn()
    const originalAppend = document.body.appendChild.bind(document.body)
    vi.spyOn(document.body, 'appendChild').mockImplementation((node) => {
      const res = originalAppend(node)
      if (node instanceof HTMLIFrameElement && node.contentWindow) {
        node.contentWindow.focus = vi.fn()
        node.contentWindow.print = printSpy
        const origWrite = node.contentWindow.document.write.bind(node.contentWindow.document)
        node.contentWindow.document.write = (html: string) => {
          printedHtml = html
          return origWrite(html)
        }
      }
      return res
    })

    const wrapper = mount(BarcodePrint, {
      props: { visible: true, copies, bookTitle: 'Sách giáo khoa' },
      global: {
        stubs: {
          Dialog: {
            props: ['visible'],
            template: '<div v-if="visible"><slot /></div>',
          },
        },
      },
    })
    await flushPromises()

    const printButton = wrapper.findAll('button').find((b) => b.text().includes('In ngay'))
    expect(printButton).toBeDefined()
    await printButton!.trigger('click')

    expect(printSpy).toHaveBeenCalledOnce()
    expect(printedHtml).toContain('<!DOCTYPE html>')
    expect(printedHtml).toContain('Sách giáo khoa')
    expect(printedHtml).toContain('LIB-000000001')
    expect(printedHtml).toContain('@page {')
  })

  it('displays empty state and disables print button when copies list is empty', async () => {
    const wrapper = mount(BarcodePrint, {
      props: { visible: true, copies: [] },
      global: {
        stubs: {
          Dialog: {
            props: ['visible'],
            template: '<div v-if="visible"><slot /></div>',
          },
        },
      },
    })
    await flushPromises()

    expect(wrapper.find('.empty-print-state').exists()).toBe(true)
    const printButton = wrapper.findAll('button').find((b) => b.text().includes('In ngay'))
    expect(printButton?.attributes('disabled')).toBeDefined()
  })

  it('handles invalid or empty barcodes gracefully without crashing', async () => {
    const copiesWithInvalid: BookCopy[] = [
      { id: 1, bookId: 1, barcode: '', status: 'AVAILABLE', shelfLocation: null, referenceOnly: false, version: 1 },
      { id: 2, bookId: 1, barcode: '   ', status: 'AVAILABLE', shelfLocation: null, referenceOnly: false, version: 1 },
    ]
    const wrapper = mount(BarcodePrint, {
      props: { visible: true, copies: copiesWithInvalid },
      global: {
        stubs: {
          Dialog: {
            props: ['visible'],
            template: '<div v-if="visible"><slot /></div>',
          },
        },
      },
    })
    await flushPromises()

    expect(wrapper.findAll('.barcode-label')).toHaveLength(2)
  })
})

