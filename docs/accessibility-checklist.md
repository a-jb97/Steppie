# Steppie 접근성 체크리스트

> 기준 문서: `docs/planning/steppie_planning_document.md`  
> 버전: 0.1.0  
> 범위: VoiceOver, TalkBack, 시각, 운동/터치, 청각, 인지, 모션 공통 검증 항목.

## 1. 출시 기준

출시 전 이 체크리스트의 필수 항목은 다음 환경에서 모두 통과해야 한다.

- iPhone portrait.
- iPad portrait 및 landscape.
- Android phone portrait.
- Android tablet portrait 및 landscape.

Phone landscape는 의도적으로 미지원이며 portrait lock 상태여야 한다.

## 2. 스크린 리더

| ID | 요구사항 | iOS | Android | 필수 |
|---|---|---|---|---:|
| SR-01 | 모든 루틴 카드는 루틴 제목을 포함한 접근성 이름을 가진다 | VoiceOver label | contentDescription | 예 |
| SR-02 | 완료 상태는 색상만이 아니라 음성으로도 전달된다 | value/label | stateDescription/contentDescription | 예 |
| SR-03 | 진행률은 완료 수와 전체 수로 읽힌다 | accessibilityLabel | contentDescription | 예 |
| SR-04 | 포커스 순서는 시각적 순서와 일치한다 | 필요 시 sort priority | 필요 시 traversal order | 예 |
| SR-05 | 보호자 PIN keypad 버튼은 숫자를 명확히 읽는다 | labels | contentDescription | 예 |
| SR-06 | 아이콘만 있는 컨트롤은 라벨을 가진다 | labels | contentDescription | 예 |
| SR-07 | modal/sheet 진입 시 포커스가 제목 또는 첫 필드로 이동한다 | focus management | focus requester | 예 |
| SR-08 | 오류 메시지는 스크린 리더에 전달된다 | accessibility announcement | live region/snackbar announcement | 예 |

## 3. 시각 접근성

| ID | 요구사항 | 필수 |
|---|---|---:|
| V-01 | 텍스트 대비는 WCAG AA를 만족한다 | 예 |
| V-02 | 비텍스트 interactive element도 가능한 범위에서 WCAG AA 대비를 만족한다 | 예 |
| V-03 | 색상만으로 상태를 전달하지 않는다 | 예 |
| V-04 | 아이 카드 제목은 큰 글자 설정에서도 읽을 수 있다 | 예 |
| V-05 | Dark mode는 토큰에 정의된 색상을 사용한다 | 예 |
| V-06 | 키보드/switch navigation의 활성 포커스 표시가 보인다 | 예 |
| V-07 | 사진 또는 아이콘이 라벨을 가리지 않는다 | 예 |

## 4. Dynamic Text

| ID | 요구사항 | 필수 |
|---|---|---:|
| DT-01 | iOS Dynamic Type을 아이/보호자 모드 모두에서 지원한다 | 예 |
| DT-02 | Android Font Scale을 아이/보호자 모드 모두에서 지원한다 | 예 |
| DT-03 | 텍스트는 잘리는 대신 줄바꿈할 수 있다 | 예 |
| DT-04 | 텍스트가 커져도 버튼은 터치 가능해야 한다 | 예 |
| DT-05 | PIN keypad는 큰 글자 설정에서도 사용할 수 있다 | 예 |

## 5. 운동/터치 접근성

| ID | 요구사항 | 필수 |
|---|---|---:|
| M-01 | 아이 모드 touch target은 최소 80x80 pt/dp-equivalent다 | 예 |
| M-02 | 보호자 컨트롤은 최소 44x44 pt/dp-equivalent다 | 예 |
| M-03 | 완료 동작은 포커스 뷰에서만 짧은 탭 1회로 실행된다 | 예 |
| M-04 | 완료 후 설정된 시간 동안 undo가 가능하다 | 예 |
| M-05 | 아이 모드에는 destructive action이 없다 | 예 |
| M-06 | drag-and-drop reorder에는 접근 가능한 대안 또는 플랫폼 reorder semantic이 있다 | 예 |
| M-07 | Switch Control/Switch Access로 primary action에 도달할 수 있다 | 예 |

## 6. 인지 접근성

| ID | 요구사항 | 필수 |
|---|---|---:|
| C-01 | 아이 포커스 뷰는 하나의 주요 활동만 보여준다 | 예 |
| C-02 | 같은 동작은 항상 같은 결과를 만든다 | 예 |
| C-03 | 아이의 task completion을 막는 auto-dismiss가 없다 | 예 |
| C-04 | 보호자 모드는 명시적 완료 또는 3분 미조작 시 종료된다 | 예 |
| C-05 | 완료 피드백은 0.4초 이내 시작된다 | 예 |
| C-06 | 완료 피드백은 1.5초 이내 종료된다 | 예 |
| C-07 | 보호자 모드의 오류 상태는 다음 행동을 설명한다 | 예 |

## 7. 청각과 음성

| ID | 요구사항 | 필수 |
|---|---|---:|
| A-01 | TTS는 켜고 끌 수 있다 | 예 |
| A-02 | 효과음은 켜고 끌 수 있다 | 예 |
| A-03 | 모든 음성 안내에는 화면 텍스트 대응물이 있다 | 예 |
| A-04 | 플랫폼이 허용하는 범위에서 TTS 속도와 볼륨 설정을 따른다 | 예 |
| A-05 | TTS가 자기 자신과 겹치지 않는다 | 예 |

## 8. 모션과 감각 자극

| ID | 요구사항 | 필수 |
|---|---|---:|
| MS-01 | iOS Reduce Motion은 장식성 애니메이션을 비활성화한다 | 예 |
| MS-02 | Android animation scale 0은 장식성 애니메이션을 비활성화한다 | 예 |
| MS-03 | 피드백 강도는 `quiet` 또는 `off`로 설정할 수 있다 | 예 |
| MS-04 | 햅틱/진동은 켜고 끌 수 있다 | 예 |
| MS-05 | flashing content를 사용하지 않는다 | 예 |

## 9. 플랫폼 검증 메모

iOS:

- VoiceOver로 테스트한다.
- Dynamic Type을 가장 큰 접근성 크기로 설정해 테스트한다.
- Reduce Motion을 켜고 테스트한다.
- Switch Control로 루틴 완료까지 도달 가능한지 테스트한다.

Android:

- TalkBack으로 테스트한다.
- Font Size와 Display Size를 크게 설정해 테스트한다.
- Animator duration scale을 off로 설정해 테스트한다.
- Switch Access로 루틴 완료까지 도달 가능한지 테스트한다.

## 10. 통과 기준

기능은 다음 조건을 만족해야 완료로 본다.

- 양 플랫폼에서 관련 체크리스트 항목을 통과한다.
- 플랫폼별 예외가 있으면 구현 PR 또는 작업 결과에 기록한다.
- 예외가 공통 계약의 사용자 경험 의미를 바꾸지 않는다.
