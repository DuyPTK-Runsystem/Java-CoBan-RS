import { defineComponent, h, type ComponentObjectPropsOptions, type PropType } from 'vue'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import TimetableAgentWorkspace from './TimetableAgentWorkspace.vue'
import { ApiError } from '@/types/api'
import type { TimetableAgentProposal, TimetableAgentReceipt, TimetableAgentRequest } from '@/types/timetableAgent'

const mocks = vi.hoisted(() => ({
  requireAccessToken: vi.fn(),
  getAuthSession: vi.fn(),
  fetchAssignments: vi.fn(),
  createProposal: vi.fn(),
  approveProposal: vi.fn(),
  executeProposal: vi.fn(),
  recoverByKey: vi.fn(),
  getActivePolicy: vi.fn(),
  confirmPolicy: vi.fn(),
  randomUUID: vi.fn(),
}))

vi.mock('@/composables/useAuthSession', () => ({
  useAuthSession: () => ({ requireAccessToken: mocks.requireAccessToken }),
}))
vi.mock('@/services/authSession', () => ({ getAuthSession: mocks.getAuthSession }))
vi.mock('@/services/assignmentApi', () => ({ fetchSubjectAssignmentsByClass: mocks.fetchAssignments }))
vi.mock('@/services/teacherLoadApi', () => ({ getActiveTeacherLoadPolicy: mocks.getActivePolicy }))
vi.mock('@/services/timetableApi', () => ({ confirmTimetableTeacherLoadPolicy: mocks.confirmPolicy }))
vi.mock('@/services/timetableAgentApi', () => ({
  createTimetableAgentProposal: mocks.createProposal,
  approveTimetableAgentProposal: mocks.approveProposal,
  executeTimetableAgentProposal: mocks.executeProposal,
  getTimetableAgentActionByKey: mocks.recoverByKey,
}))

const request: TimetableAgentRequest = {
  targetRevisionId: 42,
  expectedVersion: 7,
  classIds: [11],
  validFrom: '2026-10-05',
  validTo: '2026-12-31',
  demands: [{ assignmentId: 501, periodsPerWeek: 4 }],
  lockedEntryIds: [],
  preferences: '',
  userRequest: 'Test request',
}

function proposal(overrides: Partial<TimetableAgentProposal> = {}): TimetableAgentProposal {
  return {
    proposalId: 'proposal-1',
    proposalVersion: 2,
    proposalHash: 'hash-2',
    targetRevisionId: 42,
    expectedVersion: 7,
    status: 'READY_FOR_REVIEW',
    snapshotId: 'snapshot-1',
    expiresAt: '2099-12-31T12:00:00Z',
    entries: [],
    issues: [],
    explanation: 'Proposal',
    diff: { added: [], removed: [], unchanged: [] },
    capabilities: { canGenerate: true, canApprove: true, canExecute: false },
    ...overrides,
  }
}

const receipt: TimetableAgentReceipt = {
  actionId: 'action-1',
  proposalId: 'proposal-1',
  targetRevisionId: 42,
  newVersion: 8,
  savedEntryCount: 2,
  committedAt: '2026-10-01T10:00:00Z',
  status: 'SAVED_DRAFT',
}

const childProps: ComponentObjectPropsOptions = {
  targetRevisionId: { type: Number, default: 0 },
  expectedVersion: { type: Number, default: 0 },
  classes: { type: Array as PropType<{ id: number; name: string }[]>, default: () => [] },
  canGenerate: { type: Boolean, default: false },
  canSave: { type: Boolean, default: false },
  canRetryPending: { type: Boolean, default: false },
  busy: { type: Boolean, default: false },
  assignmentsLoading: { type: Boolean, default: false },
  pendingRecovery: { type: Boolean, default: false },
  proposal: { type: null as unknown as PropType<TimetableAgentProposal | null>, default: null },
  phase: { type: String, default: 'idle' },
  receipt: { type: null as unknown as PropType<TimetableAgentReceipt | null>, default: null },
  error: { type: String, default: '' },
}

type StubAction = { id: string; label: string; event: string; payload?: unknown }
function createActionStub(name: string, actions: StubAction[], emitted: string[]) {
  return defineComponent({
    name,
    inheritAttrs: false,
    props: childProps,
    emits: emitted,
    setup(_, { emit }) {
      return () => actions.map((action) => h('button', {
        id: action.id,
        onClick: () => emit(action.event, ...(action.payload === undefined ? [] : [action.payload])),
      }, action.label))
    },
  })
}

const PanelStub = createActionStub('TimetableAgentPanelStub', [
  { id: 'generate', label: 'generate', event: 'generate', payload: request },
  { id: 'edit', label: 'edit', event: 'inputChanged' },
], ['generate', 'inputChanged', 'classesChanged'])

const ReviewStub = createActionStub('TimetableAgentReviewStub', [
  { id: 'save', label: 'save', event: 'save' },
  { id: 'recover', label: 'recover', event: 'recover' },
  { id: 'retry', label: 'retry', event: 'retryPending' },
], ['save', 'recover', 'retryPending', 'reload'])

const detail = {
  timetableId: 1,
  semesterId: 5,
  semesterName: 'HK1',
  revisionId: 42,
  revisionNumber: 1,
  status: 'DRAFT' as const,
  effectiveFrom: '2026-09-01',
  effectiveTo: '2026-12-31',
  version: 7,
  headVersion: 1,
  blockingCount: 0,
  warningCount: 0,
  canUseTimetableAgent: true,
  policyId: 20,
  policyVersion: '2026-27',
  capabilities: { canEdit: true, canValidate: true, canPublish: false, canRevise: false },
}

const activePolicy = {
  id: 20,
  policyName: '2026-27',
  sourceDocument: 'Decision 01',
  effectiveFrom: '2026-09-01',
  effectiveTo: null,
  policyVersion: 1,
  standardPeriodsHighSchool: 19,
  homeroomReduction: 4,
  nursingChildReduction: 3,
  active: true,
  version: 1,
  rules: [],
}

let wrapper: VueWrapper | undefined

async function mountWorkspace(detailOverride = detail) {
  wrapper = mount(TimetableAgentWorkspace, {
    props: { detail: detailOverride, classes: [], teachers: [], periods: [], entries: [] },
    global: { stubs: { TimetableAgentPanel: PanelStub, TimetableAgentReview: ReviewStub } },
  })
  await flushPromises()
  return wrapper
}

async function generateAndSave(target?: VueWrapper) {
  const workspace = target ?? await mountWorkspace()
  await workspace.get('#generate').trigger('click')
  await flushPromises()
  await workspace.get('#save').trigger('click')
  await flushPromises()
}

describe('TimetableAgentWorkspace', () => {
  beforeEach(() => {
    sessionStorage.clear()
    mocks.requireAccessToken.mockReset().mockReturnValue('session-token')
    mocks.getAuthSession.mockReset().mockReturnValue({ user: { id: 7 } })
    mocks.fetchAssignments.mockReset().mockResolvedValue([])
    mocks.getActivePolicy.mockReset().mockResolvedValue(activePolicy)
    mocks.confirmPolicy.mockReset().mockResolvedValue({ ...detail, version: 8 })
    mocks.createProposal.mockReset().mockResolvedValue(proposal())
    mocks.approveProposal.mockReset().mockImplementation(async (value: TimetableAgentProposal) => proposal({ ...value, status: 'APPROVED', capabilities: { canGenerate: true, canApprove: false, canExecute: true } }))
    mocks.executeProposal.mockReset().mockResolvedValue(receipt)
    mocks.recoverByKey.mockReset()
    mocks.randomUUID.mockReset().mockReturnValue('stable-action-key')
    vi.stubGlobal('crypto', { randomUUID: mocks.randomUUID })
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
    vi.unstubAllGlobals()
  })

  it('maps API class codes and names to visible agent options', () => {
    const target = mount(TimetableAgentWorkspace, {
      props: {
        detail,
        classes: [
          { id: 11, academicYearId: 3, gradeLevelId: 10, classCode: '10A1', className: 'Lớp 10A1', capacity: 40, status: 'ACTIVE' },
          { id: 12, academicYearId: 3, gradeLevelId: 10, classCode: '10A2', className: null, capacity: 40, status: 'ACTIVE' },
        ],
        teachers: [], periods: [], entries: [],
      },
      global: { stubs: { TimetableAgentPanel: PanelStub, TimetableAgentReview: ReviewStub } },
    })

    expect(target.findComponent(PanelStub).props('classes')).toEqual([
      { id: 11, name: '10A1 · Lớp 10A1' },
      { id: 12, name: '10A2' },
    ])
    target.unmount()
  })

  it('clears approval when request inputs change and disables a stale proposal', async () => {
    const target = await mountWorkspace()
    await target.get('#generate').trigger('click')
    await flushPromises()
    await target.get('#save').trigger('click')
    await flushPromises()
    expect(mocks.approveProposal).toHaveBeenCalledTimes(1)
    expect(mocks.executeProposal).toHaveBeenCalledTimes(1)

    await target.get('#edit').trigger('click')
    expect(target.findComponent(ReviewStub).props('proposal')).toBeNull()
    expect(target.findComponent(ReviewStub).props('canSave')).toBe(false)

    await target.get('#generate').trigger('click')
    await flushPromises()
    await target.setProps({ detail: { ...detail, version: 8 } })
    expect(target.findComponent(ReviewStub).props('canSave')).toBe(false)
  })

  it('rejects an approval response whose immutable proposal binding changed', async () => {
    mocks.approveProposal.mockImplementationOnce(async (value: TimetableAgentProposal) => proposal({
      ...value,
      proposalHash: 'different-hash',
      status: 'APPROVED',
      capabilities: { canGenerate: true, canApprove: false, canExecute: true },
    }))
    const target = await mountWorkspace()
    await target.get('#generate').trigger('click')
    await flushPromises()
    await target.get('#save').trigger('click')
    await flushPromises()

    expect(target.findComponent(ReviewStub).props('canSave')).toBe(false)
    expect(target.findComponent(ReviewStub).props('error')).toContain('không khớp')
    expect(mocks.executeProposal).not.toHaveBeenCalled()
  })

  it('clears the pending key for a definitive stale response', async () => {
    mocks.executeProposal.mockRejectedValueOnce(new ApiError(409, 'Timetable revision is stale.'))
    const target = await mountWorkspace()
    await generateAndSave(target)
    await flushPromises()

    expect(sessionStorage.getItem('timetable-agent.pending.7.42')).toBeNull()
    expect(target.findComponent(ReviewStub).props('pendingRecovery')).toBe(false)
    expect(target.findComponent(ReviewStub).props('phase')).toBe('stale')
    expect(mocks.recoverByKey).not.toHaveBeenCalled()
  })

  it('retries the exact pending action only after its lease expires', async () => {
    mocks.executeProposal.mockRejectedValueOnce(new Error('connection lost')).mockResolvedValueOnce(receipt)
    const target = await mountWorkspace()
    await generateAndSave(target)
    await flushPromises()
    const pending = JSON.parse(sessionStorage.getItem('timetable-agent.pending.7.42') ?? 'null') as {
      idempotencyKey: string
      proposalId: string
      proposalVersion: number
    }

    mocks.recoverByKey.mockResolvedValueOnce({
      actionId: 'action-pending',
      proposalId: pending.proposalId,
      targetRevisionId: 42,
      status: 'PENDING',
      leaseExpiresAt: new Date(Date.now() + 60_000).toISOString(),
    })
    await target.get('#recover').trigger('click')
    await flushPromises()
    expect(target.findComponent(ReviewStub).props('canRetryPending')).toBe(false)

    mocks.recoverByKey.mockResolvedValueOnce({
      actionId: 'action-pending',
      proposalId: pending.proposalId,
      targetRevisionId: 42,
      status: 'PENDING',
      leaseExpiresAt: new Date(Date.now() - 1_000).toISOString(),
    })
    await target.get('#recover').trigger('click')
    await flushPromises()
    expect(target.findComponent(ReviewStub).props('canRetryPending')).toBe(true)

    await target.get('#retry').trigger('click')
    await flushPromises()

    expect(mocks.executeProposal).toHaveBeenCalledTimes(2)
    expect(mocks.executeProposal).toHaveBeenNthCalledWith(1, 'proposal-1', 2, 'stable-action-key', 'session-token')
    expect(mocks.executeProposal).toHaveBeenNthCalledWith(2, 'proposal-1', 2, 'stable-action-key', 'session-token')
    expect(mocks.randomUUID).toHaveBeenCalledTimes(1)
    expect(sessionStorage.getItem('timetable-agent.pending.7.42')).toBeNull()
    expect(target.findComponent(ReviewStub).props('receipt')).toEqual(receipt)
  })

  it('retains the action key across reload and recovers by key without executing twice', async () => {
    mocks.executeProposal.mockRejectedValueOnce(new Error('connection lost'))
    const first = await mountWorkspace()
    await generateAndSave(first)
    await flushPromises()

    const key = 'timetable-agent.pending.7.42'
    const pending = JSON.parse(sessionStorage.getItem(key) ?? 'null') as { idempotencyKey: string }
    expect(pending.idempotencyKey).toBe('stable-action-key')
    expect(first.findComponent(ReviewStub).props('phase')).toBe('response-lost')
    expect(first.findComponent(ReviewStub).props('canSave')).toBe(false)
    expect(mocks.executeProposal).toHaveBeenCalledTimes(1)

    first.unmount()
    wrapper = undefined
    mocks.recoverByKey.mockResolvedValueOnce(receipt)
    const restored = await mountWorkspace()
    expect(restored.findComponent(ReviewStub).props('pendingRecovery')).toBe(true)
    expect(restored.findComponent(ReviewStub).props('phase')).toBe('response-lost')
    await restored.get('#recover').trigger('click')
    await flushPromises()

    expect(mocks.recoverByKey).toHaveBeenCalledWith('stable-action-key', 'session-token')
    expect(mocks.executeProposal).toHaveBeenCalledTimes(1)
    expect(sessionStorage.getItem(key)).toBeNull()
    expect(restored.findComponent(ReviewStub).props('receipt')).toEqual(receipt)
    expect(restored.emitted('saved')).toEqual([[receipt]])
  })

  it('requires explicit current-policy confirmation before generating', async () => {
    const target = await mountWorkspace({ ...detail, policyId: null, policyVersion: null })

    expect(target.findComponent(PanelStub).props('canGenerate')).toBe(false)
    expect(target.findAll('button').some((button) => button.text().includes('Xác nhận chính sách'))).toBe(true)
    await target.findAll('button').find((button) => button.text().includes('Xác nhận chính sách'))!.trigger('click')
    await flushPromises()

    expect(mocks.confirmPolicy).toHaveBeenCalledWith(42, 20, 7, 'session-token')
    expect(target.emitted('reload')).toHaveLength(1)
    expect(target.findComponent(PanelStub).props('canGenerate')).toBe(false)

    await target.setProps({ detail: { ...detail, policyId: 20, policyVersion: '2026-27', version: 8 } })
    expect(target.findComponent(PanelStub).props('canGenerate')).toBe(true)
    expect(target.findComponent(PanelStub).props('expectedVersion')).toBe(8)
  })

  it('keeps generation disabled when there is no active policy', async () => {
    mocks.getActivePolicy.mockResolvedValueOnce(null)
    const target = await mountWorkspace({ ...detail, policyId: null, policyVersion: null })

    expect(target.findComponent(PanelStub).props('canGenerate')).toBe(false)
    expect(target.text()).toContain('Chưa có chính sách định mức tiết dạy đang hoạt động')
    expect(mocks.createProposal).not.toHaveBeenCalled()
  })

  it('refreshes the canonical policy before generating and blocks when it changed', async () => {
    const replacement = { ...activePolicy, id: 21, policyName: '2027-28' }
    mocks.getActivePolicy.mockReset().mockResolvedValueOnce(activePolicy).mockResolvedValueOnce(replacement)
    const target = await mountWorkspace()

    await target.get('#generate').trigger('click')
    await flushPromises()

    expect(mocks.createProposal).not.toHaveBeenCalled()
    expect(target.findComponent(PanelStub).props('canGenerate')).toBe(false)
    expect(target.text()).toContain('Chính sách hiện hành đã thay đổi')
  })
})
