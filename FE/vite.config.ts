import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
      coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'json-summary'],
      include: [
        'src/components/{LoginForm,RegisterForm,StudentSearchForm}.vue',
        'src/components/{EmptyState,FormAlert,StatusTag,ServerPagination,ConfirmAction,PageState}.vue',
        'src/components/{AcademicYearDialog,AcademicYearTable,SemesterDialog,SemesterStatusDialog,SemesterTable}.vue',
        'src/services/{apiClient,authSession,userApi,studentApi,academicApi,scorebookApi}.ts',
        'src/utils/academicDate.ts',
        'src/types/{api,ui}.ts',
        'src/views/{LoginView,RegisterView,AcademicYearListView,SemesterListView,ScorebookWorkspaceView}.vue',
        'src/components/{ScorebookContextPanel,ScorebookStatusHeader,AssessmentColumnPanel,AssessmentColumnDialog,ScoreGrid,ScoreEntryDialog,BulkScoreEntryDialog,SkillWeightPanel}.vue',
      ],
    },
  },
})
