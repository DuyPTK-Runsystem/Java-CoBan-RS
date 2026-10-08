<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import JsBarcode from 'jsbarcode'

import type { BookCopy } from '@/types/library/catalog'

const props = withDefaults(defineProps<{
  visible?: boolean
  copies?: BookCopy[]
  bookTitle?: string
}>(), {
  visible: false,
  copies: () => [],
  bookTitle: '',
})

const emit = defineEmits<{
  'update:visible': [visible: boolean]
}>()

const LABELS_PER_PAGE = 50

const pages = computed<BookCopy[][]>(() => {
  const result: BookCopy[][] = []
  const list = props.copies ?? []
  for (let i = 0; i < list.length; i += LABELS_PER_PAGE) {
    result.push(list.slice(i, i + LABELS_PER_PAGE))
  }
  return result
})

const totalPages = computed(() => pages.value.length)
const totalCopies = computed(() => props.copies?.length ?? 0)

const printAreaRef = ref<HTMLElement | null>(null)

function getCopyTitle(copy: BookCopy): string {
  return (copy as { book?: { title?: string } }).book?.title || props.bookTitle || ''
}

function getCopyShelfLocation(copy: BookCopy): string {
  return copy.shelfLocation?.trim() || 'Chưa xếp kệ'
}

function getTitleSizeClass(title: string): string {
  const len = title.trim().length
  if (len > 45) return 'title-compact'
  if (len > 25) return 'title-medium'
  return 'title-normal'
}

function renderBarcodes(): void {
  if (!printAreaRef.value) return
  const svgElements = printAreaRef.value.querySelectorAll<SVGSVGElement>('svg[data-barcode]')
  svgElements.forEach((svg) => {
    const code = svg.getAttribute('data-barcode')
    if (!code || !code.trim()) {
      svg.innerHTML = '<text x="50%" y="50%" text-anchor="middle" font-size="9" fill="#dc2626">Trống</text>'
      return
    }
    try {
      JsBarcode(svg, code.trim(), {
        format: 'CODE128',
        width: 1.0,
        height: 25,
        displayValue: true,
        fontSize: 9,
        margin: 0,
        textMargin: 1,
        font: 'monospace',
      })
    } catch {
      svg.innerHTML = '<text x="50%" y="50%" text-anchor="middle" font-size="9" fill="#dc2626">Mã vạch lỗi</text>'
    }
  })
}

watch(
  () => [props.visible, props.copies],
  async ([visible]) => {
    if (visible) {
      await nextTick()
      renderBarcodes()
    }
  },
  { immediate: true, deep: true }
)

function handlePrint(): void {
  const printArea = printAreaRef.value
  if (!printArea) {
    window.print()
    return
  }

  const iframe = document.createElement('iframe')
  iframe.className = 'barcode-print-frame'
  iframe.setAttribute('style', 'position:fixed;top:0;left:0;width:0;height:0;border:none;visibility:hidden;')
  document.body.appendChild(iframe)

  const doc = iframe.contentWindow?.document
  if (!doc) {
    if (iframe.parentNode) iframe.parentNode.removeChild(iframe)
    window.print()
    return
  }

  const printStyles = `
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }
    @page {
      size: A4 portrait;
      margin: 10mm;
    }
    html, body {
      margin: 0;
      padding: 0;
      background: #ffffff;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
    .no-print, .a4-page-banner {
      display: none !important;
    }
    .a4-page-wrapper {
      margin: 0;
      padding: 0;
    }
    .a4-page {
      box-sizing: border-box;
      width: 190mm;
      height: 277mm;
      background: #ffffff;
      display: grid;
      grid-template-columns: repeat(5, 38mm);
      grid-template-rows: repeat(10, 27.7mm);
      page-break-after: always;
      break-after: page;
    }
    .a4-page:last-child {
      page-break-after: auto;
      break-after: auto;
    }
    .barcode-label {
      box-sizing: border-box;
      width: 38mm;
      height: 27.7mm;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 1mm 2.5mm;
      border: 0.5px dashed #cbd5e1;
      overflow: hidden;
      text-align: center;
    }
    .barcode-svg {
      width: 100%;
      max-width: 32mm;
      max-height: 13.5mm;
      height: auto;
      display: block;
    }
    .barcode-label-title {
      display: -webkit-box;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 2;
      overflow: hidden;
      text-overflow: ellipsis;
      word-break: break-word;
      width: 100%;
      max-width: 33mm;
      max-height: 5.6mm;
      font-weight: 500;
      color: #000000;
      margin-top: 0.4mm;
      text-align: center;
    }
    .barcode-label-title.title-normal {
      font-size: 6.8pt;
      line-height: 1.15;
    }
    .barcode-label-title.title-medium {
      font-size: 6.2pt;
      line-height: 1.12;
    }
    .barcode-label-title.title-compact {
      font-size: 5.5pt;
      line-height: 1.1;
    }
    .barcode-label-shelf {
      display: block;
      width: 100%;
      max-width: 33mm;
      font-size: 6.8pt;
      line-height: 1.15;
      font-weight: 600;
      color: #000000;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      margin-top: 0.3mm;
      text-align: center;
    }
  `

  doc.open()
  doc.write('<!DOCTYPE html><html><head><title>' + (props.bookTitle || 'In mã vạch') + '</title><style>' + printStyles + '</style></head><body>' + printArea.innerHTML + '</body></html>')
  doc.close()

  const cleanup = () => {
    if (iframe.parentNode) {
      iframe.parentNode.removeChild(iframe)
    }
  }

  const printWindow = iframe.contentWindow
  if (printWindow && typeof printWindow.print === 'function') {
    try {
      printWindow.focus?.()
    } catch {
      // Ignore focus not implemented in jsdom
    }
    try {
      printWindow.print()
    } finally {
      setTimeout(cleanup, 1000)
    }
    return
  }

  window.print()
  cleanup()
}

function handleClose(): void {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="props.visible"
    modal
    header="Xem trước in mã vạch (A4)"
    :style="{ width: 'min(96vw, 1020px)' }"
    class="barcode-print-modal"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="print-dialog-header-info">
      <div class="info-meta">
        <span v-if="props.bookTitle" class="book-title-badge">{{ props.bookTitle }}</span>
        <span>Tổng số: <strong>{{ totalCopies }}</strong> bản sao · <strong>{{ totalPages }}</strong> trang A4 (50 nhãn/trang, 5 cột × 10 dòng)</span>
      </div>
      <div class="print-dialog-actions">
        <Button label="In ngay" icon="pi pi-print" severity="success" :disabled="totalCopies === 0" @click="handlePrint" />
        <Button label="Đóng" severity="secondary" outlined @click="handleClose" />
      </div>
    </div>

    <div ref="printAreaRef" class="barcode-preview-scroll">
      <div v-if="totalCopies === 0" class="empty-print-state">
        <i class="pi pi-info-circle" aria-hidden="true" />
        <p>Chưa có bản sao nào được chọn để in.</p>
      </div>

      <div
        v-for="(page, pageIdx) in pages"
        :key="pageIdx"
        class="a4-page-wrapper"
      >
        <div class="a4-page-banner no-print">
          <span>Trang {{ pageIdx + 1 }} / {{ totalPages }} ({{ page.length }} nhãn)</span>
        </div>
        <div class="a4-page" :data-page-index="pageIdx">
          <div
            v-for="copy in page"
            :key="copy.id"
            class="barcode-label"
          >
            <svg class="barcode-svg" :data-barcode="copy.barcode" />
            <span
              v-if="getCopyTitle(copy)"
              class="barcode-label-title"
              :class="getTitleSizeClass(getCopyTitle(copy))"
              :title="getCopyTitle(copy)"
            >
              {{ getCopyTitle(copy) }}
            </span>
            <span class="barcode-label-shelf" :title="getCopyShelfLocation(copy)">
              {{ getCopyShelfLocation(copy) }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.print-dialog-header-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1rem;
  padding-bottom: .75rem;
  border-bottom: 1px solid var(--surface-border, #e2e8f0);
}
.info-meta {
  display: flex;
  flex-direction: column;
  gap: .25rem;
  font-size: .875rem;
  color: var(--text-color, #1e293b);
}
.book-title-badge {
  font-weight: 600;
  color: var(--primary-color, #10b981);
}
.print-dialog-actions {
  display: flex;
  gap: .5rem;
}
.barcode-preview-scroll {
  background: #f1f5f9;
  padding: 1.5rem;
  border-radius: .5rem;
  max-height: 70vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}
.empty-print-state {
  padding: 3rem;
  text-align: center;
  color: var(--text-color-secondary, #64748b);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: .5rem;
}
.empty-print-state i {
  font-size: 2rem;
}
.a4-page-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.a4-page-banner {
  font-size: .8125rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: .5rem;
}
.a4-page {
  box-sizing: border-box;
  width: 190mm;
  height: 277mm;
  background: #ffffff;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
  display: grid;
  grid-template-columns: repeat(5, 38mm);
  grid-template-rows: repeat(10, 27.7mm);
  page-break-after: always;
  break-after: page;
}
.a4-page:last-child {
  page-break-after: auto;
  break-after: auto;
}
.barcode-label {
  box-sizing: border-box;
  width: 38mm;
  height: 27.7mm;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 1mm 2.5mm;
  border: 0.5px dashed #cbd5e1;
  overflow: hidden;
  text-align: center;
}
.barcode-svg {
  width: 100%;
  max-width: 32mm;
  max-height: 13.5mm;
  height: auto;
  display: block;
}
.barcode-label-title {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-word;
  width: 100%;
  max-width: 33mm;
  max-height: 5.6mm;
  font-weight: 500;
  color: #1e293b;
  margin-top: 0.4mm;
  text-align: center;
}
.barcode-label-title.title-normal {
  font-size: 6.8pt;
  line-height: 1.15;
}
.barcode-label-title.title-medium {
  font-size: 6.2pt;
  line-height: 1.12;
}
.barcode-label-title.title-compact {
  font-size: 5.5pt;
  line-height: 1.1;
}
.barcode-label-shelf {
  display: block;
  width: 100%;
  max-width: 33mm;
  font-size: 6.8pt;
  line-height: 1.15;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 0.3mm;
  text-align: center;
}

</style>

<style>
@page {
  size: A4 portrait;
  margin: 10mm;
}

@media print {
  body > :not(.p-dialog-mask):not(.barcode-print-frame) {
    display: none !important;
  }
  .app-header,
  .sidebar,
  .page-content,
  .p-dialog-header,
  .p-dialog-footer,
  .print-dialog-header-info,
  .a4-page-banner,
  .no-print {
    display: none !important;
  }
  .p-dialog-mask {
    position: static !important;
    display: block !important;
    padding: 0 !important;
    margin: 0 !important;
    background: transparent !important;
    overflow: visible !important;
  }
  .p-dialog {
    position: static !important;
    max-height: none !important;
    max-width: none !important;
    width: auto !important;
    margin: 0 !important;
    padding: 0 !important;
    border: none !important;
    box-shadow: none !important;
    background: transparent !important;
    transform: none !important;
    overflow: visible !important;
  }
  .p-dialog-content {
    overflow: visible !important;
    padding: 0 !important;
    margin: 0 !important;
    max-height: none !important;
    background: transparent !important;
  }
  .barcode-preview-scroll {
    overflow: visible !important;
    max-height: none !important;
    padding: 0 !important;
    margin: 0 !important;
    background: transparent !important;
  }
  .a4-page-wrapper {
    margin: 0 !important;
    padding: 0 !important;
  }
  .a4-page {
    box-shadow: none !important;
    margin: 0 !important;
    background: #ffffff !important;
    break-after: page !important;
    page-break-after: always !important;
  }
  .a4-page:last-child {
    break-after: auto !important;
    page-break-after: auto !important;
  }
  .barcode-label {
    border: 0.5px dashed #cbd5e1 !important;
  }
  .barcode-label-title,
  .barcode-label-shelf {
    color: #000000 !important;
  }
}
</style>

