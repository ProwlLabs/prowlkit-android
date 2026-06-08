# Contributing

Thanks for contributing to ProwlKit Android.

## Development Setup

1. Android Studio Ladybug or newer, JDK 17.
2. Clone the repo and run:

```bash
./gradlew :sample:assembleDebug
./gradlew :prowl-core:testDebugUnitTest
```

3. Local Maven smoke test:

```bash
./gradlew publishAllToMavenLocal
```

## Pull Request Guidelines

- Keep changes focused and reviewable.
- Add or update tests for behavior changes in `prowl-core` when practical.
- Avoid exposing new public API unless necessary.
- Match existing Kotlin style and module boundaries (`prowl-core`, `prowl-ui`, `prowl` facade).
- Update `README.md` and `CHANGELOG.md` when public behavior or API changes.

## Release

See the **Release Checklist** section in `README.md`.
