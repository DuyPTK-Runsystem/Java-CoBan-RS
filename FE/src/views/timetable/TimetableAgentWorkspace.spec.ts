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
  randomUUID: vi.fn(),
}))

vi.mock('@/composables/useAuthSession', () => ({
  useAuthSession: () => ({ requireAccessToken: mocks.requireAccessToken }),
}))
vi.mock('@/services/authSession', () => ({ getAuthSession: mocks.getAuthSession }))
vi.mock('@/services/assignmentApi', () => ({ fetchSubjectAssignmentsByClass: mocks.fetchAssignments }))
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
  canGenerate: { type: Boolean, default: false },
  canApprove: { type: Boolean, default: false },
  canExecute: { type: Boolean, default: false },
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
  { id: 'approve', label: 'approve', event: 'approve' },
  { id: 'execute', label: 'execute', event: 'execute' },
  { id: 'recover', label: 'recover', event: 'recover' },
  { id: 'retry', label: 'retry', event: 'retryPending' },
], ['approve', 'execute', 'recover', 'retryPending', 'reload'])

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
  capabilities: { canEdit: true, canValidate: true, canPublish: false, canRevise: false },
}

let wrapper: VueWrapper | undefined

function mountWorkspace() {
  wrapper = mount(TimetableAgentWorkspace, {
    props: { detail, classes: [], teachers: [], periods: [], entries: [] },
    global: { stubs: { TimetableAgentPanel: PanelStub, TimetableAgentReview: ReviewStub } },
  })
  return wrapper
}

async function generateAndApprove(target = mountWorkspace()) {
  await target.get('#generate').trigger('click')
  await flushPromises()
  await target.get('#approve').trigger('click')
  await flushPromises()
}

describe('TimetableAgentWorkspace', () => {
  beforeEach(() => {
    sessionStorage.clear()
    mocks.requireAccessToken.mockReset().mockReturnValue('session-token')
    mocks.getAuthSession.mockReset().mockReturnValue({ user: { id: 7 } })
    mocks.fetchAssignments.mockReset().mockResolvedValue([])
    mocks.createProposal.mockReset().mockResolvedValue(proposal())
    mocks.approveProposal.mockReset().mockImplementation(async (value: TimetableAgentProposal) => proposal({ ...value, status: 'APPROVED', capabilities: { canGenerate: true, canApprove: false, canExecute: true } }))
    mocks.executeProposal.mockReset()
    mocks.recoverByKey.mockReset()
    mocks.randomUUID.mockReset().mockReturnValue('stable-action-key')
    vi.stubGlobal('crypto', { randomUUID: mocks.randomUUID })
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
    vi.unstubAllGlobals()
  })

  it('clears approval when request inputs change and disables a stale proposal', async () => {
    const target = mountWorkspace()
    await target.get('#generate').trigger('click')
    await flushPromises()
    await target.get('#approve').trigger('click')
    await flushPromises()
    expect(target.findComponent(ReviewStub).props('canExecute')).toBe(true)

    await target.get('#edit').trigger('click')
    expect(target.findComponent(ReviewStub).props('proposal')).toBeNull()
    expect(target.findComponent(ReviewStub).props('canExecute')).toBe(false)

    await target.get('#generate').trigger('click')
    await flushPromises()
    await target.setProps({ detail: { ...detail, version: 8 } })
    expect(target.findComponent(ReviewStub).props('canApprove')).toBe(false)
  })

  it('rejects an approval response whose immutable proposal binding changed', async () => {
    mocks.approveProposal.mockImplementationOnce(async (value: TimetableAgentProposal) => proposal({
      ...value,
      proposalHash: 'different-hash',
      status: 'APPROVED',
      capabilities: { canGenerate: true, canApprove: false, canExecute: true },
    }))
    const target = mountWorkspace()
    await target.get('#generate').trigger('click')
    await flushPromises()
    await target.get('#approve').trigger('click')
    await flushPromises()

    expect(target.findComponent(ReviewStub).props('canExecute')).toBe(false)
    expect(target.findComponent(ReviewStub).props('error')).toContain('không khớp')
    expect(mocks.executeProposal).not.toHaveBeenCalled()
  })

  it('clears the pending key for a definitive stale response', async () => {
    mocks.executeProposal.mockRejectedValueOnce(new ApiError(409, 'Timetable revision is stale.'))
    const target = mountWorkspace()
    await generateAndApprove(target)
    await target.get('#execute').trigger('click')
    await flushPromises()

    expect(sessionStorage.getItem('timetable-agent.pending.7.42')).toBeNull()
    expect(target.findComponent(ReviewStub).props('pendingRecovery')).toBe(false)
    expect(target.findComponent(ReviewStub).props('phase')).toBe('stale')
    expect(mocks.recoverByKey).not.toHaveBeenCalled()
  })

  it('retries the exact pending action only after its lease expires', async () => {
    mocks.executeProposal.mockRejectedValueOnce(new Error('connection lost')).mockResolvedValueOnce(receipt)
    const target = mountWorkspace()
    await generateAndApprove(target)
    await target.get('#execute').trigger('click')
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
    const first = mountWorkspace()
    await generateAndApprove(first)
    await first.get('#execute').trigger('click')
    await flushPromises()

    const key = 'timetable-agent.pending.7.42'
    const pending = JSON.parse(sessionStorage.getItem(key) ?? 'null') as { idempotencyKey: string }
    expect(pending.idempotencyKey).toBe('stable-action-key')
    expect(first.findComponent(ReviewStub).props('phase')).toBe('response-lost')
    expect(first.findComponent(ReviewStub).props('canExecute')).toBe(false)
    expect(mocks.executeProposal).toHaveBeenCalledTimes(1)

    first.unmount()
    wrapper = undefined
    mocks.recoverByKey.mockResolvedValueOnce(receipt)
    const restored = mountWorkspace()
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
})
