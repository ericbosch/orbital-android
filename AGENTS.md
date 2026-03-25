# Orbital Android · Guidelines

Local AI agent dispatcher for Android. Port of the React/Vite mockup.

## Stack & Architecture

- **Language:** Kotlin 2.0+
- **UI:** Jetpack Compose (Material 3)
- **Networking:** Ktor Client (WebSockets + Streaming)
- **Dependency Injection:** Hilt
- **Local Discovery:** Android NSD (mDNS)
- **Appearance:** Dynamic themes (Amoled, Nord, Solarized, etc.) + Accent colors.

## Project Structure

```
app/src/main/java/com/orbital/app/
├── core/               # Shared logic, networking, DI modules
│   ├── network/        # Ktor, WebSockets, NSD discovery
│   └── di/             # Hilt modules
├── data/               # Repository & API client implementations
├── ui/                 # UI components and themes
│   ├── theme/          # Color.kt (Design tokens), Theme.kt, Type.kt
│   ├── components/     # Shared UI (Buttons, Cards, Icons)
│   └── screens/        # Port of React screens (Splash, Scan, Home, etc.)
└── domain/             # Business logic (Agent management, Skills)
```

## Styling (Design Tokens)

All colors and fonts from `src/constants.js` in the React mockup must be ported to `ui/theme/`.
- Themes: `dark`, `amoled`, `nord`, `solarized`, `light`.
- Accents: `indigo`, `blue`, `teal`, `rose`, `amber`, `green`.
- Fonts: `Syne`, `JetBrains Mono`, `Space Grotesk`, `DM Sans`.

## Testing

- UI Tests: ComposeTestRule for screen flows.
- Unit Tests: JUnit 5 for IA client logic and gateway routing.
- Integration: MockWebServer for testing IA streaming responses.

## Deep Links & Integrations

- Integration with Termius/Mosh via Intent/Deep-links.
- Support for Tailscale via Android SDK.
