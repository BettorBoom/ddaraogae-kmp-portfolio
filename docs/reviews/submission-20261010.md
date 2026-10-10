# 제출 범위 독립 Sol 리뷰 · 2026-10-10

- Reviewer: 별도 GPT-6.1 Sol 에이전트 1회(사용자 선택 A).
- 입력: 당시 비커밋 migration diff, `AGENTS.md`, ADR-010, domain/data/runtime/Android/iOS/UI, Room 보존 테스트, scenario/Release guard. Reviewer가 기록한 tracked binary diff SHA-256: `f6d4bfe9153f4f305c92ea784ca2e20b98b6b1b47876a5788e31d9b5ce317563`.
- 독립 outcome: **changes requested**. 검토자가 테스트를 직접 실행하거나 이후 수정본을 재승인한 것은 아니다.

## 지적과 처리

1. `AppRuntime.pauseForBackground()`가 Room Flow의 단일 snapshot만 확인해 start/resume 저장이 늦게 도착하면 background 시간을 계속 셀 수 있었다. 사용자 범위 런타임이 중단 시각을 보존하고 늦게 나타난 Recording 구간을 그 시각으로 pause하도록 수정했다. 중단 대기 중 checkpoint와 늦은 위치 callback도 막는다. 지연 방출 회귀 테스트를 추가했다.
2. 좌표 transaction이 구간 ID와 이전 좌표 시간은 검사했지만, 해당 구간 **시작 전**의 정확한 좌표를 수용할 수 있었다. Room v4에 타이머 checkpoint와 별개인 `segmentStartedAtMillis`를 추가하고, start/resume에서 설정해 append 시 하한으로 검사한다. v3 migration은 복원할 수 없는 구간 시작에 마지막 checkpoint를 보수적으로 사용한다. 시작 전·재개 전 좌표와 v3→v4 보존 회귀 테스트를 추가했다.

## 검증 상태

수정 후 공통 Kotlin/JVM/iOS 테스트 33개와 Android 일반 계측 1개, 아키텍처 검사, Swift 공개 API 검사가 통과했다. iOS UI·시나리오·Release guard·PR CI의 최종 결과는 제출 직전 devlog와 Actions에 연결한다. 독립 리뷰를 self-approval로 바꾸지 않는다.

## 후속 독립 재검토 · 2026-10-10

동일 Sol 리뷰어가 staged+unstaged 소스, migration·회귀 테스트, Android/iOS host와 UI, ADR·README·PDF 텍스트를 다시 읽었다. 앞선 두 코드 결함은 수정된 것으로 보고했고 신규 고심각도 결함은 발견하지 못했다. 다만 pending resume→background 회귀 테스트가 없고 PDF가 이전 13m·지도 설명을 담고 있다고 지적했다. 구현자는 실제 재개 지연 테스트를 추가해 공통 Kotlin 35개를 실행했고 PDF를 현재 Android 26m·iOS GPS 미연결 화면으로 재생성해 렌더링을 확인했다. 리뷰어는 이 최종 테스트와 PDF를 직접 실행하지 않았으며 최종 승인으로 표기하지 않는다. 리뷰 당시 diff SHA-256은 `1374639ca5f9ca99393dd7e00a6c93831224e2e5cd545a296bb6336ad4a71c29`이고 이후 변경이 있으므로 제출 커밋 해시를 별도로 기록한다.
