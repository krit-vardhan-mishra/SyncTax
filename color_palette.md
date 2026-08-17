# SyncTax Desktop Color Palette & Theme Specification

This document details the complete color palette, design variables, and UI roles currently configured in the application.

All styles in the app are controlled by the HSL variables inside [styles.css](file:///k:/GitHub/Players/Nora-master/src/renderer/src/assets/styles/styles.css) and mapped to Tailwind CSS inside [tailwind.config.js](file:///k:/GitHub/Players/Nora-master/tailwind.config.js).

---

## ☀️ 1. Light Mode Palette

In light mode, the application follows a clean, high-contrast, soft blue-gray layout with purple-indigo accents.

| CSS Variable | HSL Value | Color Preview / Approximation | UI Role & Usage |
| :--- | :--- | :--- | :--- |
| `--background-color-1` | `0 0% 100%` | `#FFFFFF` | Main application background, main windows, and page body. |
| `--background-color-2` | `212 48% 94%` | `#EBF0F6` | Secondary panel cards, songs list grid, queue items background. |
| `--background-color-3` | `213 80% 88%` | `#C3DCFC` | Active elements, hover highlights on song lists, and volume sliders. |
| `--side-bar-background` | `212 50% 94%` | `#EBF0F6` | Background color for the persistent left sidebar navigation. |
| `--text-color` | `0 0% 0%` | `#000000` | Primary text headings, body text, and selected items. |
| `--text-color-dimmed` | `0 0% 50%` | `#7F7F7F` | Subtext, track duration labels, artist/album secondary names. |
| `--text-color-highlight` | `203 39% 44%` | `#44779B` | Muted secondary accent color for icons, active tabs, and sliders. |
| `--text-color-highlight-2` | `247 74% 63%` | `#635BE9` | Primary highlight color (play/pause active state, buttons focus). |
| `--foreground-color-1` | `247 74% 65%` | `#6961EA` | Accent highlights on buttons, buttons borders, and active indicators. |
| `--context-menu-background` | `0 0% 100%` | `#FFFFFF` (90% alpha) | Background fill for popup menus and dropdown windows. |
| `--context-menu-list-hover` | `198 18% 89%` | `#D9E2E8` (75% alpha) | Highlight cover color when hovering over context menu items. |
| `--seekbar-background-color` | `0 0% 20%` | `#333333` | Fill color for the seekbar slider thumb and active range. |
| `--seekbar-track-background-color`| `0 0% 80%` | `#CCCCCC` | Background track bar for the player seekbar slider. |
| `--scrollbar-thumb-background-color`| `212 48% 94%` | `#EBF0F6` | Fill color for webkit scrollbar handle overlays. |

---

## 🌙 2. Dark Mode Palette

In dark mode, the application switches to deep charcoal grays and glowing neon lavender accents to ensure premium visual styling.

| CSS Variable | HSL Value | Color Preview / Approximation | UI Role & Usage |
| :--- | :--- | :--- | :--- |
| `--dark-background-color-1` | `228 7% 14%` | `#212226` | Main dark mode application body, content background, and lists. |
| `--dark-background-color-2` | `225 8% 20%` | `#2F3138` | Secondary panel cards, dark menu buttons, inner cards container. |
| `--dark-background-color-3` | `213 80% 88%` | `#C3DCFC` | Selected active rows and navigation links background. |
| `--dark-side-bar-background` | `228 7% 20%` | `#2F3138` | Left sidebar background in dark mode. |
| `--dark-text-color` | `0 0% 100%` | `#FFFFFF` | Primary white text for maximum readability. |
| `--dark-text-color-dimmed` | `0 0% 50%` | `#7F7F7F` | Dimmed descriptors, subheadings, and secondary text labels. |
| `--dark-text-color-highlight` | `213 80% 88%` | `#C3DCFC` | Muted highlight accent for icons and selections in dark mode. |
| `--dark-text-color-highlight-2` | `244 98% 80%` | `#A6A2FB` | Neon highlight purple-pink (play control buttons, key focus accents). |
| `--dark-context-menu-background` | `228 7% 14%` | `#212226` (90% alpha) | Background fill for popup menus and hover settings lists. |
| `--dark-context-menu-list-hover` | `224 8% 28%` | `#42454F` | Hover indicators inside contextual menus. |
| `--dark-seekbar-background-color` | `240 1% 83%` | `#D3D3D4` | Player active seek track range and thumb indicator in dark mode. |
| `--dark-seekbar-track-background-color`| `0 0% 25%` | `#404040` | Background track bar for the player seekbar slider. |

---

## 🎨 3. CSS Declaration Block

These HSL variables are declared in [styles.css](file:///k:/GitHub/Players/Nora-master/src/renderer/src/assets/styles/styles.css):

```css
:root {
  /* Core Variables */
  --background-color-1: 0 0% 100%;
  --background-color-2: 212 48% 94%;
  --background-color-3: 213 80% 88%;
  --side-bar-background: 212 50% 94%;
  --background-color-dimmed: 0 0% 30%;
  --song-background-color: var(--background-color-1);
  --text-color: 0 0% 0%;
  --text-color-dimmed: 0 0% 50%;
  --text-color-white: 0 0% 100%;
  --text-color-black: 0 0% 0%;
  --text-color-crimson: 348 83% 47%;
  --text-color-highlight: 203 39% 44%;
  --text-color-highlight-2: 247 74% 63%;
  --context-menu-background: 0 0% 100%;
  --context-menu-list-hover: 198 18% 89%;
  --seekbar-background-color: 0 0% 20%;
  --seekbar-track-background-color: 0 0% 80%;
  --foreground-color-1: 247 74% 65%;
  --scrollbar-thumb-background-color: 212 48% 94%;

  /* Dark Mode Equivalents */
  --dark-background-color-1: 228 7% 14%;
  --dark-background-color-2: 225 8% 20%;
  --dark-background-color-3: 213 80% 88%;
  --dark-song-background-color: var(--background-color-2);
  --dark-side-bar-background: 228 7% 20%;
  --dark-text-color: 0 0% 100%;
  --dark-text-color-dimmed: 0 0% 50%;
  --dark-text-color-highlight: 213 80% 88%;
  --dark-text-color-highlight-2: 244 98% 80%;
  --dark-context-menu-background: 228 7% 14%;
  --dark-context-menu-list-hover: 224 8% 28%;
  --dark-seekbar-background-color: 240 1% 83%;
  --dark-seekbar-track-background-color: 0 0% 25%;
  --dark-scrollbar-thumb-background-color: 212 48% 94%;
}
```
