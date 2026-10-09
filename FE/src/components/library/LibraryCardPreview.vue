<script setup lang="ts">
import { computed } from 'vue'

import { formatLibraryDate } from '@/utils/libraryCardDates'

const props = defineProps<{
  cardNo: string
  displayName: string
  patronId: number
  status: string
  expiresAt: string
  qrUrl?: string
  downloadingQr?: boolean
}>()

const statusLabel = computed(() => ({ ACTIVE: 'ĐANG HOẠT ĐỘNG', EXPIRED: 'ĐÃ HẾT HẠN', REVOKED: 'ĐÃ THU HỒI' }[props.status] ?? props.status))
</script>

<template>
  <article class="library-card-preview" :class="`card-${status.toLowerCase()}`" aria-label="Thẻ thư viện điện tử">
    <header class="card-banner">
      <span class="school-emblem" aria-hidden="true"><i class="pi pi-book" /></span>
      <div><small>SỞ GIÁO DỤC VÀ ĐÀO TẠO TP HỒ CHÍ MINH</small><strong>TRƯỜNG TRUNG HỌC CƠ SỞ</strong></div>
    </header>
    <div class="card-body">
      <div class="card-identity">
        <div class="qr-frame">
          <img v-if="qrUrl" :src="qrUrl" alt="Mã QR thẻ thư viện">
          <span v-else class="qr-placeholder"><i :class="downloadingQr ? 'pi pi-spin pi-spinner' : 'pi pi-qrcode'" aria-hidden="true" />{{ downloadingQr ? 'Đang tải mã QR' : 'Mã QR được tải bảo mật' }}</span>
        </div>
        <small>MÃ THẺ</small><strong class="card-number">{{ cardNo }}</strong>
      </div>
      <div class="card-details">
        <h2>THẺ THƯ VIỆN</h2>
        <dl>
          <div><dt><i class="pi pi-user" aria-hidden="true" /> Họ và tên</dt><dd>{{ displayName }}</dd></div>
          <div><dt><i class="pi pi-id-card" aria-hidden="true" /> Mã độc giả</dt><dd>{{ patronId }}</dd></div>
          <div><dt><i class="pi pi-calendar" aria-hidden="true" /> Hiệu lực đến</dt><dd>{{ formatLibraryDate(expiresAt) }}</dd></div>
        </dl>
        <span class="card-status">{{ statusLabel }}</span>
      </div>
    </div>
    <span class="lotus-watermark" aria-hidden="true">✿</span>
  </article>
</template>

<style scoped>
.library-card-preview { position: relative; isolation: isolate; overflow: hidden; width: min(100%, 34rem); aspect-ratio: 1.586 / 1; border: 1px solid #cbd5e1; border-radius: 16px; background: linear-gradient(135deg, #fff 0%, #fbfdff 74%, #eff6ff 100%); box-shadow: 0 14px 34px rgb(15 23 42 / 12%); color: #0f172a; }
.card-banner { display: flex; align-items: center; gap: .8rem; min-height: 18%; padding: .8rem 1.2rem; color: white; background: linear-gradient(110deg, #1a56db, #1e40af); }
.card-banner div { display: grid; gap: .2rem; }
.card-banner small { font-size: clamp(.48rem, 1.6vw, .65rem); letter-spacing: .055em; }
.card-banner strong { font-size: clamp(.55rem, 1.8vw, .78rem); letter-spacing: .025em; }
.school-emblem { display: grid; place-items: center; width: 2.5rem; height: 2.5rem; flex: 0 0 auto; border: 2px solid rgb(255 255 255 / 85%); border-radius: 50%; font-size: 1.2rem; }
.card-body { display: grid; grid-template-columns: 35% 1fr; gap: 1.3rem; height: 82%; padding: 1rem 1.4rem 1.25rem; }
.card-identity { display: flex; flex-direction: column; align-items: center; justify-content: center; min-width: 0; }
.qr-frame { display: grid; place-items: center; width: min(100%, 8.7rem); aspect-ratio: 1; border: 2px solid #2563eb; border-radius: 9px; background: white; padding: .32rem; }
.qr-frame img { width: 100%; height: 100%; object-fit: contain; image-rendering: pixelated; }
.qr-placeholder { display: grid; gap: .45rem; justify-items: center; color: #64748b; text-align: center; font-size: .68rem; }
.qr-placeholder i { color: #2563eb; font-size: 2.4rem; }
.card-identity > small { margin-top: .55rem; color: #64748b; font-size: .58rem; letter-spacing: .12em; }
.card-number { max-width: 100%; overflow: hidden; color: #0f172a; font-family: ui-monospace, monospace; font-size: clamp(.65rem, 2vw, .92rem); text-overflow: ellipsis; }
.card-details { min-width: 0; padding-top: .1rem; }
.card-details h2 { margin: 0 0 .75rem; color: #047857; font-size: clamp(.9rem, 2.7vw, 1.25rem); letter-spacing: .1em; }
.card-details dl { display: grid; gap: .52rem; margin: 0; }
.card-details dl > div { min-width: 0; }
.card-details dt { display: flex; align-items: center; gap: .38rem; color: #64748b; font-size: clamp(.55rem, 1.6vw, .68rem); }
.card-details dt i { color: #2563eb; }
.card-details dd { margin: .08rem 0 0 1.3rem; overflow: hidden; font-size: clamp(.65rem, 1.9vw, .83rem); font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.card-status { display: inline-flex; margin-top: .65rem; border-radius: 999px; padding: .2rem .55rem; background: #d1fae5; color: #047857; font-size: .58rem; font-weight: 800; letter-spacing: .06em; }
.card-expired .card-status, .card-revoked .card-status { background: #fee2e2; color: #b91c1c; }
.lotus-watermark { position: absolute; z-index: -1; right: -.35rem; bottom: -.8rem; color: rgb(56 189 248 / 16%); font-size: 8rem; transform: rotate(-20deg); }
@media print { .library-card-preview { width: 85.6mm; height: 54mm; break-inside: avoid; border-radius: 3.18mm; box-shadow: none; } }
</style>
