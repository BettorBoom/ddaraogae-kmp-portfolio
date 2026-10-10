# 따라오개 · Android 우선 KMP 마이그레이션

기존 [따라오개 Android 팀 프로젝트](https://github.com/nbc-final-team2/ddaraogae)를 **기능별 Kotlin Multiplatform 모듈**로 다시 만들고 있습니다. 2026년 10월 11일 지원을 위해 양 플랫폼의 로컬 산책·실제 foreground 위치 기록을 실행할 수 있게 한 시연 범위입니다. 원본 홈·산책·이력·완료 화면의 색상과 정보 순서를 공통 Compose UI에 옮겼습니다. [남은 마이그레이션 범위](ROADMAP.md)의 완료나 배포를 뜻하지 않습니다.

원본 앱은 팀 공동 작업입니다. 이 저장소의 마이그레이션은 원본의 Android View XML과 Naver Map 화면을 그대로 복사한 것이 아니라 공통 UI·데이터 수명·플랫폼 연결을 다시 설계한 작업이며, 원본 저장소에 공개된 반려견 로고와 샘플 지도 자산을 재사용했습니다. 기존 앱과 현재 시연판의 차이도 아래 표와 [UI 결정 기록](docs/adr/011-original-ui-fidelity.md)에 명시했습니다.

| 기능 | Android | iOS |
|---|---|---|
| 반려견 이름 1개 등록·수정, 산책에 이름 스냅샷 | 로컬 Room | 같은 공통 Room |
| 산책 시작·중단/재개·종료, 경과 시간, 재실행 복원 | 지원 | 지원 |
| 목록·상세·좌표 상대 이동선 | 지원 | 화면/저장 지원 |
| 실제 위치·거리 | **앱이 열린 동안** GPS/네트워크 위치 | **앱이 열린 동안** Core Location 위치 |
| 경로 시연 데이터 | 실제 위치가 들어온 경우만 | 실제 위치 또는 별도 Debug scenario DB의 **합성 좌표** |
| 로그인·클라우드 동기화·상시 백그라운드 위치·지도 타일 | 미구현 | 미구현 |

iOS Debug 합성 경로는 화면 상단에 **“개발용 합성 경로 · 실제 GPS가 아니에요.”**라고 표시합니다. Release에는 이 합성 데이터 진입점을 넣지 않습니다. 산책 화면의 지도 이미지는 원본 앱에 포함된 **고정 샘플**이며 현재 위치나 좌표와 맞지 않습니다. 경로 상세는 저장 좌표의 상대적 이동선을 그 위에 표시하고 불일치를 화면에 밝힙니다. 로컬 `local-preview`는 Firebase 사용자 ID가 아닙니다.

## 직접 시연하기

필요 도구: JDK 17, Android SDK 36/Build Tools 35, Xcode 26 및 iPhone Simulator. 현재 설정은 Android minSdk 28/targetSdk 35, iOS 16.0 이상입니다. 새 Firebase 계정·프로젝트나 서명 비밀번호는 **로컬 시연에 필요하지 않습니다**.

Android: 저장소를 Android Studio에서 열고 `androidApp`을 에뮬레이터/기기에 실행합니다. CLI에서는 `local.properties`에 `sdk.dir=/절대/Android/sdk`를 지정한 뒤 다음을 실행합니다. `ddaraogae.buildRoot`를 지정하지 않은 기본 APK 경로를 사용한 예입니다.

```sh
./gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

앱에서 **반려견 이름 등록하기 → 이름 저장하기 → 하단 산책 탭 → 산책 시작하기 → 산책 마치기 → 산책 기록 보기** 순서로 진행합니다. 홈 아래에도 같은 산책 진입 버튼이 있습니다. 위치 권한은 앱을 사용 중일 때 요청합니다. 권한을 거절하거나 기기의 위치 서비스가 꺼져도 시간 기록은 가능하고 경로/거리는 비어 있을 수 있습니다. 산책 도중 홈으로 돌아왔다 다시 들어오거나 앱을 재시작해 중단/재개와 기록 복원을 확인할 수 있습니다. 기록 목록의 이전 산책 상세도 열 수 있습니다.

iOS: 먼저 시뮬레이터용 공용 프레임워크를 빌드하고 [Xcode 프로젝트](iosApp/iosApp.xcodeproj)를 열어 `Ddaraogae` scheme을 iPhone Simulator에서 실행합니다. `local.properties`에 `ddaraogae.buildRoot`를 썼다면 무시되는 `iosApp/Local.xcconfig`의 `DDARAOGAE_KMP_BUILD_DIR`도 같은 `sharedApp` build 경로로 맞춥니다.

```sh
./gradlew :sharedApp:linkDebugFrameworkIosSimulatorArm64
open iosApp/iosApp.xcodeproj
```

iOS 제품 화면에서 이름·산책·기록을 Android와 같은 순서로 확인합니다. 산책 중 **앱 사용 중 위치 권한**을 허용하면 Core Location 표본이 공통 Room 산책 기록에 저장됩니다. 권한을 거절하거나 정확도가 낮으면 시간만 남고 경로가 비어 있을 수 있습니다. 앱이 배경으로 가면 수집을 멈추고 산책을 중단합니다. 위치 장비 없이 경로 도식만 살펴볼 때는 scenario 전용 Debug 빌드(`-Pddaraogae.scenarios=true`와 Swift 조건 `DEBUG DDARAOGAE_SCENARIOS`)에 시작 인자 `--demo-route`를 지정합니다. 이 빌드는 `walks-demo.db`를 사용하며 일반 `walks.db`와 분리됩니다. 일반 빌드에 인자를 넘겨도 합성 경로가 생성되지 않습니다.

실제 실행 화면: [Android 이름 등록 후 홈](docs/evidence/submission/android-home.png) · [Android 에뮬레이터 GPS 기록 중](docs/evidence/submission/android-gps-walk.png) · [Android 저장된 이동선](docs/evidence/submission/android-record.png) · [iOS 시뮬레이터 Core Location 기록](docs/evidence/submission/ios-gps-record.png). [Android 조작 영상](docs/evidence/submission/android-demo.mp4)도 있습니다. 두 플랫폼 사진의 거리·위치 값은 **에뮬레이터/시뮬레이터에 주입한 좌표**로 확인한 foreground callback 결과이며 야외 실측이나 실기기 검증을 뜻하지 않습니다.

제출용 [2쪽 PDF 포트폴리오](output/pdf/ddaraogae-kmp-portfolio.pdf)는 목표·화면·설계 선택·검증 범위와 미완료 항목을 한 파일로 정리했습니다.

## 설계와 검증

`domain`은 산책 상태·거리·저장 계약, `data`는 Room v1→v2→v3→v4 migration과 좌표/시간의 원자적 저장을 소유합니다. `feature:dogs`, `feature:walk`, `feature:history`는 화면과 상호작용을 맡고 `sharedApp`이 Navigation 3·Koin 사용자 범위를 조립합니다. Android Activity와 iOS Swift host는 각자 권한·foreground 위치 callback·앱 수명을 연결합니다. 화면 ViewModel을 떠나도 기록 수명은 앱 사용자 범위에 남습니다. 늦게 도착한 위치 callback은 산책 ID·재개 segment·구간 시작 시각 검사를 통과하지 못하면 버립니다. [범위 결정](docs/adr/010-deadline-local-core.md), [원본 UI 이식 결정](docs/adr/011-original-ui-fidelity.md), [iOS 위치 결정](docs/adr/012-ios-foreground-location.md)을 참고하세요.

```sh
python3 scripts/run_platform_checks.py kmp --ticket M05
python3 scripts/run_platform_checks.py android --ticket M05 --scenarios
python3 scripts/run_platform_checks.py swift --ticket M05 --scenarios
python3 scripts/collect_scenario_screens.py
python3 scripts/run_platform_checks.py android --ticket M05
python3 scripts/run_platform_checks.py swift --ticket M05
python3 scripts/run_platform_checks.py swift --ticket M05 --location
python3 scripts/check_swift_api.py
python3 scripts/check_scenario_release.py
```

각 runner는 원본 JUnit XML/xcresult를 보관하고 테스트 0개·누락·skip·실패를 완료로 세지 않습니다. UI 전용 모듈은 양 플랫폼 앱 컴파일과 실제 화면 검사에서 확인합니다. 위 명령은 Android 에뮬레이터/iPhone Simulator가 있어야 합니다. [설계 결정](docs/adr/010-deadline-local-core.md)과 [남은 범위](ROADMAP.md)에 클라우드·서명·실기기 검증 조건을 구분했습니다. 현재 소스의 실행 결과는 개발 기록과 PR/Actions에 연결합니다.

iOS 제품 UI 테스트는 Debug 전용 `walks-ui-test.db`, 위치 주입 테스트는 `walks-location-test.db`를 재설정합니다. 일반 시연의 `walks.db`와 합성 경로의 `walks-demo.db`는 이 재설정 대상이 아닙니다. 위치 테스트는 Simulator에 앱 사용 중 위치 권한을 부여하고 테스트 종료 시 주입 경로를 지웁니다.

2026-10-10 검증: 공통 Kotlin 39개, Android 제품 계측 1개·시나리오 3개, iOS 제품 XCTest 2개·시나리오 5개·Core Location 1개가 통과했습니다. Android/iOS 각 39개 실제 시나리오 PNG를 수집했습니다. Release의 합성 경로 표식 부재와 Swift 공개 API 경계도 검사했습니다. 이 수치는 플랫폼 시뮬레이터와 로컬 실행 결과이며 실제 기기·클라우드 배포를 포함하지 않습니다.

## 남은 범위

이것은 로컬 핵심 흐름을 보여 주는 **진행 중인 포트폴리오**입니다. Firebase 실제 인증과 계정별 전환, 반려견 여러 마리/사진, 양 플랫폼 백그라운드 추적, 실시간 지도 SDK, 서버 동기화·충돌/재시도, signed APK/TestFlight/실기기 검증은 별도 작업입니다. 서명 키의 준비나 로컬 백업을 앱 배포 완료로 해석하지 않습니다. 기능·검증을 빠뜨리지 않도록 [남은 범위](ROADMAP.md)에 구분하고 완료한 부분만 위 표에 표시했습니다.
