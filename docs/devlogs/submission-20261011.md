# 2026-10-11 지원용 로컬 코어 작업 기록

최신 로컬 결과(2026-10-10): 공통 Kotlin JVM+iOS 35개, Android 제품 계측 1개·시나리오 계측 3개, iOS 제품 XCTest 2개·시나리오 XCTest 5개, Python 하네스 33개가 각각 실패/skip 0으로 통과했다. Android/iOS 각 39장 화면 PNG를 최신 테스트 원본에서 수집했고 Debug fixture의 Release 배제 3건과 unsigned Release 바이너리를 확인했다. 외부 실기기·배포는 아래 미완료 범위를 따른다.

## WHY / 결정

2026-10-09 사용자 요청: 웹 제작을 제외하고 앱 마이그레이션의 초기 설정과 Android/iOS 핵심 시연을 빠짐없이 수행한다. 채용 마감은 10월 11일이다. 사용자는 100점 전체 구현을 축소했다고 착각하지 않도록 요구했고, 기술 선택 중 비용·범위 변경을 제외한 최적의 세부 판단을 위임했다. [ADR-010](../adr/010-deadline-local-core.md)에 두 플랫폼 공통 로컬 코어, Android 실제 foreground 위치, iOS 제품의 시간/기록과 Debug 전용 합성 경로를 구분했다. 원래 M04 전체 최신 검증→M06 착수·최종 100점 기준은 유지하며 M06 인증을 시작하지 않았다.

대안은 전체 M06 인증과 TestFlight를 먼저 끝내는 방식, iOS GPS까지 동시에 구현하는 방식, Android만 보여 주는 방식이었다. 현재 마감까지 실제 계정·실기기 결과를 확보하지 못한 기능을 README에 완료로 표시할 위험이 커서 선택하지 않았다. 지도 SDK/TDS 재현도 경로 저장과 양 플랫폼 복원 검증보다 이번 범위에 덜 중요해 Canvas 좌표 도식으로 정했다. Flutter/새 웹앱/두 번째 DB 프레임워크는 도입하지 않았다.

## 실제 변경

- v3 Room migration으로 기존 v1/v2 산책을 유지하고 반려견 이름 스냅샷·경과 시간·세그먼트·좌표를 저장했다. 독립 리뷰 뒤 v4에서 체크포인트와 별개의 구간 시작 시각을 추가했다. DB 행과 거리 갱신은 한 transaction이며 품질이 낮거나 시간 역전·비정상 속도·구간 시작 전 좌표·이전 segment callback을 버린다.
- `feature:dogs` 이름 화면과 `feature:history` 목록/상세/경로 도식을 추가했다. Android/iOS 공통 Navigation3에 연결하고 iOS 시연 DB는 `walks-demo.db`로 분리했다.
- Android foreground `LocationManager` + coarse/fine 요청, Activity stop 시 구독 해제/중단을 구현했다. iOS host는 background 전환 시 중단하며 실제 Core Location은 아직 연결하지 않았다.
- 사용자별 Koin scope와 런타임이 산책을 소유한다. 새 프로세스는 과거 Recording을 Interrupted로 복구하고 새 산책은 중단시키지 않는다.
- README의 실행·기능표·한계를 현재 시연 범위로 재작성했다. 공개 self-hosted runner는 소유자 수동 dispatch·기본 브랜치·정확한 저장소 조건을 요구하도록 바꿨다.

## 실패·반증과 수정

첫 Android/iOS 컴파일에서 Compose `StrokeCap` import와 Native `Dispatchers.IO` 사용이 실패해 해당 API를 바로잡았다. JVM 실제 DB 재개방과 v2→v3 migration 테스트는 통과했다. 처음 통합 runner는 UI 전용 신규 모듈의 단위 테스트 0개를 누락으로 취급했다. runner에서 테스트 소스가 실제 0개인 타깃만 건너뛰고 Android APK/iOS framework 컴파일은 그대로 강제했다.

Android 계측 첫 실행은 OS 권한 창 때문에 Compose 계층을 잠시 찾지 못했다. 계측 목적에 맞춰 테스트 안에서 권한을 부여하고 시스템 권한 선택 자체는 별도 검증 범위로 남겼다. 두 번째 실패는 기록 중 카드가 늘면서 버튼이 화면 아래로 밀렸는데 테스트가 스크롤 없이 `assertIsDisplayed`했다. 실제 앱은 기록 중이었고 스크롤 후 확인하도록 수정했다. 2026-10-09 최신 Android 에뮬레이터 일반 `BootstrapTest` 1개와 시나리오 3개가 실패/skip 없이 통과했다.

iOS 첫 XCTest는 Compose 입력 필드를 UIKit `TextField`로 가정해 찾지 못했다. 필드에 안정적인 testTag를 주고 XCTest에서 접근성 ID로 찾은 뒤 키보드에 입력했다. 다음 XCTest는 기록 중 버튼이 첫 화면의 접근성 계층 밖으로 내려가 `waitForExistence`가 실패했다. 실패 xcresult의 첫 UI hierarchy에는 제목 `함께 걷는 중이에요`와 기록 중 카드가 있었고 산책 버튼은 viewport 밖이었다. 실행 후 앱 종료로 기록이 `Interrupted`가 된 것은 의도된 복구 흐름이다. 버튼 노출까지 스크롤하고 첫 실패에서 멈추도록 테스트를 수정했다. 수정본의 iOS 실제 XCTest 5개가 전부 통과했다.

Swift API 검사는 새 `StateFlow`가 ObjC header로 노출된 것을 거절했다. Android/Compose 내부 API에는 `HiddenFromObjC`를 적용하고 필수 `pauseForBackground`만 Swift host에 남겼다. 바뀐 공개 산책 모델 계약은 생성 header diff를 검토한 뒤 baseline에 반영했고 API 검사가 통과했다. 전체 Python 테스트에서는 두 새 feature 모듈이 아키텍처 검사 fixture에 없어 실패했다. fixture를 실제 모듈 목록으로 갱신한 뒤 33개가 통과했다.

에뮬레이터 시연 녹화에서 기록 상세의 `산책 목록으로`가 이전 `Walk`로 돌아오는 것을 발견했다. `Record` 진입 경로가 Walk/History 두 가지라 단순 pop은 버튼 의미와 맞지 않았다. 기록 상세를 닫을 때 기존 History가 있으면 거기로 돌아가고, Walk에서 왔다면 Home 위에 History를 놓도록 수정했다. Android/iOS UI 회귀 검사를 추가해 결과를 확인한다.

2026-10-10 사용자 교정: 첫 캡처의 큰 카드 UI는 기존 따라오개 화면과 다르다. 원본 공개 README 스크린샷과 Android XML 레이아웃을 다시 비교했다. [ADR-011](../adr/011-original-ui-fidelity.md)에 따라 원본 갈색/회색 토큰, 홈의 반려견·산책 그래프·도장·날씨 구획, 하단 탭, 지도 중심 산책·하단 거리/시간 조작부, 이력 그래프·완료 화면 정보 순서를 공통 Compose에서 재구성했다. 원본 View XML을 iOS에서 직접 재사용할 수 없어 공통 컴포넌트로 다시 만들고, 공개된 원본 앱의 샘플 지도·반려견 로고 자산은 다시 사용했다. 산책 기록/거리 값은 실제 로컬 DB를 사용하고, 도장·날씨·실제 지도 타일은 연결 전임을 화면에 표시한다. 샘플 지도는 현재 위치와 무관한 고정 이미지이며 경로 선도 상대 위치만 나타낸다. 변경 후 KMP 33개 테스트·양 플랫폼 컴파일이 통과했고, 제품 UI/화면 매트릭스와 제출 이미지는 재검증한다.

첫 Android 실행에서는 공통 Compose 자산의 생성 코드가 컴파일됐지만 Android APK에 실제 PNG가 없어서 `MissingResourceException`으로 중단됐다. iOS도 Xcode host에 PNG를 복사하는 첫 경로가 런타임의 `compose-resources/composeResources/...` 탐색 경로와 달랐다. Android 앱 자산과 iOS 앱 번들에 같은 원본 파일을 복사하고, Android 사본은 공통 자산과 해시가 같은지 아키텍처 검사에서 확인한다. 수정 후 Android 일반 UI 1개와 iOS 일반 UI 2개가 통과했다. UI 시나리오 실행기는 일반 iOS framework를 시나리오 빌드에 재사용하면 Swift 전용 진입점을 못 찾는 문제가 있었다. `run_platform_checks.py swift --scenarios`가 matching KMP framework를 먼저 빌드하도록 고쳤다. 이는 테스트 하네스 문제이며 앱 기능 통과로 세지 않는다.

원본 `fragment_home.xml`과 재캡처한 새 홈을 다시 대조해 큰 산책 버튼을 하단으로 옮기고, 빈 그래프도 원본처럼 고정 높이·눈금을 유지했다. 도장 영역은 원본 형태의 진행 막대를 쓰되 실제 집계가 연결되지 않았음을 밝힌다. `fragment_walk.xml`과 비교해 시작 전 지도 위 독립 버튼과 기록 중 거리·종료·시간 가로 배치를 적용했다. 이는 비주얼 충실도 선택이며 Android/iOS 동작 재검증 전에는 완료로 기록하지 않는다.

Android 첫 최종 배치 일반 UI 1개·시나리오 3개는 통과했다. 첫 시나리오 재실행은 새 산책 버튼의 `primary-action` 식별자 누락으로 2개가 실패했고, 접근성 식별자를 복구한 뒤 3개를 다시 통과시켰다. iOS 첫 시나리오 실행에서는 39장 매트릭스 자체는 끝났지만 Bootstrap이 홈의 아래로 이동한 버튼을 화면 밖에서 찾지 못했고, Debug 경로 테스트는 변경된 `전체 보기` 문구 대신 옛 `모든 산책 기록`을 찾았다. 두 테스트는 원본 화면에 가까워진 하단 탭·새 문구를 사용하도록 고쳤다. 최신 재실행에서는 제품 2개·시나리오 5개가 모두 통과했다.

2026-10-10 [독립 Sol 리뷰](../reviews/submission-20261010.md)는 처음 두 가지 변경을 요구했다. 빠른 start/resume→background 전환에서 Room Flow 단일 snapshot을 놓치는 문제는 중단 시각을 사용자 런타임에 보존해 늦은 Recording도 중단하도록 수정했다. 구간 시작 전 좌표 수용 문제는 Room v4의 별도 구간 시작 칼럼으로 고쳤다. 후속 독립 재검토는 두 코드 결함의 수정과 신규 고심각도 결함 부재를 보고했고, 미검증 pending resume 경로와 오래된 PDF를 추가로 지적했다. Interrupted→resume→background의 늦은 Flow 방출 테스트를 추가해 공통 35개가 통과했고 PDF를 현재 제품 PNG·26m 기록으로 다시 생성해 2쪽을 육안 확인했다. 후속 리뷰어가 이 마지막 테스트·PDF를 직접 실행하거나 재승인한 것으로 기록하지 않는다.

## 검증과 아직 남은 것

- 2026-10-09 로컬 증거: JVM+iOS 공용 테스트 30개, Android 계측 3개, iOS Simulator XCTest 5개, Python 하네스 33개, 모두 skip/실패 0. Android APK·iOS 시뮬레이터 framework 빌드, Room v1/v2 보존·좌표 품질·재개방, Swift header 검사, Release 합성 데이터 배제 3개 guard와 unsigned Release 바이너리 검사 통과.
- Android Pixel_4a_API_34 에뮬레이터에서 제품 UI로 Bori 이름을 등록하고 산책 중 `adb emu geo fix`로 좌표를 주입했다. 첫 실행에서는 LocationManager callback으로 13m·위치 2개를 확인했다. 최종 제품 화면 재검증에서는 35초·26m·위치 3개를 저장한 뒤 기록을 다시 열었다. [최종 화면 증거](../evidence/submission/android-record.png)는 에뮬레이터 입력이며 야외 GPS 품질이나 실기기 결과가 아니다.
- iPhone 17 Pro iOS 26 Simulator의 로컬 산책·재실행·완료 화면과 Debug 합성 경로 분리/재열기 5개 XCTest가 통과했다. [제품 기록 화면](../evidence/submission/ios-record.png)에는 GPS 부재를 그대로 표시한다. Android/iOS 각각 39개 시나리오 PNG를 실제 실행에서 수집했고 대표 화면을 `docs/evidence/M05`에 남겼다.
- 남은 제출 작업: 현재 소스 PR/Actions와 공개 전 Git 이력·이미지·Actions/산출물 검사, 새 공개 저장소 검증. 지원용 PDF/영상은 현재 제품 화면으로 교체했다.
- 외부 증거: signed APK 실제 Android 설치, Apple 서명/TestFlight 처리·초대·실기기 설치, iCloud 서버 복원, M04 최신 전체 readiness는 미완료. 이전 교체 키와 iCloud 로컬 복사는 보존했고 비밀번호를 읽거나 배포 키를 사용하지 않았다.

## 반증해야 할 다음 위험

위치 제공자가 꺼진 경우 시간만 진행되는지와 야외 오차·속도 필터의 UX는 실기기에서 추가 검증해야 한다. 좌표 품질/속도 거절은 Room/JVM 테스트로만 검증했다. 39개 실제 화면 매트릭스는 큰 글씨·다크 모드·상태별 렌더링을 확인하지만 모든 스크롤 위치의 육안 검사를 뜻하지 않는다. 전체 M04/M06/100점 항목을 이 제출 범위의 통과로 바꾸지 않는다.

## 공개 방식 결정

2026-10-09 사용자 선택: **A — 깨끗한 스냅샷으로 새 공개 저장소**. 기존 비공개 PR·CI 이력은 보존한다. 이유는 기존 23개 커밋 저자에 개인 Gmail이 있고 M04 Firebase 화면 두 장에 표시 이름이 포함되어, 저장소 공개 설정만 바꾸면 과거 이력까지 노출되기 때문이다. 새 저장소에는 최종 빌드 가능한 소스·공개 가능한 기술 기록·현재 화면 증거만 넣고, 새 첫 커밋은 GitHub noreply 저자로 만든다. 재현 가능한 테스트를 새 스냅샷에서 다시 확인한 뒤 공개한다.
