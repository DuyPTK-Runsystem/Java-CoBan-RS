<script setup lang="ts">
import { reactive, watch } from 'vue'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'

import FormAlert from '@/components/common/FormAlert.vue'
import type { BookDetail, BookFormValues } from '@/types/library/catalog'

const props = withDefaults(defineProps<{
  initialValue?: BookDetail | null
  saving?: boolean
  errorMessage?: string
  errorTone?: 'error' | 'warning'
  fieldErrors?: Record<string, string>
}>(), { initialValue: null, saving: false, errorMessage: '', errorTone: 'error', fieldErrors: () => ({}) })
const emit = defineEmits<{ save: [values: BookFormValues]; cancel: [] }>()

const values = reactive<BookFormValues>(emptyValues())
const errors = reactive<Record<string, string>>({})

function emptyValues(): BookFormValues {
  return { isbn: '', title: '', author: '', publisher: '', publishedYear: null, category: '', listPrice: null, coverUrl: '' }
}

function syncValues(): void {
  Object.assign(values, {
    isbn: props.initialValue?.isbn ?? '',
    title: props.initialValue?.title ?? '',
    author: props.initialValue?.author ?? '',
    publisher: props.initialValue?.publisher ?? '',
    publishedYear: props.initialValue?.publishedYear ?? null,
    category: props.initialValue?.category ?? '',
    listPrice: props.initialValue?.listPrice ?? null,
    coverUrl: props.initialValue?.coverUrl ?? '',
  })
  Object.keys(errors).forEach((key) => delete errors[key])
}

watch(() => props.initialValue, syncValues, { immediate: true })

function validate(): boolean {
  Object.keys(errors).forEach((key) => delete errors[key])
  if (!values.title.trim()) errors.title = 'Tên sách là bắt buộc.'
  else if (values.title.trim().length > 200) errors.title = 'Tên sách tối đa 200 ký tự.'
  if (!values.author.trim()) errors.author = 'Tác giả là bắt buộc.'
  else if (values.author.trim().length > 200) errors.author = 'Tác giả tối đa 200 ký tự.'
  if (values.publisher.trim().length > 200) errors.publisher = 'Nhà xuất bản tối đa 200 ký tự.'
  if (values.category.trim().length > 100) errors.category = 'Thể loại tối đa 100 ký tự.'
  if (values.publishedYear !== null && (!Number.isInteger(values.publishedYear) || values.publishedYear < 1 || values.publishedYear > 9999)) {
    errors.publishedYear = 'Năm xuất bản phải từ 1 đến 9999.'
  }
  if (values.listPrice !== null && (values.listPrice < 0 || Math.abs(values.listPrice * 100 - Math.round(values.listPrice * 100)) > 0.0000001 || values.listPrice > 9999999999.99)) {
    errors.listPrice = 'Giá bìa phải không âm và có tối đa 2 chữ số thập phân.'
  }
  if (values.coverUrl.trim()) {
    try {
      const url = new URL(values.coverUrl.trim())
      if (!['http:', 'https:'].includes(url.protocol)) throw new Error('unsupported protocol')
    } catch {
      errors.coverUrl = 'Địa chỉ ảnh bìa phải là URL HTTP hoặc HTTPS hợp lệ.'
    }
    if (values.coverUrl.trim().length > 2048) errors.coverUrl = 'Địa chỉ ảnh bìa tối đa 2048 ký tự.'
  }
  if (values.isbn.trim().length > 20) errors.isbn = 'ISBN tối đa 20 ký tự.'
  return Object.keys(errors).length === 0
}

function save(): void {
  if (!validate()) return
  emit('save', {
    isbn: values.isbn.trim(),
    title: values.title.trim(),
    author: values.author.trim(),
    publisher: values.publisher.trim(),
    publishedYear: values.publishedYear,
    category: values.category.trim(),
    listPrice: values.listPrice,
    coverUrl: values.coverUrl.trim(),
  })
}
</script>

<template>
  <form class="form-stack" novalidate @submit.prevent="save">
    <FormAlert v-if="props.errorMessage" :tone="props.errorTone" :message="props.errorMessage" />
    <div class="catalog-form-grid">
      <div class="field-group"><label for="book-title">Tên sách <span class="required-mark" aria-hidden="true">*</span></label><InputText id="book-title" v-model="values.title" maxlength="200" fluid aria-required="true" :invalid="Boolean(errors.title || props.fieldErrors.title)" /><small v-if="errors.title || props.fieldErrors.title" class="field-error">{{ errors.title || props.fieldErrors.title }}</small></div>
      <div class="field-group"><label for="book-author">Tác giả <span class="required-mark" aria-hidden="true">*</span></label><InputText id="book-author" v-model="values.author" maxlength="200" fluid aria-required="true" :invalid="Boolean(errors.author || props.fieldErrors.author)" /><small v-if="errors.author || props.fieldErrors.author" class="field-error">{{ errors.author || props.fieldErrors.author }}</small></div>
      <div class="field-group"><label for="book-isbn">ISBN</label><InputText id="book-isbn" v-model="values.isbn" maxlength="20" fluid :invalid="Boolean(errors.isbn || props.fieldErrors.isbn)" /><small v-if="errors.isbn || props.fieldErrors.isbn" class="field-error">{{ errors.isbn || props.fieldErrors.isbn }}</small></div>
      <div class="field-group"><label for="book-publisher">Nhà xuất bản</label><InputText id="book-publisher" v-model="values.publisher" maxlength="200" fluid :invalid="Boolean(errors.publisher || props.fieldErrors.publisher)" /><small v-if="errors.publisher || props.fieldErrors.publisher" class="field-error">{{ errors.publisher || props.fieldErrors.publisher }}</small></div>
      <div class="field-group"><label for="book-year">Năm xuất bản</label><InputNumber id="book-year" v-model="values.publishedYear" :min="1" :max="9999" :use-grouping="false" fluid :invalid="Boolean(errors.publishedYear || props.fieldErrors.publishedYear)" /><small v-if="errors.publishedYear || props.fieldErrors.publishedYear" class="field-error">{{ errors.publishedYear || props.fieldErrors.publishedYear }}</small></div>
      <div class="field-group"><label for="book-category">Thể loại</label><InputText id="book-category" v-model="values.category" maxlength="100" fluid :invalid="Boolean(errors.category || props.fieldErrors.category)" /><small v-if="errors.category || props.fieldErrors.category" class="field-error">{{ errors.category || props.fieldErrors.category }}</small></div>
      <div class="field-group"><label for="book-price">Giá bìa</label><InputNumber id="book-price" v-model="values.listPrice" :min="0" :max="9999999999.99" :min-fraction-digits="0" :max-fraction-digits="2" mode="currency" currency="VND" locale="vi-VN" fluid :invalid="Boolean(errors.listPrice || props.fieldErrors.listPrice)" /><small v-if="errors.listPrice || props.fieldErrors.listPrice" class="field-error">{{ errors.listPrice || props.fieldErrors.listPrice }}</small></div>
      <div class="field-group"><label for="book-cover">URL ảnh bìa</label><InputText id="book-cover" v-model="values.coverUrl" maxlength="2048" type="url" fluid :invalid="Boolean(errors.coverUrl || props.fieldErrors.coverUrl)" /><small v-if="errors.coverUrl || props.fieldErrors.coverUrl" class="field-error">{{ errors.coverUrl || props.fieldErrors.coverUrl }}</small></div>
    </div>
    <div class="form-actions"><Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving" @click="emit('cancel')" /><Button type="submit" :label="props.initialValue ? 'Lưu thay đổi' : 'Tạo đầu sách'" icon="pi pi-check" :loading="props.saving" :disabled="props.saving" /></div>
  </form>
</template>
