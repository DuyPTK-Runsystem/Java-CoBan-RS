import { describe, expect, it } from 'vitest'
import { timetableWorkspacePath } from './timetableWorkspacePath'

describe('timetable list workspace routing', () => {
  it('uses the selected revision id for the workspace route', () => {
    const rows = [
      { revisionId: 11, timetableId: 1 },
      { revisionId: 12, timetableId: 1 },
      { revisionId: 27, timetableId: 4 },
    ]

    expect(rows.map(({ revisionId }) => timetableWorkspacePath(revisionId))).toEqual([
      '/v2/timetables/11',
      '/v2/timetables/12',
      '/v2/timetables/27',
    ])
  })
})
