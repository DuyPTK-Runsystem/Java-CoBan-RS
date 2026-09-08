import { describe, expect, it } from 'vitest'
import TeacherListView from './TeacherListView.vue'

describe('TeacherListView', () => {
  it('exports the teacher profile route view', () => {
    expect(TeacherListView).toBeTruthy()
  })
})
