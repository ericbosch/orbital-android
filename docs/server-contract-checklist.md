# Server Contract Checklist (Orbital + OrbitDock)

Status date: 2026-03-26

## Goal

Define a stable Android-facing contract that works with:

- Orbital server (current)
- OrbitDock server (future compatibility profile)

## Phase 1: Connectivity and Identity (must-have)

- `GET /health` returns 200 and JSON status.
- Auth token accepted via `Authorization: Bearer ...` (HTTP) and WS token/header.
- Server profile detectable:
  - Orbital profile
  - OrbitDock profile
- App surfaces active backend profile in Settings.

## Phase 2: Session List + History (must-have)

- List sessions endpoint normalized to common `Session` model.
- Fetch message history endpoint normalized to common `ChatMessage` model.
- Pagination support:
  - load latest page
  - load older pages on demand

## Phase 3: Realtime Stream (must-have)

- WebSocket connect + keepalive.
- Incremental text chunks delivered without frame-loss.
- Stream states surfaced in UI:
  - connecting
  - retrying
  - fallback mode
- Error mapping to actionable user messages.

## Phase 4: Human-in-the-loop (must-have)

- `permission_request` surfaced as action-required in chat.
- Interactive questions surfaced as action-required in chat.
- Transport hooks for approve/deny/answer operations.

## Phase 5: Operator Controls (next)

- Interrupt session turn.
- Resume session.
- Fork session (if backend supports).
- Optional: compact context / undo.

## Contract Strategy

- Keep Android domain models backend-agnostic.
- Add per-backend adapter layer:
  - `OrbitalAdapter`
  - `OrbitDockAdapter`
- Select adapter from detected backend profile.
- Preserve backward compatibility with Orbital as default.

## Current progress

- Backend profile persisted and shown in Settings.
- Stream reliability improved (retry/status/friendly errors).
- Action-required stream events surfaced in chat.

## Next implementation tasks

- Add `SessionGateway` interface and two implementations (Orbital/OrbitDock).
- Move current `OrbitalApiClient` behavior under Orbital adapter.
- Implement minimal OrbitDock adapter:
  - health
  - sessions list
  - messages history
  - websocket send/stream baseline
