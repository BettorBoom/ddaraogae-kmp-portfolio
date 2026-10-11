# 화면별 원본 패리티 · 2026-10-11

[원본 따라오개](https://github.com/nbc-final-team2/ddaraogae)의 HEAD XML·strings·dimens·화면 코드를 기준으로 공통 Compose UI를 다시 만들었다. 이 표는 **무엇이 같고, 무엇이 다르며, 무엇이 아직 연결되지 않았는지**를 화면마다 적는다. 결정과 대안은 [ADR-013](adr/013-original-ui-port-scope.md), 남은 기능 작업은 [현황표](migration-gap-audit.md)에 있다.

- 비교 방법: 원본 README 화면 6장(2024-07판)과 XML을 보고, Android 에뮬레이터(Pixel 4a API 34)와 iPhone 17 Pro Simulator(iOS 26)에서 실행한 화면을 나란히 확인했다. 원본 이미지는 이 저장소에 복사하지 않았다.
- 같은 공통 Compose 코드를 쓰지만 한쪽 확인으로 다른 쪽을 대신하지 않았다. 아래 캡처는 `개발용 예시 데이터` 배너가 있는 **시나리오 빌드**이며, 일반 제품 빌드는 [README](../README.md)의 화면을 본다.
- 사용 가능한 캡처: [Android](evidence/M05/android) · [iOS](evidence/M05/ios) (기본 1배 13장). 전체 상태 행렬(플랫폼당 57장)은 테스트가 매번 새로 만든다.
- 표의 `원본과 같음`은 XML 수치(dp/sp/색/문구)와 구조를 옮겼다는 뜻이며 픽셀 단위 일치를 주장하지 않는다. 폰트는 두 플랫폼 모두 시스템 글꼴이다(SF Pro 미동봉).

| 화면 | 원본 근거 | 원본과 같음 | 다르거나 연결 전 | 캡처 |
|---|---|---|---|---|
| 하단 탭 | `activity_main.xml`, `nav_menu.xml`, `rec_main_nav.xml` | 홈·산책·마이페이지, 1dp `#EEEEEE` 상단선, 선택=갈색·비선택=`#7C7C7C`, 라벨 상시 표시, 아이콘 원본 변환 | 큰 글씨에서 80dp가 고정이 아니라 최소 높이로 늘어난다 | [마이페이지](evidence/M05/android/mypage-ready-light-1x.png) |
| 홈 | `fragment_home.xml`, `item_home_dog_selection.xml`, `HomeFragment` | 32sp 굵은 제목, "N 시간 전"(0시간/24시간 규칙), 100dp 반려견 원(선택=banana 링)·빈 상태 paw 타일+플러스, 7일 선형 그래프(Y축 ×1.1 올림), "14개 중 N개 획득"+banana 진행바, 카드 8dp/1dp 그림자 | 날씨·미세먼지 카드는 데이터가 없어 "연결 전" 칩만 보인다. 반려견 1마리. 오타 "흭득"은 "획득". 그래프 점은 원본의 리소스 id 버그 대신 의도한 `light_blue` | [Android](evidence/M05/android/home-ready-light-1x.png) · [iOS](evidence/M05/ios/home-ready-light-1x.png) |
| 산책 | `fragment_walk.xml`, `item_walk_dogs.xml`, `WalkFragment` | 전체 지도, 48dp 반려견 원+주황 체크박스, 220sp×60sp 반경 30 "산책하기", 진행 중 1dp 선+흰 바(거리 20sp·정지 버튼·시간) | 지도는 원본에 들어 있던 고정 샘플 이미지(줌·주변 장소·현재 위치 버튼 없음). 중단/재개 시트는 원본에 없는 추가 화면. 반려견은 기본 선택(원본은 매번 미선택). 위치 거절이어도 시간은 기록 | [Android](evidence/M05/android/walk-ready-light-1x.png) · [iOS](evidence/M05/ios/walk-ready-light-1x.png) |
| 산책 완료 + 도장 | `activity_finish.xml`, `fragment_stamp_dialog_.xml`, `FinishActivity` | "산책 완료", 230sp 둥근 경로 지도, 시간·거리, "오늘 산책을 함께한 반려견"(48dp), 한국어 날짜, 55sp "산책 끝!", 도장 다이얼로그(80% 폭·점·"확인했어요!") | 경로는 샘플 지도 위 상대 선이라 캡션으로 밝힌다. 이미 저장 중이라 "저장되지 않습니다" 종료 확인은 없다 | [Android](evidence/M05/android/finish-ready-light-1x.png) · [iOS](evidence/M05/ios/finish-ready-light-1x.png) |
| 산책 모아보기 | `activity_history.xml`, `dialog_calendar.xml`, `dialog_walk_history_map.xml` | 툴바, "{y}년 {m}월 ▼" 연·월 3×4 다이얼로그, 월 그래프(7일씩 가로 이동, 오늘 라벨 주황), 5개씩 늘어나는 카드, 썸네일→경로 팝업(시간·거리·이름 칩) | 공유 아이콘은 "연결 전". 처음에 오늘이 보이도록 스크롤한다. 카드 썸네일은 지도 스냅샷 대신 샘플 지도 위 경로 | [Android](evidence/M05/android/history-ready-light-1x.png) · [iOS](evidence/M05/ios/history-ready-light-1x.png) |
| 도장 목록·상세 | `fragment_all_stamp.xml`, `fragment_stamp_detail.xml`, `StampInfo.kt` | 2열 카드 8개("내가 얻은 도장 보러가기"·CLICK), 상세의 100dp 도장·설명·3열 획득 일시 격자, 도장 규칙 8종 | README 사진의 옛 '총 도장 수' 카드는 HEAD에 없어 HEAD를 따랐다 | [목록](evidence/M05/android/stamps-ready-light-1x.png) · [iOS](evidence/M05/ios/stamps-ready-light-1x.png) |
| 마이페이지 | `fragment_mypage.xml` | 20sp 제목+0.5dp 구분선, 18sp 섹션, 14sp 행, chevron `#D9D9D9` | 로그아웃·회원탈퇴·약관 행은 "연결 전" 안내. 행은 48dp 최소 높이(원본은 글자 높이) | [Android](evidence/M05/android/mypage-ready-light-1x.png) · [iOS](evidence/M05/ios/mypage-ready-light-1x.png) |
| 반려견 목록·상세·폼 | `fragment_pet_list.xml`, `fragment_detail_pet.xml`, `activity_add.xml`, `activity_edit_pet.xml` | 140dp 행·100dp 원, 이름*·성별*(남아/여아 칩)·나이·견종·메모, 완료/수정 완료, 삭제 확인 | 사진은 "연결 전". 반려견 1마리만 저장. 기존 v4 기록의 성별은 비어 있음 | [목록](evidence/M05/android/pets-ready-light-1x.png) · [폼](evidence/M05/android/pet-form-ready-light-1x.png) |
| 알림 리마인더 | `activity_alarm.xml` | 56dp 바+1dp 선, "추가"(주황), 빈 상태 28sp | 시간 선택기와 알림 예약이 없다("추가"는 안내) | 전체 상태 행렬에만 있음 |
| 로그인·회원가입 | `fragment_log_in.xml`, `fragment_sign_up.xml` | 입력 55dp 외곽선, 갈색 버튼, Google 버튼, 약관 3종 체크와 형식 검사 경고 | **UI만**: 시나리오 갤러리에서만 보이고 인증이 없다(전달 준비 전체 검증 전 제품 로그인 금지) | [로그인](evidence/M05/android/login-ready-light-1x.png) · [가입](evidence/M05/android/signup-ready-light-1x.png) |
| 스플래시 | `styles.xml`(`Theme.App.Starting`) | — | 이식하지 않았다(갈색 배경+로고) | — |
| 다크·큰 글씨 | `values-night/*` | 원본 night 값(`#333333`·`#CAC4D0`·흰 글자) | 대비 테스트를 통과하도록 Material 역할 색은 일부 조정. 2배 글씨에서 고정 높이는 늘어난다 | 각 화면의 `long-dark-2x` |

## 알려진 차이

- 차트 글자는 원본(MPAndroidChart)처럼 dp 크기다. 글씨 배율에 따라 커지지 않는다.
- 마이페이지 행 같은 터치 영역은 프로젝트 규칙(최소 터치 높이 48dp)에 맞춰 원본보다 크다.
- 원본의 보조 대비(`#7C7C7C` on `#F6F6F6` 약 3.9:1)는 그대로 유지했다. WCAG AA 4.5:1에는 못 미친다.
