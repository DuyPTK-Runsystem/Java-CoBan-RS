<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'

import FormAlert from '@/components/common/FormAlert.vue'
import PageState from '@/components/common/PageState.vue'
import TimetableWeekGrid from '@/components/timetable/TimetableWeekGrid.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { getMyTimetable, getTimetablePeriods } from '@/services/timetableApi'
import { extractApiErrorMessage } from '@/types/api'
import type { TimetableEntry, TimetablePeriod } from '@/types/timetable'
import type { LoadingState } from '@/types/ui'

const router = useRouter()
const { requireAccessToken } = useAuthSession()

const periods = ref<TimetablePeriod[]>([])
const entries = ref<TimetableEntry[]>([])
const loadingState = ref<LoadingState>('loading')
const generalError = ref('')

async function loadData() {
  const token = requireAccessToken()
  if (!token) return
  loadingState.value = 'loading'
  generalError.value = ''
  try {
    const [periodList, entryList] = await Promise.all([
      getTimetablePeriods(undefined, token),
      getMyTimetable(token),
    ])
    periods.value = periodList
    entries.value = entryList
    loadingState.value = 'idle'
  } catch (err) {
    loadingState.value = 'error'
    generalError.value = extractApiErrorMessage(err, 'Không thể tải thời khóa biểu của bạn')
  }
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <div class="p-6 max-w-7xl mx-auto flex flex-col gap-6">
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Thời khóa biểu giảng dạy của tôi</h1>
        <p class="text-sm text-gray-500">Xem lịch dạy đã được công bố của bạn và quản lý lịch bận</p>
      </div>
      <Button
        label="Đăng ký lịch bận"
        icon="pi pi-calendar-times"
        severity="secondary"
        @click="router.push('/v2/my-timetable/unavailability')"
      />
    </div>

    <FormAlert v-if="generalError" :message="generalError" type="error" />

    <PageState
      v-if="loadingState === 'loading' || loadingState === 'error'"
      :state="loadingState"
      :message="generalError"
      @retry="loadData"
    />

    <div v-else class="flex flex-col gap-4">
      <div v-if="entries.length === 0" class="bg-white p-8 rounded-xl border border-gray-200 text-center text-gray-500">
        Bạn chưa có tiết dạy nào được phân công hoặc thời khóa biểu chưa được công bố.
      </div>

      <TimetableWeekGrid
        v-else
        :periods="periods"
        :entries="entries"
        :can-edit="false"
        view-mode="TEACHER"
      />
    </div>
  </div>
</template>

