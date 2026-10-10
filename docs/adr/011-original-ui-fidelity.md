# ADR-011 · 기존 따라오개 화면 구조 보존

- 결정일: 2026-10-10
- 결정자: 사용자(기존 앱 UI/UX에 가깝게 이식), Codex(공통 UI 구현 방법 위임)
- 상태: 제출 범위 핵심 화면에 적용. 기능과 시각 검증 결과는 devlog에서 확인.

## 문제와 근거

첫 로컬 코어 구현은 산책 데이터를 저장했지만 큰 정보 카드가 세로로 나열되어 기존 Android 앱의 화면과 달랐다. 사용자가 이 차이를 지적했고, [기존 프로젝트 README](https://github.com/nbc-final-team2/ddaraogae)의 홈·산책·이력 스크린샷과 `fragment_home.xml`, `fragment_walk.xml`, `activity_history.xml`, `activity_finish.xml`을 다시 확인했다. 기존 UI는 갈색 `#6F4E37`, 연한 회색 `#F6F6F6`, 작은 흰색 카드, 홈의 반려견·그래프·도장·날씨 구획, 지도 중심 산책과 하단 조작부, 완료 화면의 지도·거리·시간 순서가 핵심이다.

기존 View XML과 Naver Map Fragment는 Android 전용이라 공통 Compose Multiplatform UI 코드로 그대로 재사용할 수 없다. 따라서 색상·정보 순서·화면 구성과 사용자 문구를 재현하고, 기존 동작 중 이번 제출 범위에 없는 서비스는 완성된 것처럼 표시하지 않는다. 산책 그래프는 로컬 기록의 최근 7회 거리로 계산한다. 도장·날씨는 연결 전 상태를 표시한다. 원본 앱에 포함된 `img_sample_map.png`와 `doglogo.png`를 공통 Compose 자산으로 재사용한다. 샘플 지도는 현재 위치와 지리적으로 맞지 않는 **고정 이미지**라고 화면에 표시한다. 기록 상세의 실제 좌표는 상대적인 이동선 도식으로만 그리며, 지도 타일을 연결한 것으로 주장하지 않는다.

## 적용·검증

`designsystem`의 토큰과 공통 페이지/카드, `sharedApp` 홈·하단 탭, `feature:walk`의 지도 중심 화면, `feature:history`의 그래프·목록·완료 레이아웃을 바꾼다. 데이터 계약·Room migration·좌표 필터는 유지한다. Android/iOS 제품 UI, 2배 글씨·다크·오류/재시도 시나리오를 다시 실행하고 캡처와 PDF/영상을 새 화면으로 갱신한다. 화면이 기존 디자인에 더 가까워져도 Naver Map·날씨·도장·사진·실기기 지도가 구현됐다고 주장하지 않는다.
