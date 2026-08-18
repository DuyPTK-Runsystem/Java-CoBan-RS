---
name: Academic Core
colors:
  surface: '#f7f9fb'
  surface-dim: '#d8dadc'
  surface-bright: '#f7f9fb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f6'
  surface-container: '#eceef0'
  surface-container-high: '#e6e8ea'
  surface-container-highest: '#e0e3e5'
  on-surface: '#191c1e'
  on-surface-variant: '#464555'
  inverse-surface: '#2d3133'
  inverse-on-surface: '#eff1f3'
  outline: '#777587'
  outline-variant: '#c7c4d8'
  surface-tint: '#4d44e4'
  primary: '#3525ce'
  on-primary: '#ffffff'
  primary-container: '#4f46e6'
  on-primary-container: '#dbd8ff'
  inverse-primary: '#c3c0ff'
  secondary: '#505f76'
  on-secondary: '#ffffff'
  secondary-container: '#d0e1fb'
  on-secondary-container: '#54647a'
  tertiary: '#7e3000'
  on-tertiary: '#ffffff'
  tertiary-container: '#a54100'
  on-tertiary-container: '#ffd2c0'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e2dfff'
  primary-fixed-dim: '#c3c0ff'
  on-primary-fixed: '#0f0069'
  on-primary-fixed-variant: '#3322cd'
  secondary-fixed: '#d3e4fe'
  secondary-fixed-dim: '#b7c8e1'
  on-secondary-fixed: '#0b1c30'
  on-secondary-fixed-variant: '#38485d'
  tertiary-fixed: '#ffdbcc'
  tertiary-fixed-dim: '#ffb695'
  on-tertiary-fixed: '#351000'
  on-tertiary-fixed-variant: '#7b2f00'
  background: '#f7f9fb'
  on-background: '#191c1e'
  surface-variant: '#e0e3e5'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  container-max: 1280px
  sidebar-width: 260px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 32px
---

## Brand & Style

This design system is built for a high-utility Student Management SaaS, focusing on clarity, efficiency, and a sense of institutional reliability. The aesthetic follows a **Modern Corporate** approach, characterized by a refined use of whitespace, a constrained color palette, and high-legibility typography. 

The goal is to reduce the cognitive load for administrators and educators. The interface employs a "Content-First" philosophy where the UI chrome recedes, allowing data tables, student profiles, and scheduling forms to take priority. Visual interest is generated through precise alignment and purposeful use of a singular primary accent color to guide user intent.

## Colors

The palette is anchored by a deep **Indigo (#4F46E6)** primary color, chosen for its associations with professionalism and focus. 

- **Backgrounds:** Use the light neutral (#F8FAFC) for global application backgrounds to provide a soft contrast against white surfaces.
- **Surfaces:** Use pure White (#FFFFFF) for cards, modals, and navigation elements.
- **Accents:** Secondary colors are derived from the Slate scale (#64748B) to handle de-emphasized text and icons.
- **Status:** Use standard semantic colors (Success: Emerald, Warning: Amber, Error: Rose) sparingly to indicate student performance or system alerts.

## Typography

The typography system utilizes **Inter** for its exceptional legibility in data-heavy environments. 

- **Hierarchy:** Use `display-lg` for dashboard overviews and `headline-md` for page titles.
- **Data Tables:** Use `body-sm` for table row content to maximize information density without sacrificing readability.
- **Labels:** Form labels and table headers should use `label-md` and `label-sm` respectively, with the latter utilizing an uppercase treatment for structural distinction.

## Layout & Spacing

The design system employs a **Fixed-Fluid Hybrid Grid**. 

- **Authenticated State:** A persistent left-hand sidebar (260px) contains primary navigation. The main content area uses a fluid width with a maximum cap of 1280px to prevent excessive line lengths in data tables.
- **Authentication State:** Centered card layouts with a maximum width of 480px are used for Login/Signup to maintain focus.
- **Spacing Rhythm:** Based on a 4px baseline. Standard component spacing is 16px (4 units), while section spacing is 32px (8 units).
- **Responsive Behavior:** On mobile, the sidebar collapses into a bottom-sheet or hamburger menu, and horizontal margins shrink to 16px.

## Elevation & Depth

Depth is conveyed through **Tonal Layering** and **Soft Shadows**. 

1. **Level 0 (Background):** #F8FAFC - The base canvas.
2. **Level 1 (Surfaces):** #FFFFFF - Cards and sidebars. Use a subtle 1px border (#E2E8F0) to define edges.
3. **Level 2 (Interactive):** Apply a soft, diffused shadow (0px 4px 6px -1px rgba(0, 0, 0, 0.1)) for dropdowns, modals, and hovered states of cards.

Avoid heavy blurs or glassmorphism to maintain the professional, utilitarian nature of the tool.

## Shapes

The design system uses a **Rounded (Level 2)** shape language. 

- **Standard Elements:** Buttons, Input fields, and Checkboxes utilize a 0.5rem (8px) radius.
- **Large Elements:** Main content cards and Modals utilize a 1rem (16px) radius to soften the large surface areas.
- **Small Elements:** Tooltips and tags use a 0.25rem (4px) radius.

## Components

### Buttons
- **Primary:** Solid #4F46E6 background with white text. 8px corner radius.
- **Secondary:** White background with #E2E8F0 border and #1E293B text.
- **Size:** 40px height for standard actions; 32px for table-row actions.

### Input Fields
- **Style:** 1px #E2E8F0 border, 8px radius, White background. 
- **Focus State:** 2px solid #4F46E6 with a subtle outer glow.
- **Validation:** Clear 14px error text below the field in Rose-600.

### Cards
- **Style:** White background, 1px border, 16px padding.
- **Header:** Integrated headline-sm with a subtle bottom divider.

### Data Tables (PrimeVue Inspired)
- **Header:** Light gray background (#F1F5F9), semi-bold text.
- **Rows:** 52px minimum height, subtle hover state (#F8FAFC).
- **Pagination:** Clean, centered controls with active page highlighted in primary indigo.

### Authenticated Header
- **Structure:** 64px height, White surface, 1px bottom border.
- **Contents:** Left-aligned brand logo; Right-aligned flex container with "Welcome, [Name]" text and a secondary-style "Logout" button.