# ProwlKit Android

<p align="center">
  <img src="prowl-ui/src/main/res/drawable-nodpi/prowl_kit.png" alt="ProwlKit" width="220" />
</p>

<p align="center">
  <a href="https://github.com/ProwlLabs/prowlkit-android/releases/latest"><img alt="Version" src="https://img.shields.io/github/v/tag/ProwlLabs/prowlkit-android?label=version&sort=semver"></a>
  <a href="https://github.com/ProwlLabs/prowlkit-android/actions/workflows/publish.yml"><img alt="Publish" src="https://img.shields.io/github/actions/workflow/status/ProwlLabs/prowlkit-android/publish.yml?branch=main&label=publish"></a>
  <img alt="Android" src="https://img.shields.io/badge/Android-7.0%2B%20(API%2024)-3DDC84">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.0-7F52FF">
</p>

<p align="center">
  A lightweight network debugger for Android — OkHttp interception, sensitive-data masking,<br>
  response mocking, and a built-in Jetpack Compose inspector. Companion to
  <a href="https://github.com/ProwlLabs/prowlkit-ios">ProwlKit iOS</a>.
</p>

## Overview

ProwlKit Android is inspired by [Chucker](https://github.com/ChuckerTeam/chucker) but aims for **feature parity with [ProwlKit iOS](https://github.com/ProwlLabs/prowlkit-ios)** — including mock responses, request rewrites, HAR export, and a full inspector UI.

| Module | Role |
|--------|------|
| `:prowl-core` | OkHttp interceptor, storage, mocking, rewrites, masking, export |
| `:prowl-ui` | Jetpack Compose inspector (list, detail, mock/rewrite editors, settings) |
| `:prowl` | Public facade (`Prowl.start()`, `applyProwl()`) |
| `:prowl-grpc` | Optional gRPC client interceptor |
| `:sample` | Demo app |

## Features

- OkHttp request/response logging (application interceptor, Chucker-style)
- Runtime logging toggle (pause/resume interception)
- Thread-safe FIFO log buffer (default `200`)
- Built-in sensitive data masking (toggleable at runtime)
- Jetpack Compose inspector dashboard + detail tabs
- Real-time search with query syntax
- URL ignore rules via substring and regex pattern
- **Response mocking** — short-circuit matching requests with a fake response
- **Request rewrite rules** — mutate URL, headers, or body before the network
- Mock / rewrite editors pre-filled from captured traffic
- Persistent mock rules, rewrite rules, and optional session restore
- WebSocket event logging via `newProwlWebSocket()`
- Export logs as formatted text, cURL, or HAR
- Endpoint rate alerts (per method + path)
- Optional response-body transform for logging (encrypted payloads)
- Request timing breakdown (DNS, connect, TLS, body phases)
- Host IP capture, multipart parsing, JSON tree viewer
- Floating debug bubble, shake-to-open / shake-to-clear
- Watch / pin endpoints
- Light / dark / system theme
- Localization: English, Indonesian, Arabic, Japanese, Korean, Chinese, Spanish

## Install (Maven)

Add the repository, then depend on the facade artifact `prowl` (pulls in `prowl-core` + `prowl-ui`).

### Maven Central

```kotlin
dependencies {
    debugImplementation("io.github.prowllabs:prowl:0.1.0")
}
```

> Use `debugImplementation` in production apps so the inspector never ships to release builds.

### GitHub Packages

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/ProwlLabs/prowlkit-android")
            credentials {
                username = providers.gradleProperty("gpr.user").get()
                password = providers.gradleProperty("gpr.key").get()
            }
        }
    }
}

// app/build.gradle.kts
dependencies {
    debugImplementation("io.github.prowllabs:prowl:0.1.0")
}
```

Set credentials in `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=ghp_xxxxxxxxxxxxxxxxxxxx
```

### Local development (`mavenLocal`)

```bash
./gradlew publishAllToMavenLocal
```

```kotlin
// settings.gradle.kts
repositories {
    mavenLocal()
    google()
    mavenCentral()
}
```

### Granular modules (optional)

```kotlin
debugImplementation("io.github.prowllabs:prowl-core:0.1.0") // interceptor only
debugImplementation("io.github.prowllabs:prowl-ui:0.1.0")   // UI only
debugImplementation("io.github.prowllabs:prowl-grpc:0.1.0") // gRPC only
```

## Quick Start

### 1) Start Prowl

Call once at app startup (typically `Application.onCreate`):

```kotlin
import com.prowllabs.prowl.Prowl

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prowl.start(this)
    }
}
```

### 2) Attach the interceptor

```kotlin
import com.prowllabs.prowl.applyProwl
import okhttp3.OkHttpClient

val client = OkHttpClient.Builder()
    .applyProwl()
    .build()
```

Works with **Retrofit**, **Ktor OkHttp engine**, or any client built on OkHttp.

### 3) Open the inspector

After `Prowl.start()`:

- Tap the **persistent notification**, or
- Enable **Floating debug bubble** in Prowl Settings, or
- Call `Prowl.show()` from your own debug menu

```kotlin
Prowl.show()
Prowl.stop() // dismiss notification and stop debugger runtime
```

## Ignore Noise URLs (Optional)

Pass partial URL strings at startup:

```kotlin
Prowl.start(
    context = this,
    ignoredUrls = listOf(
        "firebaselogging.googleapis.com",
        "app-analytics-services.com",
    ),
    ignoredUrlRegexes = listOf(
        """https://api\.example\.com/v[0-9]+/health""",
    ),
)
```

Or add rules at runtime:

```kotlin
Prowl.ignoreUrl("res.cloudinary.com")
Prowl.ignoreUrlRegex("""https://telemetry\.[^/]+/collect""")
```

## Configure Storage and Masking

```kotlin
import com.prowllabs.prowl.Prowl
import com.prowllabs.prowl.core.masking.SensitiveDataMasker
import com.prowllabs.prowl.core.storage.ProwlStorage

Prowl.configure(
    storage = ProwlStorage(limit = 500),
    masker = SensitiveDataMasker(
        sensitiveHeaders = setOf("authorization", "cookie", "x-api-key"),
        sensitiveJsonKeys = setOf("password", "token", "accessToken"),
    ),
)

Prowl.isSensitiveDataMaskingEnabled = false // default (show raw values)
Prowl.isSensitiveDataMaskingEnabled = true  // redact sensitive values
```

## Response Mocking

Create rules from the inspector (**Share → Create Mock**) or programmatically:

```kotlin
import com.prowllabs.prowl.core.mocking.ProwlMockRule

Prowl.addMockRule(
    ProwlMockRule(
        targetUrlPattern = "/api/users",
        targetMethod = "GET",
        mockStatusCode = 200,
        mockBody = """{"users":[]}""".toByteArray(),
        mockHeaders = mapOf("Content-Type" to "application/json; charset=utf-8"),
    ),
)
```

Mock rules persist across process death (`prowl_mock_rules.json`).

Use a **specific URL pattern** (e.g. `/content/api/v4/chapters`) so you do not accidentally mock unrelated endpoints.

## Request Rewrite Rules

Rewrite outgoing requests before they hit the network (separate from response mocks):

```kotlin
import com.prowllabs.prowl.core.mocking.ProwlRequestRewriteRule

Prowl.addRequestRewriteRule(
    ProwlRequestRewriteRule(
        targetUrlPattern = "api.staging.example.com",
        replacementUrl = "https://api.example.com",
        headerOverrides = mapOf("X-Env" to "dev"),
    ),
)
```

## WebSocket Logging

```kotlin
import com.prowllabs.prowl.newProwlWebSocket

client.newProwlWebSocket(request, listener)
```

## Endpoint Rate Alerts (Optional)

```kotlin
import com.prowllabs.prowl.core.logging.ProwlEndpointRateAlertRule

Prowl.endpointRateAlertRules = listOf(
    ProwlEndpointRateAlertRule(
        match = ProwlEndpointRateAlertRule.Match.UrlContains("api.example.com/search"),
        threshold = 50,
    ),
)

Prowl.resetEndpointRateAlertCounters()
```

## Response Body Transform for Logging (Optional)

```kotlin
import com.prowllabs.prowl.core.logging.ResponseBodyLoggingTransformer

Prowl.responseBodyLoggingTransformer = ResponseBodyLoggingTransformer { body, contentType ->
    // Return decoded bytes for display, or null to keep the original payload.
    null
}
```

## Toggle Logging at Runtime

```kotlin
Prowl.isLoggingEnabled = false // pause interception
Prowl.isLoggingEnabled = true  // resume interception
```

## Debug-Only Integration Pattern

Wrap Prowl behind a debug source set so release builds never link the library:

```kotlin
// src/debug/.../NetworkDebugger.kt
object NetworkDebugger {
    fun install(app: Application) = Prowl.start(app)
    fun applyTo(builder: OkHttpClient.Builder) = builder.applyProwl()
}

// src/release/.../NetworkDebugger.kt
object NetworkDebugger {
    fun install(app: Application) = Unit
    fun applyTo(builder: OkHttpClient.Builder) = builder
}
```

## Export Logs

From the inspector detail screen or settings:

- **Formatted text** — readable full entries
- **cURL** — replayable commands
- **HAR** — import into Charles, Proxyman, etc.

## Sample App

```bash
./gradlew :sample:assembleDebug
```

The `:sample` module demonstrates `Prowl.start()`, `.applyProwl()`, mock rules, and opening the inspector.

## Build

```bash
./gradlew :sample:assembleDebug
./gradlew :prowl-core:testDebugUnitTest
```

## Publish (Maintainers)

1. Copy `publish.properties.example` → `publish.properties` and fill credentials.
2. Bump `VERSION` in `gradle.properties`.
3. Publish:

```bash
./gradlew publishAllToMavenLocal          # ~/.m2/repository
./gradlew publishAllToGitHubPackages      # needs GITHUB_ACTOR + GITHUB_TOKEN
./gradlew publishAllToMavenCentral        # needs Sonatype + signing keys
```

CI workflow `.github/workflows/publish.yml` runs on GitHub **Release published** (tag → version, strips leading `v`).

### Maven Central checklist

1. Namespace `io.github.prowllabs` verified at [central.sonatype.com](https://central.sonatype.com/).
2. Add repo secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY`, `SIGNING_PASSWORD`.
3. Tag a release (e.g. `v0.1.0`) and publish; close + release the staging repo in the Sonatype UI.

## Troubleshooting

- **Inspector does not show traffic** — ensure `.applyProwl()` is on the same `OkHttpClient` your app uses and `Prowl.isLoggingEnabled` is `true`.
- **Mock seems ignored** — check the URL pattern matches the full URL; use a specific path substring.
- **App crashes after mocking** — use status `200` and valid JSON for the endpoint schema; overly broad patterns can break auth/token calls.
- **Stale library after local publish** — run `./gradlew --refresh-dependencies` in the host app and reinstall.
- **Floating bubble icon not updating** — force-stop the app or toggle the bubble setting in Prowl Settings after upgrading.

## Public API Policy

- `Prowl` facade remains the main public entrypoint.
- Core internals stay `internal` unless there is a clear consumer need.
- Any new public API should be documented in this `README.md` and added to `CHANGELOG.md`.

## Release Checklist

1. Run tests and sample build:

```bash
./gradlew :prowl-core:testDebugUnitTest :sample:assembleDebug
```

2. Verify CI / publish workflow is green.
3. Review public API surface on the `prowl` facade.
4. Validate docs examples in this `README.md` still match current behavior.
5. Create an immutable annotated tag and push it:

```bash
git tag -a 0.1.0 -m "Release 0.1.0"
git push origin 0.1.0
```

6. Publish to Maven Central / GitHub Packages as needed.
7. Update `CHANGELOG.md` with release notes.

## Notes

- Built on OkHttp 4.x and Jetpack Compose Material 3.
- Log capture is designed to avoid breaking host networking (body replay, safe intercept fallback).
- Mock and rewrite rules persist to app-private storage.
- Distributed via Maven (Central / GitHub Packages) — not on JCenter.
- See [`CONTRIBUTING.md`](CONTRIBUTING.md) for development guidelines and [`SECURITY.md`](SECURITY.md) for disclosure policy.

## License

Distributed under the MIT License — see [`LICENSE`](LICENSE).
