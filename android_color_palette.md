# SyncTax Android Color Palette & Tailwind CSS Configuration Guide

This file provides a detail of the color palette used in the SyncTax Android Application, mapped to **HEX** and **HSL** formats. You can use this document to align the colors of the **SyncTax Desktop** application (which utilizes CSS variables and Tailwind CSS).

---

## 🎨 Color Palette Conversion Reference

### ☀️ Light Mode Theme Colors

| Android Variable Name | HEX Value | HSL Value | UI Role & Desktop Mapped CSS Variable |
| :--- | :--- | :--- | :--- |
| `LightMainBackground` | `#F8F9FA` | `210 17% 98%` | `--background-color-1` (Main background) |
| `LightAppBarBackground` | `#FFFFFF` | `0 0% 100%` | `--background-color-2` / `--side-bar-background` (Surfaces) |
| `LightAccentPrimary` | `#FF0033` | `348 100% 50%` | `--text-color-highlight-2` / `--foreground-color-1` (Primary Crimson Accent) |
| `LightAccentPressed` | `#E0002A` | `349 100% 44%` | Active pressed button backgrounds |
| `LightCardBorder` / `LightDivider` | `#E0E0E0` | `0 0% 88%` | `--seekbar-track-background-color` (Borders & Dividers) |
| `LightTextTitle` | `#1C1B1F` | `260 8% 12%` | `--text-color` (Primary headings) |
| `LightTextBody` | `#49454F` | `256 7% 29%` | `--text-color-dimmed` (Secondary description/text) |
| `LightTextTertiary` | `#79747E` | `257 5% 47%` | Tertiary/disabled labels |
| `LightChipUnselected` | `#E8E8E8` | `0 0% 91%` | `--scrollbar-thumb-background-color` / Chip backings |

---

### 🌙 Dark Mode Theme Colors

| Android Variable Name | HEX Value | HSL Value | UI Role & Desktop Mapped CSS Variable |
| :--- | :--- | :--- | :--- |
| `MainBackground` | `#0E0E0F` | `240 5% 7%` | `--dark-background-color-1` (Deep dark background) |
| `AppBarBackground` / `BottomNavBackground` | `#19191C` | `240 6% 10%` | `--dark-side-bar-background` / Mini-player surface |
| `CardBackground` | `#1A1A1D` | `240 6% 11%` | `--dark-background-color-2` (Content card backgrounds) |
| `CardBorder` / `ProgressUnfilled` | `#2A2A2E` | `240 5% 17%` | `--dark-seekbar-track-background-color` (Outlines / Tracks) |
| `AccentPrimary` | `#FF3B50` | `354 100% 62%` | `--dark-text-color-highlight-2` (Primary Red Accent) |
| `AccentPressed` | `#C92E3D` | `354 62% 48%` | Pressed/active button backgrounds in dark mode |
| `TextTitle` | `#FFFFFF` | `0 0% 100%` | `--dark-text-color` (Primary text) |
| `TextBody` | `#B3B3B3` | `0 0% 70%` | `--dark-text-color-dimmed` (Secondary descriptions) |
| `TextTertiary` | `#8C8C8C` | `0 0% 55%` | Tertiary metadata/labels |
| `ChipUnselected` | `#242428` | `240 5% 15%` | Unselected chip/tag backings |

---

## 🛠️ CSS Variables for SyncTax Desktop (`styles.css`)

Copy and paste this CSS block directly into your desktop application's global styles stylesheet (e.g., `styles.css`):

```css
:root {
  /* ==================== LIGHT MODE ==================== */
  --background-color-1: 210 17% 98%;           /* Light background: #F8F9FA */
  --background-color-2: 0 0% 100%;             /* Card & Panel: #FFFFFF */
  --background-color-3: 0 0% 95%;              /* Focus/hover highlights */
  --side-bar-background: 0 0% 100%;            /* Sidebar background: #FFFFFF */
  --text-color: 260 8% 12%;                    /* Primary title text: #1C1B1F */
  --text-color-dimmed: 256 7% 29%;             /* Secondary body text: #49454F */
  --text-color-highlight: 257 5% 47%;          /* Subtext/labels: #79747E */
  --text-color-highlight-2: 348 100% 50%;      /* Primary red accent: #FF0033 */
  --foreground-color-1: 348 100% 50%;          /* Focus borders and active indicators */
  
  --context-menu-background: 0 0% 100%;
  --context-menu-list-hover: 0 0% 91%;         /* Chip/hover: #E8E8E8 */
  --seekbar-background-color: 348 100% 50%;
  --seekbar-track-background-color: 0 0% 88%;  /* Track background: #E0E0E0 */
  --scrollbar-thumb-background-color: 0 0% 88%;

  /* ==================== DARK MODE ==================== */
  --dark-background-color-1: 240 5% 7%;         /* Main Dark: #0E0E0F */
  --dark-background-color-2: 240 6% 11%;        /* Card background: #1A1A1D */
  --dark-background-color-3: 240 6% 10%;        /* Active highlights: #19191C */
  --dark-side-bar-background: 240 6% 10%;       /* Sidebar background: #19191C */
  --dark-text-color: 0 0% 100%;                 /* Primary text: #FFFFFF */
  --dark-text-color-dimmed: 0 0% 70%;           /* Secondary text: #B3B3B3 */
  --dark-text-color-highlight: 0 0% 55%;        /* Subtext: #8C8C8C */
  --dark-text-color-highlight-2: 354 100% 62%;  /* Dark Red Accent: #FF3B50 */
  
  --dark-context-menu-background: 240 6% 10%;   /* Popup menus: #19191C */
  --dark-context-menu-list-hover: 240 5% 15%;   /* Hover overlays: #242428 */
  --dark-seekbar-background-color: 354 100% 62%;
  --dark-seekbar-track-background-color: 240 5% 17%; /* Track background: #2A2A2E */
}
```

---

## 🚀 Tailwind CSS Config Extension (`tailwind.config.js`)

If the SyncTax Desktop application maps these custom CSS variables inside Tailwind CSS, you can configure your `tailwind.config.js` theme options as follows:

```javascript
module.exports = {
  theme: {
    extend: {
      colors: {
        // Light Theme variables mapped to CSS custom properties
        background: {
          light: 'hsl(var(--background-color-1) / <alpha-value>)',
          card: 'hsl(var(--background-color-2) / <alpha-value>)',
          sidebar: 'hsl(var(--side-bar-background) / <alpha-value>)',
        },
        text: {
          primary: 'hsl(var(--text-color) / <alpha-value>)',
          secondary: 'hsl(var(--text-color-dimmed) / <alpha-value>)',
          tertiary: 'hsl(var(--text-color-highlight) / <alpha-value>)',
        },
        accent: {
          primary: 'hsl(var(--foreground-color-1) / <alpha-value>)',
        },

        // Dark Theme variables mapped to CSS custom properties
        dark: {
          background: 'hsl(var(--dark-background-color-1) / <alpha-value>)',
          card: 'hsl(var(--dark-background-color-2) / <alpha-value>)',
          sidebar: 'hsl(var(--dark-side-bar-background) / <alpha-value>)',
          text: {
            primary: 'hsl(var(--dark-text-color) / <alpha-value>)',
            secondary: 'hsl(var(--dark-text-color-dimmed) / <alpha-value>)',
            tertiary: 'hsl(var(--dark-text-color-highlight) / <alpha-value>)',
          },
          accent: {
            primary: 'hsl(var(--dark-text-color-highlight-2) / <alpha-value>)',
          }
        }
      }
    }
  }
}
```
