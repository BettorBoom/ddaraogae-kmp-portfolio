# 2026-10-11 제출 범위 독립 리뷰

- Reviewer: `/root/submission_sol_review` (GPT-6.1 Sol, 구현과 분리된 코드·증거 검토)
- 처음 outcome: `changes_requested` — 자정 산책의 도장 날짜, 산책 화면의 샘플 지도 안내 누락, README iOS 좌표 주입 직후 초기화.
- 수정본 outcome: `approved` **for source and README changes only**. 320dp·2배 글씨에서 안내와 반려견 선택의 겹침 가능성을 추가 지적해, 안내를 선택 영역 아래 124dp로 내리고 최대 폭 180dp로 제한한 뒤 재검토받았다. 추가 지적은 없었다.
- 검토자 확인 fingerprint: 공통 수정 소스 3개 SHA-256 `dedf8917796bdc7b894090c94f93affaf48986ad440073b25f7dbdd2cf30bc33`; 비공개 diff `0cd41ce022088535a0fea84602f865826f74fa8448652d26cfcca75db2948777`; 공개 diff `5e7f8bc0fc7c5d4d6dac2c0ee7292862ed86b4d5d48bc956e8a3eecf1ec89ac1`.
- 검토 범위: 원본 `FinishActivity`의 시작일 전달과 공통 도장 계산, 산책 화면의 안내 배치, 공개 README의 비동기 iOS 경로 명령 순서, 두 저장소의 동일한 수정 소스 및 `git diff --check`. 검토자는 시뮬레이터·에뮬레이터 테스트를 직접 실행하지 않았다.
- 개인정보 검사: 공개 커밋 저자·파일명·추적 파일·PDF 메타데이터와 제품 화면 시트에서 노출을 발견하지 않았다. 영상 모든 프레임과 이미지 전부의 개인정보 부재까지 보증한 것은 아니다.
- 미해결 기능 범위: 실제 지도, 제품 인증, 날씨, 사진, 여러 반려견, 알림 예약, 백그라운드 위치, 실기기·TestFlight는 승인 범위 밖이며 README·현황표에 미연결로 공개한다.

수정 뒤 구현자가 M05 필수 6개를 다시 실행해 `checks_complete=true`와 입력 fingerprint `2b4831b3fcf6044f4a26b4b9c83e042a78612d0664d4d333e85959b87588daa3`를 확인했다. 공통 86개·Android 5개·iOS 7개·화면 57+57장과 Release 가드·모듈 경계가 통과했다. 검토자의 소스 승인은 이 실행과 독립적이며 시뮬레이터 결과 자체에 대한 독립 재현은 아니다.
