import { shallowMount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import SkillWeightPanel from './SkillWeightPanel.vue'

describe('SkillWeightPanel', () => {
  it('validates total and final-exam dominance', () => {
    const wrapper = shallowMount(SkillWeightPanel, { props: { config: null } })
    const view = wrapper.vm as unknown as {
      kttt: number | null
      ktdk: number | null
      ktck: number | null
      save: () => void
      validationMessage: string
    }
    view.kttt = 40
    view.ktdk = 40
    view.ktck = 20
    view.save()
    expect(view.validationMessage).toContain('KTCK')
    expect(wrapper.emitted('save')).toBeUndefined()
  })

  it('emits a valid skill-weight request', () => {
    const wrapper = shallowMount(SkillWeightPanel, { props: { config: null } })
    const view = wrapper.vm as unknown as {
      kttt: number | null
      ktdk: number | null
      ktck: number | null
      save: () => void
    }
    view.kttt = 20
    view.ktdk = 30
    view.ktck = 50
    view.save()

    expect(wrapper.emitted('save')?.[0]).toEqual([{
      ktttWeightPercent: 20,
      ktdkWeightPercent: 30,
      ktckWeightPercent: 50,
    }])
  })
})

