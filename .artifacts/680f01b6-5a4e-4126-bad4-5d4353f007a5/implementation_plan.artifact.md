# Implementation Plan - Regenerate README and Remove Gemini References

This plan outlines the steps to improve the project's presentation on GitHub and remove all references to Gemini AI and its API keys, as the core functionality of ScrollBreak (Wikipedia breaks) does not rely on them.

## User Review Required

> [!IMPORTANT]
> I will be removing the `firebase-ai` dependency and the `secrets` Gradle plugin configuration, as they appear to be remnants from a template and are currently only used for the Gemini API key. If you plan to use other secrets in the future, you may need to re-add the secrets plugin.

## Proposed Changes

### Documentation & Configuration

#### [MODIFY] [README.md](file:///C:/Users/admin_arthur/Downloads/scrollbreak/README.md)
- Rewrite the README to focus on ScrollBreak's features: mindful Wikipedia breaks, app usage monitoring, and saving articles.
- Remove all setup instructions related to `GEMINI_API_KEY` and AI Studio.
- Add a proper "Features" and "Getting Started" section.

#### [MODIFY] [.env.example](file:///C:/Users/admin_arthur/Downloads/scrollbreak/.env.example)
- Remove the `GEMINI_API_KEY` placeholder and related comments.
- Keep the file empty or with a generic comment if it might be used later, or simply remove the Gemini specific parts.

#### [MODIFY] [metadata.json](file:///C:/Users/admin_arthur/Downloads/scrollbreak/metadata.json)
- Remove `"majorCapabilities": ["MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API"]` to avoid misleading capabilities.

### Build System

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/admin_arthur/Downloads/scrollbreak/app/build.gradle.kts)
- Remove `alias(libs.plugins.secrets)`.
- Remove `implementation(libs.firebase.ai)`.
- Remove the `secrets { ... }` configuration block.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/admin_arthur/Downloads/scrollbreak/build.gradle.kts)
- Remove `alias(libs.plugins.secrets) apply false`.

#### [MODIFY] [gradle/libs.versions.toml](file:///C:/Users/admin_arthur/Downloads/scrollbreak/gradle/libs.versions.toml)
- Remove `firebase-ai` and `secretsGradlePlugin` / `libs.plugins.secrets`.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure the project still builds correctly without the removed dependencies and plugins.

### Manual Verification
- Review the new `README.md` to ensure it correctly presents the project.
- Verify that no "Gemini" or "API Key" references remain in the codebase using `grep`.
