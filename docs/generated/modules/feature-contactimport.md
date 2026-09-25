# `:feature:contactimport`

Source directory: `feature/contactimport`

## Direct project dependencies

- `:core`
- `:core:ui`
- `:feature:contacts`
- `:feature:identity`
- `:feature:invite`
- `:data:database`
- `:core:protocol`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `RotatedLuminance` | `class` | `androidMain` | `feature/contactimport/src/androidMain/kotlin/com/cbgm/sparrow/feature/contactimport/device/QrScanner.android.kt` |
| `ImportSharedIdentityUseCase` | `class` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/domain/usecase/ImportSharedIdentityUseCase.kt` |
| `VerifyContactByQrUseCase` | `class` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/domain/usecase/VerifyContactByQrUseCase.kt` |
| `ImportIdentityViewModel` | `class` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/ImportIdentityViewModel.kt` |
| `IdentityImportTrustUi` | `class` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/model/IdentityImportTrustUi.kt` |
| `ImportIdentityUiEvent` | `interface` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/model/ImportIdentityUiEvent.kt` |
| `ImportIdentityUiState` | `class` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/model/ImportIdentityUiState.kt` |
| `ScanIdentityNavigationViewModel` | `class` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/scan/ScanIdentityNavigationViewModel.kt` |
| `ScanIdentityUiEvent` | `interface` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/scan/model/ScanIdentityUiEvent.kt` |
| `ScannedIdentityPreview` | `class` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/scan/model/ScannedIdentityPreview.kt` |
| `VerifyContactQrViewModel` | `class` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/verify/VerifyContactQrViewModel.kt` |
| `VerifyContactQrUiEvent` | `interface` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/verify/model/VerifyContactQrUiEvent.kt` |
| `VerifyContactQrUiState` | `class` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/verify/model/VerifyContactQrUiState.kt` |
