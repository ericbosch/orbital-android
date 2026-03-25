# Orbital Android — Plan de Implementación

> Fecha: 2026-03-25
> Estado: En progreso
> Referencia: claudecodeui (server Node.js), happy (daemon TypeScript), litter (mobile monorepo)

---

## Contexto arquitectónico

**Servidor destino:** claudecodeui corriendo en el elitebook (puerto 8080 LAN / Tailscale).
**Protocolo:** REST + WebSocket (ws://).
**Descubrimiento:** Android NSD (mDNS) → `_claudecodeui._tcp` o IP manual.
**Auth:** Bearer token (header `Authorization`).
**Streaming:** WebSocket con eventos JSON — `output`, `tool_use`, `done`, `error`.

---

## Estado actual del esqueleto

| Fichero | Estado |
|---|---|
| `NsdDiscoveryService` | ✅ Estructura lista, falta probar |
| `OrbitalApiClient` | ⚠️ Esqueleto Ktor, endpoints por implementar |
| `OrbitalRepository` | ⚠️ Stub, lógica por añadir |
| `MainViewModel` | ⚠️ Stub básico |
| `Splash / Scan / DetectAgents / Ready` | ✅ UI lista, falta conectar lógica real |
| `HomeScreen` | ✅ UI lista, datos hardcoded |
| `AgentsScreen / AgentDetailScreen` | ✅ UI lista, datos hardcoded |
| `ChatScreen` | ⚠️ UI básica, streaming por implementar |
| `SessionsScreen` | ✅ UI lista, datos hardcoded |
| `SettingsScreen` | ✅ UI lista, persistencia por añadir |
| Domain models | ✅ Agent, Server, Session, Skill definidos |

---

## Fase 1 — Conectividad y descubrimiento
> Objetivo: la app encuentra el servidor y establece conexión autenticada.

- [ ] **1.1** Completar `NsdDiscoveryService`: emitir `Flow<List<Server>>` con servidores descubiertos vía mDNS
- [ ] **1.2** Añadir entrada manual de IP/puerto en `ScanScreen` como fallback
- [ ] **1.3** Implementar `OrbitalApiClient.connect(server, token)`: validar URL + ping `GET /api/health`
- [ ] **1.4** Persistir servidor + token en `DataStore` (dentro de `OrbitalRepository`)
- [ ] **1.5** Conectar flujo onboarding completo: Scan → DetectAgents (NSD) → Ready → Home
- [ ] **1.6** `MainViewModel`: estado `Connected / Disconnected / Scanning`

## Fase 2 — Proyectos y sesiones
> Objetivo: listar y navegar proyectos/sesiones reales del servidor.

- [ ] **2.1** `GET /api/projects` → `OrbitalRepository.getProjects(): Flow<List<Project>>`
- [ ] **2.2** `GET /api/sessions?projectName=X` → `getSessionsForProject()`
- [ ] **2.3** `SessionsScreen`: reemplazar datos mock por datos reales
- [ ] **2.4** `HomeScreen`: sesiones recientes reales (últimas 5 por `updatedAt`)
- [ ] **2.5** Modelo `Project` en `domain/` con campos básicos (name, path, sessionCount)

## Fase 3 — Chat y streaming
> Objetivo: conversación funcional con streaming de respuestas.

- [ ] **3.1** `GET /api/sessions/:id/messages` → cargar historial al abrir chat
- [ ] **3.2** WebSocket connect: `ws://<host>/ws?token=<token>&sessionId=<id>`
- [ ] **3.3** Enviar mensaje: evento `{ type: "message", content: "...", sessionId: "..." }`
- [ ] **3.4** Recibir streaming: parsear eventos `output` (texto incremental), `tool_use`, `done`, `error`
- [ ] **3.5** `ChatScreen`: burbujas de mensaje, indicador de pensamiento (spinner), tool calls colapsables
- [ ] **3.6** Scroll automático al último mensaje
- [ ] **3.7** Input con envío por teclado + botón, deshabilitar durante streaming

## Fase 4 — Agents y Skills
> Objetivo: mostrar agentes disponibles y sus capacidades.

- [ ] **4.1** `GET /api/agents` (o derivar de proyectos activos) → `AgentsScreen` con datos reales
- [ ] **4.2** `AgentDetailScreen`: mostrar skills/tools del agente, historial de sesiones
- [ ] **4.3** Acción "Nueva sesión" desde `AgentDetailScreen` → navegar a `ChatScreen` con sesión nueva
- [ ] **4.4** `ExploreScreen`: búsqueda en proyectos/sesiones (`GET /api/search?q=...`)

## Fase 5 — Settings y persistencia
> Objetivo: configuración persistida, temas funcionando end-to-end.

- [ ] **5.1** `AppearanceStore`: guardar tema + accent en `DataStore` (ya existe, completar)
- [ ] **5.2** `SettingsScreen`: conectar selector de tema → aplicar en tiempo real
- [ ] **5.3** Gestión de servidores: añadir/eliminar/editar en Settings
- [ ] **5.4** Token de autenticación: input en Settings + validación contra servidor
- [ ] **5.5** `TroubleshootScreen`: ping al servidor, ver estado daemon, logs recientes

## Fase 6 — Pulido y UX
> Objetivo: app usable en el día a día.

- [ ] **6.1** Reconexión automática WebSocket con backoff exponencial
- [ ] **6.2** Manejo offline: banner de desconexión, cola de mensajes pendientes
- [ ] **6.3** Notificaciones push cuando Claude termina una tarea larga (ntfy: `krinekk-dev`)
- [ ] **6.4** Deep links: `orbital://session/<id>` para abrir sesión directamente
- [ ] **6.5** Integración Termius: Intent para abrir SSH al servidor desde la app
- [ ] **6.6** Markdown rendering en burbujas de chat (code blocks, bold, listas)

---

## Decisiones técnicas

| Decisión | Elección | Razón |
|---|---|---|
| WebSocket client | Ktor `HttpClient` + `webSocket {}` | Ya en el proyecto, evita dependencia extra |
| Persistencia | Jetpack DataStore (Preferences) | Ligero, async, suficiente para config |
| Streaming parse | Ktor `incoming.consumeEach` + `JsonDecoder` | Consistente con el resto de Ktor |
| Token storage | DataStore encriptado (`EncryptedSharedPreferences` fallback) | Seguridad básica |
| NSD service type | `_http._tcp` filtrado por nombre | claudecodeui no anuncia tipo propio |

---

## Orden de ejecución recomendado

```
F1 (conectividad) → F2 (proyectos) → F3 (chat) → F5 (settings) → F4 (agents) → F6 (pulido)
```

F3 es el núcleo: con chat funcionando la app ya es útil. F4 y F6 son mejoras incrementales.

---

## Notas

- `claudecodeui` usa WebSocket nativo (`ws` npm package), **no** Socket.io — Ktor compatible directo.
- El token se pasa como query param en la URL del WebSocket (`?token=xxx`).
- Los mensajes de streaming son JSON lines: cada línea es un evento completo.
- `happy` usa cifrado AES en mensajes — **no aplica** aquí, claudecodeui no cifra en tránsito local.
