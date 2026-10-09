import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import ScannerUploadDemo from './ScannerUploadDemo.vue'

const imageBitmap = { close: vi.fn() }

function mockDetector(rawValue: string | undefined) {
  const detector = { detect: vi.fn().mockResolvedValue(rawValue ? [{ rawValue }] : []) }
  const constructor = vi.fn(function (this: object) {
    return detector
  })
  Object.defineProperty(window, 'BarcodeDetector', { configurable: true, value: constructor })
  Object.defineProperty(window, 'createImageBitmap', { configurable: true, value: vi.fn().mockResolvedValue(imageBitmap) })
  return { detector, constructor }
}

function setFile(input: HTMLInputElement, file: File) {
  Object.defineProperty(input, 'files', { configurable: true, value: [file] })
  input.dispatchEvent(new Event('change'))
}

afterEach(() => {
  vi.restoreAllMocks()
  Reflect.deleteProperty(window, 'BarcodeDetector')
  Reflect.deleteProperty(window, 'createImageBitmap')
  imageBitmap.close.mockReset()
})

describe('ScannerUploadDemo', () => {
  it.each([
    { formats: ['qr_code'] as Array<'qr_code' | 'code_128'>, value: 'PATRON-QR-104' },
    { formats: ['code_128'] as Array<'qr_code' | 'code_128'>, value: 'COPY-128-009' },
  ])('decodes a supported image locally and emits the $value value', async ({ formats, value }) => {
    const { detector, constructor } = mockDetector(` ${value} `)
    const wrapper = mount(ScannerUploadDemo, {
      props: { id: 'scanner', label: 'Mã cần quét', formats },
    })
    const file = new File(['image bytes'], 'code.png', { type: 'image/png' })

    setFile(wrapper.get('input[type="file"]').element as HTMLInputElement, file)
    await vi.waitFor(() => expect(wrapper.emitted('decoded')).toEqual([[value]]))

    expect(constructor).toHaveBeenCalledWith({ formats })
    expect(detector.detect).toHaveBeenCalledWith(imageBitmap)
    expect(wrapper.get('output').text()).toBe(value)
    expect(wrapper.text()).toContain('ảnh không được gửi lên máy chủ')
    expect(imageBitmap.close).toHaveBeenCalledOnce()
  })

  it('shows a manual-entry fallback when BarcodeDetector is unsupported', async () => {
    Object.defineProperty(window, 'createImageBitmap', { configurable: true, value: vi.fn().mockResolvedValue(imageBitmap) })
    const wrapper = mount(ScannerUploadDemo, { props: { id: 'scanner', label: 'Mã thẻ' } })

    setFile(wrapper.get('input[type="file"]').element as HTMLInputElement,
      new File(['image bytes'], 'card.png', { type: 'image/png' }))
    await vi.waitFor(() => expect(wrapper.text()).toContain('chưa hỗ trợ đọc ảnh QR/Code 128'))

    expect(wrapper.emitted('decoded')).toBeUndefined()
    expect(wrapper.find('output').exists()).toBe(false)
  })

  it('shows a manual-entry fallback when the image has no supported code', async () => {
    const { detector } = mockDetector(undefined)
    const wrapper = mount(ScannerUploadDemo, { props: { id: 'scanner', label: 'Mã sách' } })

    setFile(wrapper.get('input[type="file"]').element as HTMLInputElement,
      new File(['image bytes'], 'empty.png', { type: 'image/png' }))
    await vi.waitFor(() => expect(wrapper.text()).toContain('Không tìm thấy QR hoặc Code 128'))

    expect(detector.detect).toHaveBeenCalledOnce()
    expect(wrapper.emitted('decoded')).toBeUndefined()
    expect(wrapper.find('output').exists()).toBe(false)
  })

  it('rejects non-image files before decoding and retains manual entry', async () => {
    const createImageBitmap = vi.fn()
    Object.defineProperty(window, 'createImageBitmap', { configurable: true, value: createImageBitmap })
    const wrapper = mount(ScannerUploadDemo, { props: { id: 'scanner', label: 'Mã sách' } })

    setFile(wrapper.get('input[type="file"]').element as HTMLInputElement,
      new File(['text'], 'codes.txt', { type: 'text/plain' }))
    await vi.waitFor(() => expect(wrapper.text()).toContain('Chỉ hỗ trợ tệp ảnh'))

    expect(createImageBitmap).not.toHaveBeenCalled()
    expect(wrapper.emitted('decoded')).toBeUndefined()
  })
})
