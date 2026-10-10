# ADR-012 · iOS 사용 중 위치 수집

- 결정일: 2026-10-10
- 결정자: Codex (사용자가 기술적 세부 선택을 위임)
- 상태: 구현 중. M10 전체 완료나 실기기 검증을 뜻하지 않는다.

## 이유와 선택

제출용 로컬 앱은 Android foreground 위치만 기록한다. 같은 산책 저장 계약을 iOS에서도 검증하려면 iOS 호스트가 실제 Core Location 표본을 공통 엔진에 전달해야 한다. `CLLocationManager`와 권한 요청은 Swift 호스트에 두고, 산책 ID·재개 구간·시간·정확도 검증과 Room 트랜잭션은 기존 KMP 엔진에 둔다. Kotlin→Swift 경계는 현재 기록 중인 `(walkId, segment)` 관찰과 원시 위치 표본 제출만 노출한다. 화면/ViewModel은 위치 리스너의 소유자가 아니다.

첫 단계는 `When In Use` 권한과 화면이 활성 상태인 동안의 위치만 요청한다. 권한 거절·위치 서비스 중단·오류는 시간 기록을 유지하면서 안내한다. 앱이 배경으로 가면 리스너를 해제하고 기존 산책 중단 규칙을 적용한다. iOS 백그라운드 위치 모드, Always 권한, MapKit 실시간 지도는 배터리·심사·권한·실기기 검증을 포함한 M10 후속 작업으로 남긴다. 기존 정적 샘플 지도는 실제 좌표와 일치하지 않으므로 화면에 그 관계를 계속 명시한다.
Apple의 [CLLocationManager](https://developer.apple.com/documentation/corelocation/cllocationmanager)·[delegate](https://developer.apple.com/documentation/corelocation/cllocationmanagerdelegate) 문서에 따라 매니저를 메인 실행 루프에서 만들고 delegate를 곧바로 연결한다. 위치 표본의 시각을 확인한 뒤 기존 공통 저장 검증을 통과시킨다.

대안은 KMP/iOS에서 Core Location을 직접 바인딩하는 방식이다. Swift 호스트가 이미 앱 수명과 Firebase 공식 SDK를 소유하므로 같은 호스트에서 권한과 리스너를 관리하는 쪽이 경계를 덜 늘린다. 위치 표본은 늦게 올 수 있으므로 Swift 리스너 해제 외에 공통 엔진의 산책 ID·segment·구간 시작 시각 검사를 유지한다. Debug 시나리오 합성 경로는 별도 DB에 남겨 실제 GPS 성과로 취급하지 않는다.

## 검증 조건

Swift API 변경을 생성 header와 대조하고, Android/iOS 빌드·기존 화면 회귀·KMP 관찰 취소 및 이전 segment 거부를 확인한다. Simulator 위치 주입은 Core Location 연결의 증거로 구분하며, 실기기 위치 정확도·권한 철회·잠금/백그라운드 동작을 대체하지 않는다. M10 티켓은 이 결과만으로 닫지 않는다.

Debug XCTest의 초기화 인자는 제품 `walks.db`를 지우지 않고, 일반 화면은 `walks-ui-test.db`, 위치 주입은 `walks-location-test.db`, 합성 경로는 기존 `walks-demo.db`만 재설정한다. 이 인자와 파일명은 Release binary에서 제외되는지 별도 검사한다. Simulator 위치 시나리오는 테스트 종료 시 주입 경로를 지운다.
