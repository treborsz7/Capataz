# Fix Build Error: Could Not Resolve androidx.media3:media3-common-ktx:1.4.1

The project is failing to build because it references `androidx.media3:media3-common-ktx`, which does not exist. In Media3, the base classes and Kotlin extensions are part of the `media3-common` artifact.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///D:/Android/QRCodeScanner/gradle/libs.versions.toml)
- Rename `androidx-media3-common-ktx` to `androidx-media3-common`.
- Update the artifact name from `media3-common-ktx` to `media3-common`.

#### [MODIFY] [build.gradle.kts](file:///D:/Android/QRCodeScanner/app/build.gradle.kts)
- Update the dependency reference from `libs.androidx.media3.common.ktx` to `libs.androidx.media3.common`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to verify that the project builds successfully and the dependency is resolved.
