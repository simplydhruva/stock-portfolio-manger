# Universal UI Design System for Java Swing Desktop App

## Objective
Create a universal UI design system for the existing Java Swing desktop app inspired by modern dashboard styles. The system will be visually appealing, modular, and adaptable to various content pages.

---

## Design Components

### 1. Navigation Bar / Sidebar
- Persistent sidebar on the left with grouped icons and labels for main sections:
  - Dashboard
  - Profile
  - Analytics
  - Settings
  - Watchlist
  - Trading
  - Portfolio
  - Community
  - Leaderboard
  - Competitions
  - Forum
- Consistent iconography, color gradients, and shape styles.
- Collapsible sidebar for more screen space (future enhancement).
- Highlight active section with soft glow or accent color.
- Implemented SidebarPanel.java with basic navigation and highlight functionality.
- Integrated SidebarPanel into App.java UI layout.

### 2. Page Layout
- Use flexible grid or card layouts for page bodies.
- Components like tables, charts, forms, and info cards share visual themes:
  - Soft glow highlights (especially in dark mode)
  - Subtle drop shadows
  - Rounded corners
  - Legible typography with consistent font styles and sizes
- Responsive resizing to adapt to window size changes.
- Existing screens refactored to use CardLayout for navigation.

### 3. Theme Management
- Support dark and light modes.
- Auto-detect system theme preference on startup.
- Allow manual override via Settings page.
- Preserve color contrasts and visual accents in both modes.
- Smooth animated transitions between themes.
- ThemeManager singleton implemented with ThemeChangeListener for notifications.

### 4. Reusable Content & Widgets
- Create reusable Swing components for:
  - Charts and graphs (using libraries like JFreeChart)
  - Info cards and status indicators
  - Tables with sorting and filtering
- Components inherit app theme and respond to theme changes.
- Animated transitions for real-time data updates.
- To be developed in next phases.

### 5. Settings Page Enhancements
- Theme toggle button with current theme sync status (system/manual).
- Profile settings with avatar upload, bio, location, website.
- Save and persist user preferences including theme.
- SettingsScreen.java exists and integrated.

---

## Backend Support (Existing)
- Ensure backend APIs provide consistent context-aware data for all pages.
- Support real-time updates for UI elements.
- Store and update theme preferences per user.

---

## Implementation Summary

- SidebarPanel.java created and integrated.
- App.java updated to include SidebarPanel.
- WatchlistScreen fixed for null user handling.
- SettingsScreen verified integrated.
- ThemeManager.java created for theme management (dark/light modes, system detection, manual override).
- ThemeChangeListener.java created for theme change notifications.
- ThemeManager integrated into SidebarPanel for dynamic theme switching.
- SettingsScreen enhanced with theme status display and listener for theme changes.
- InfoCard.java created as a reusable UI component with theme support.
- InfoCard integrated into DashboardScreen for displaying portfolio metrics.
- DashboardScreen updated to use theme listener.
- Further UI components and settings enhancements planned.

---

## Next Steps

- Integrate ThemeManager into existing screens and components for theme switching.
- Develop reusable UI components with theme support.
- Enhance page layouts with consistent styling.
- Add animations and transitions for real-time updates.
- Thorough testing of navigation, theme, and UI consistency.

Please review this updated plan and let me know if you want me to proceed with the next implementation steps.
