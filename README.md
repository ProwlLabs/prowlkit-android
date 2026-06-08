# ProwlKit Android

Android network debugger inspired by [Chucker](https://github.com/ChuckerTeam/chucker), with **feature parity to [ProwlKit iOS](https://github.com/ProwlLabs/prowlkit-ios)** — including response mocking.

## Modules

| Module | Role |
|--------|------|
| `:prowl-core` | OkHttp interceptor, FIFO storage, mocking, masking, export |
| `:prowl-ui` | Jetpack Compose inspector (list, detail, mock editor, settings) |
| `:prowl` | Public facade (`Prowl.start()`, `applyProwl()`) |
| `:sample` | Demo app |

## Quick start

```kotlin
// Application.kt
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prowl.start(this)
    }
}

// OkHttp client
val client = OkHttpClient.Builder()
    .applyProwl()
    .build()
```

Open the persistent notification (Chucker-style) or call `Prowl.show()` to launch the inspector.

## Features (v0.1)

- OkHttp request/response logging
- **Response mocking** with URL substring + method matching
- Mock editor pre-filled from captured requests
- Active mocks manager (toggle / delete)
- Sensitive data masking (headers + JSON keys)
- URL ignore rules (substring + regex)
- Export formatted text or cURL
- Endpoint rate alerts
- FIFO in-memory buffer (default 200)

## iOS ↔ Android mapping

| iOS | Android |
|-----|---------|
| `URLProtocol` | OkHttp `Interceptor` |
| `Prowl.start()` | `Prowl.start(context)` + `.applyProwl()` |
| Shake gesture | Notification tap / `Prowl.show()` |
| SwiftUI inspector | Jetpack Compose |
| Alamofire / Moya plugins | Retrofit + OkHttp (same interceptor) |

## Install (Maven)

Add the repository, then depend on the facade artifact `prowl` (pulls in `prowl-core` + `prowl-ui`).

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
    implementation("io.github.prowllabs:prowl:0.1.0")
}
```

Set credentials in `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=ghp_xxxxxxxxxxxxxxxxxxxx
```

### Maven Central

```kotlin
dependencies {
    implementation("io.github.prowllabs:prowl:0.1.0")
}
```

Granular modules (optional):

```kotlin
implementation("io.github.prowllabs:prowl-core:0.1.0") // interceptor only
implementation("io.github.prowllabs:prowl-ui:0.1.0")   // UI only
```

## Publish

1. Copy `publish.properties.example` → `publish.properties` and fill credentials.
2. Bump `VERSION` in `gradle.properties` (or `publish.properties`).
3. Publish:

```bash
# Local smoke test (~/.m2/repository)
./gradlew publishAllToMavenLocal

# GitHub Packages (needs GITHUB_ACTOR + GITHUB_TOKEN)
./gradlew publishAllToGitHubPackages

# Maven Central (needs Sonatype token + SIGNING_KEY)
./gradlew publishAllToMavenCentral
```

CI workflow `.github/workflows/publish.yml` runs on GitHub **Release published** (tag → version, strips leading `v`).

### Maven Central checklist

1. Namespace `io.github.prowllabs` verified at [central.sonatype.com](https://central.sonatype.com/).
2. Add repo secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY`, `SIGNING_PASSWORD`.
3. Tag release (e.g. `v0.1.0`) and publish; close + release the staging repo in Sonatype UI.

## Build

```bash
./gradlew :sample:assembleDebug
```

## License

MIT — same as ProwlKit iOS.
# prowlkit-android
