# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
