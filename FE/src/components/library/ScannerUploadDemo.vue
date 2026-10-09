<script setup lang="ts">
import { computed, ref } from 'vue'

type BarcodeFormat = 'qr_code' | 'code_128'
type Detection = { rawValue: string }
type Detector = { detect: (image: ImageBitmap) => Promise<Detection[]> }
type DetectorConstructor = {
  new (options: { formats: BarcodeFormat[] }): Detector
  getSupportedFormats?: () => Promise<BarcodeFormat[]>
}

const props = withDefaults(defineProps<{
  id: string
  label: string
  formats?: BarcodeFormat[]
}>(), {
  formats: () => ['qr_code', 'code_128'],
})

const emit = defineEmits<{
  decoded: [value: string]
}>()

const busy = ref(false)
const message = ref('Chọn ảnh QR hoặc barcode; ảnh sẽ được xử lý ngay trên thiết bị này.')
const decodedValue = ref('')
const isError = ref(false)
const inputId = computed(() => `${props.id}-file`)

async function decodeFile(file: File): Promise<string> {
  const bitmap = await createImageBitmap(file)
  try {
    const DetectorApi = (window as Window & { BarcodeDetector?: DetectorConstructor }).BarcodeDetector
    if (!DetectorApi) throw new Error('UNSUPPORTED')
    const supported = await DetectorApi.getSupportedFormats?.()
    const formats = props.formats.filter((format) => (format === 'qr_code' || format === 'code_128')
      && (!supported || supported.includes(format)))
    if (formats.length === 0) throw new Error('UNSUPPORTED')
    const detector = new DetectorApi({ formats })
    const results = await detector.detect(bitmap)
    return results[0]?.rawValue?.trim() ?? ''
  } finally {
    bitmap.close()
  }
}

async function handleFile(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  decodedValue.value = ''
  isError.value = false
  if (!file) return

  if (!file.type.startsWith('image/')) {
    isError.value = true
    message.value = 'Chỉ hỗ trợ tệp ảnh. Bạn có thể nhập mã bằng tay.'
    input.value = ''
    return
  }

  busy.value = true
  message.value = 'Đang đọc ảnh trong trình duyệt…'
  try {
    const value = await decodeFile(file)
    if (!value) {
      isError.value = true
      message.value = 'Không tìm thấy QR hoặc Code 128 trong ảnh. Hãy thử ảnh rõ hơn hoặc nhập mã bằng tay.'
      return
    }
    decodedValue.value = value
    message.value = `Đã đọc ${file.name}. Chuỗi được đưa vào ô nhập; ảnh không được gửi lên máy chủ.`
    emit('decoded', value)
  } catch (error) {
    isError.value = true
    message.value = error instanceof Error && error.message === 'UNSUPPORTED'
      ? 'Trình duyệt này chưa hỗ trợ đọc ảnh QR/Code 128. Hãy nhập mã bằng tay.'
      : 'Không thể đọc ảnh này trong trình duyệt. Hãy thử ảnh khác hoặc nhập mã bằng tay.'
  } finally {
    busy.value = false
    input.value = ''
  }
}
</script>

<template>
  <section :aria-labelledby="`${id}-label`" class="scanner-upload">
    <div class="scanner-upload__head">
      <label class="scanner-upload__button" :for="inputId">Chọn ảnh</label>
      <span :id="`${id}-label`" class="scanner-upload__label">{{ label }}</span>
      <input
        :id="inputId"
        class="scanner-upload__input"
        type="file"
        accept="image/*"
        :disabled="busy"
        @change="handleFile"
      >
    </div>
    <p class="scanner-upload__message" :class="{ 'scanner-upload__message--error': isError }" aria-live="polite">
      {{ message }}
    </p>
    <output v-if="decodedValue" class="scanner-upload__result" :for="inputId">{{ decodedValue }}</output>
  </section>
</template>

<style scoped>
.scanner-upload { display: grid; gap: .5rem; padding: .75rem; border: 1px dashed var(--surface-border, #cbd5e1); border-radius: .5rem; background: var(--surface-50, #f8fafc); }
.scanner-upload__head { display: flex; align-items: center; gap: .65rem; flex-wrap: wrap; }
.scanner-upload__button { display: inline-flex; align-items: center; min-height: 2.25rem; padding: 0 .75rem; border: 1px solid var(--primary-color, #4f46e5); border-radius: .375rem; color: var(--primary-color, #4f46e5); font-weight: 600; cursor: pointer; }
.scanner-upload__button:hover { background: var(--primary-50, #eef2ff); }
.scanner-upload:focus-within .scanner-upload__button { outline: 2px solid var(--primary-color, #4f46e5); outline-offset: 2px; }
.scanner-upload__label { color: var(--text-color, #334155); font-size: .875rem; font-weight: 600; }
.scanner-upload__input { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; clip-path: inset(50%); }
.scanner-upload__message { margin: 0; color: var(--text-color-secondary, #64748b); font-size: .8125rem; }
.scanner-upload__message--error { color: var(--red-600, #dc2626); }
.scanner-upload__result { padding: .5rem .65rem; border-radius: .375rem; background: var(--green-50, #f0fdf4); color: var(--green-800, #166534); overflow-wrap: anywhere; font-family: monospace; }
</style>
