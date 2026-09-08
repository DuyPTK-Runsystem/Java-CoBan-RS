import { ref } from 'vue'

export type TranscriptTab = 'term' | 'annual'

export function useTranscriptTabState(initial: TranscriptTab = 'term') {
  const activeTab = ref<TranscriptTab>(initial)

  function selectTab(tab: TranscriptTab): void {
    activeTab.value = tab
  }

  return { activeTab, selectTab }
}
