import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AuthenticatedLayout from './AuthenticatedLayout.vue'
import ButtonStub from '@/test/stubs/ButtonStub.vue'

describe('AuthenticatedLayout', () => {
  it('keeps default navigation and accepts static module navigation', async () => {
    const wrapper = mount(AuthenticatedLayout, {
      props: { userName: 'academic.admin' },
      global: {
        stubs: {
          Button: ButtonStub,
          RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
        },
      },
      slots: { default: '<p>Page content</p>' },
    })
    expect(wrapper.text()).toContain('Students')
    expect(wrapper.text()).toContain('Add student')
    expect(wrapper.text()).toContain('Page content')

    await wrapper.setProps({ navigation: [{ label: 'Academic years', to: '/v2/academic-years', icon: 'pi pi-calendar' }] })
    expect(wrapper.text()).toContain('Academic years')
    expect(wrapper.text()).not.toContain('Add student')
  })

  it('emits logout from the shell action', async () => {
    const wrapper = mount(AuthenticatedLayout, {
      props: { userName: 'academic.admin' },
      global: {
        stubs: {
          Button: ButtonStub,
          RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
        },
      },
    })
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('logout')).toHaveLength(1)
  })
})
