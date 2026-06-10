# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Released]

## [1.0.1] - 2026-06-10

### Changed

- `prowl-ui` now depends on `material-icons-core` instead of `material-icons-extended`, reducing transitive APK/DEX size for consumers.
- Compose `ui-tooling-preview` is `compileOnly` in `prowl-ui` so it is not shipped to library consumers.

## [1.0.0] - 2026-06-09

### Added

- Statistics charts: traffic area graph, latency trend, vertical status/method bars, latency histogram.
- Collapsible **Overview** section in Settings (collapsed by default with request summary).
- P95 latency metric in statistics overview.

### Changed

- Settings UI revamp with Material 3 grouped sections (Capture, Privacy, Inspector, Mocks, Export).
- Mock import/export moved from Export to **Mocks** section; Export is logs-only (text, cURL, HAR).
- Shake accelerometer registers only when shake-to-open or shake-to-clear is enabled.
- Default toggles: request logging ON; persistence, masking, bubble, and shake gestures OFF.

### Fixed

- Duplicate WebSocket sensor listener when inspector was open.
- Gradle consumer no longer holds accelerometer when shake features are disabled.

## [0.1.0] - 2026-06-08

Initial public release of ProwlKit Android.

### Added

#### Core (`prowl-core`)
- OkHttp application interceptor with request/response logging and timing breakdown.
- Thread-safe FIFO log storage (default `200`, configurable via `ProwlStorage(limit)`).
- Runtime toggles: `Prowl.isLoggingEnabled` and `Prowl.isSensitiveDataMaskingEnabled` (default OFF).
- Built-in `SensitiveDataMasker` for headers and JSON keys.
- URL ignore rules (substring + regex) at startup and runtime.
- Response mocking (`ProwlMocker`) with persistent rules.
- Request rewrite rules (`ProwlRequestRewriter`) with persistent rules.
- Optional session persistence across process death.
- WebSocket logging via `ProwlWebSocket`.
- HAR, formatted text, and cURL export.
- Endpoint rate alerts and optional response-body logging transformer.
- gRPC interceptor module (`prowl-grpc`).

#### UI (`prowl-ui`)
- Jetpack Compose inspector (list, detail, settings).
- Mock and request-rewrite editors pre-filled from captured traffic.
- JSON tree viewer, image preview, stats charts, search syntax.
- Persistent notification, floating debug bubble, shake gestures.
- Watch / pin endpoints, theme picker, multi-language UI strings.

#### Facade (`prowl`)
- `Prowl.start(context)`, `Prowl.show()`, `Prowl.stop()`.
- `OkHttpClient.Builder.applyProwl()` extension.
- `OkHttpClient.newProwlWebSocket()` helper.
