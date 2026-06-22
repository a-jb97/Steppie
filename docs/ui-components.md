# Steppie UI 컴포넌트 계약

> 기준 문서: `docs/design-tokens.md`, `docs/product-spec.md`, `docs/accessibility-checklist.md`, `docs/localization-contract.md`, `docs/test-scenarios.md`  
> 디자인 참고: [Design System](https://www.figma.com/design/WxHeEdWiUgBFYKbrppgTz7/Steppie?node-id=26-4), [UI Screens](https://www.figma.com/design/WxHeEdWiUgBFYKbrppgTz7/Steppie?node-id=4-26)  
> 버전: 0.1.0  
> 범위: iOS/iPadOS와 Android가 공유하는 UI 토큰 사용법, 컴포넌트 의미, 상태, 반응형 레이아웃 및 접근성 계약.

## 1. 문서 우선순위와 단위

- 색상, 글꼴 크기, 간격의 실제 값은 `docs/design-tokens.md`를 단일 기준으로 사용한다.
- 이 문서는 해당 토큰을 컴포넌트에 적용하는 방식과 플랫폼 공통 상태를 정의한다.
- Figma는 구성과 시각적 의도를 확인하는 참고 자료다. Figma 변수명 또는 fallback 값이 `docs/design-tokens.md`와 다르면 문서 토큰이 우선한다.
- iOS의 수치는 pt, Android의 수치는 동일한 dp를 사용한다. 글꼴은 각각 pt와 sp를 사용한다.
- 크기는 고정 픽셀로 변환하지 않는다. safe area, Dynamic Type, Font Scale 및 멀티태스킹 창 크기를 반영한다.
- 상태명과 접근성 의미는 플랫폼 API 이름과 분리된 공통 enum으로 관리한다.

## 2. 토큰

### 2.1 Color

컴포넌트 구현은 raw hex가 아니라 다음 semantic token을 사용한다.

| 토큰 | 용도 |
|---|---|
| `color.background.primary` | 포커스 영역, 카드 내부 icon well, 기본 surface |
| `color.background.secondary` | 화면 보조 배경, 태블릿 목록 pane, 보호자 화면 surface |
| `color.text.primary` | 활동명, 버튼 외 일반 핵심 텍스트 |
| `color.text.secondary` | 화면 제목의 보조 계층, 설명, 시간, 메타데이터 |
| `color.border.subtle` | Secondary Button outline, divider, 입력/정보 카드 경계 |
| `color.focus.ring` | Primary Button 배경과 키보드·Switch 포커스 표시 |
| `color.success` | 완료 check, 완료 카드 outline, 완료 상태 텍스트 |
| `color.warning` | 보호자 경고와 주의 상태 |
| `color.danger` | Danger Button, 삭제 및 복원 전 destructive 경고 |
| `color.card.sky` | 기본/current 루틴 카드 배경 |
| `color.card.mint` | completed 루틴 카드 배경 |
| `color.card.lemon` | 루틴 카드 선택 색상 |
| `color.card.peach` | 루틴 카드 선택 색상 |
| `color.card.lavender` | 루틴 카드 선택 색상 |
| `color.card.rose` | 루틴 카드 선택 색상 |

적용 규칙:

- 카드 색상 여섯 개는 활동을 구별하는 사용자 선택값이다. `sky=current`, `mint=completed`처럼 색 자체에 도메인 상태를 저장하지 않는다.
- 완료 피드백 동안에는 원래 카드 색상과 별도로 `color.success`, check 아이콘, 완료 문구를 함께 사용한다.
- Light/Dark 값은 `docs/design-tokens.md`의 같은 token을 사용하며 컴포넌트에서 별도 hex를 분기하지 않는다.
- 텍스트와 interactive element의 대비는 WCAG AA를 충족해야 한다. 색상만으로 상태를 구분하지 않는다.

현재 Figma의 추가 변수는 새 공통 토큰이 아니다. 구현 시 다음과 같이 정식 토큰으로 해석한다.

| Figma 변수 | 공통 토큰 |
|---|---|
| `color/brand/primary` | `color.focus.ring` |
| `color/text/on-brand` | `color.background.primary` |
| `color/text/accent` | `color.focus.ring` |
| `color/surface/raised`, `color/surface/input` | `color.background.primary` |
| `color/icon/primary`, `color/border/strong` | `color.text.primary` |

### 2.2 Typography

| 토큰 | 용도 | Figma에서 대응하는 역할 |
|---|---|---|
| `font.child.cardTitle` | Focus RoutineCard 활동명 | Display, 32, Bold |
| `font.child.listTitle` | List RoutineCard 활동명 | Heading, 24, Bold |
| `font.child.progress` | 완료 수/전체 수 진행률 | Body Emphasis 역할 |
| `font.guardian.title` | 보호자 화면 및 큰 pane 제목 | Title, 28, Bold |
| `font.guardian.section` | 설정 블록, 정보 카드 제목 | Body Emphasis, 20 |
| `font.guardian.body` | 폼 값, 일반 설명, 컨트롤 | Body, 17 |
| `font.guardian.caption` | 시간, 상태, 보조 문구 | Caption, 14 |
| `font.button` | 모든 Button label | Body, 17, 강조 |

규칙:

- 크기와 weight는 `docs/design-tokens.md` 값을 사용한다. 위 Figma 역할명은 시각적 계층을 설명하는 별칭이다.
- iOS와 Android는 각 플랫폼의 system font 및 문서에 정의된 한국어 fallback을 사용한다. Figma의 Mango Ddobak은 시각 참고이며 공통 런타임 폰트 요구사항이 아니다.
- 아이 활동명은 최소 24pt/sp를 유지하고 두 줄까지 줄바꿈한다. 글자를 맞추기 위해 축소하지 않는다.
- Dynamic Type 또는 Font Scale에서 두 줄로 충분하지 않으면 카드 높이를 늘리고 뒤 요소를 아래로 reflow한다. 말줄임은 마지막 수단이며 활동명에는 사용하지 않는다.

### 2.3 Spacing

| 토큰 | 값 | 대표 용도 |
|---|---:|---|
| `space.2xs` | 4 | 아이콘과 짧은 상태의 미세 간격 |
| `space.xs` | 8 | 진행 표시 dot 간격, 밀접한 label 간격 |
| `space.sm` | 12 | 관련 컨트롤 간격, List RoutineCard 내부 세로 padding |
| `space.md` | 16 | 카드 내부 기본 padding, 목록 item 간격 |
| `space.lg` | 24 | 화면 가장자리 padding, Focus RoutineCard 내부 gap |
| `space.xl` | 32 | 태블릿 pane의 기본 세로 padding, 큰 영역 간격 |
| `space.2xl` | 48 | 화면 section 또는 디자인 보드 수준의 큰 간격 |

현재 Figma의 `spacing/*` scale은 이름이 한 단계씩 다르므로 suffix를 그대로 코드에 옮기지 않는다. 예를 들어 Figma의 24 값인 `spacing/xl`은 공통 `space.lg`에 해당한다. 숫자와 용도를 확인해 위 공통 토큰으로 변환한다.

관련 shape/layout token:

- RoutineCard: `radius.card`.
- Button: `radius.control`.
- focus 표시: `stroke.focus`와 `color.focus.ring`.
- divider: `stroke.divider`와 `color.border.subtle`.
- 아이 모드 interactive surface: `touch.child.minimum` 이상.
- 보호자 모드 control: `touch.guardian.minimum` 이상.

## 3. Button

### 3.1 종류

| 종류 | 의미 | 시각 규칙 | 사용 규칙 |
|---|---|---|---|
| `primary` | 현재 화면의 핵심 진행·저장 행동 | 배경 `color.focus.ring`, label `color.background.primary` | 하나의 action group에 원칙적으로 하나 |
| `secondary` | 취소, 뒤로, 보조 행동 | 배경 `color.background.primary`, `color.border.subtle` outline, label `color.text.primary` | Primary와 나란히 또는 독립 사용 |
| `danger` | 삭제처럼 되돌리기 어렵거나 데이터 교체를 유발하는 행동 | 배경 `color.danger`, label `color.background.primary` | 보호자 모드에서만 사용하며 별도 확인 필요 |

버튼 label은 `docs/localization-contract.md`의 `action.*` key를 사용한다. 아이 모드에는 `danger`를 노출하지 않는다.

### 3.2 크기

| 크기 | 최소 높이 | 용도 |
|---|---:|---|
| `regular` | 54 | Figma 기본 Button 및 보호자 화면의 일반 text button |
| `childLarge` | 80 | 아이가 직접 누르는 독립 Button |

- `regular`도 실제 hit area가 보호자 최소 44보다 작아지면 안 된다.
- Focus RoutineCard는 카드 전체가 primary action이므로 `childLarge` Button을 카드 안에 중복 배치하지 않는다.
- 폭은 container에 맞춰 유연하게 정한다. Figma의 180 너비는 specimen 크기이며 고정 계약이 아니다.
- 가로 padding은 `space.lg`, 버튼 사이 gap은 `space.md`, radius는 `radius.control`, label은 `font.button`을 사용한다.
- 큰 글자에서 label이 두 줄이면 높이를 늘린다. label을 잘라 54 높이를 유지하지 않는다.

### 3.3 상태

모든 Button은 `enabled`, `pressed`, `disabled`, `loading` 중 정확히 하나의 상태를 갖는다.

| 상태 | 입력과 의미 | 시각·피드백 규칙 |
|---|---|---|
| `enabled` | action 실행 가능 | 종류별 기본 색상과 label을 표시한다. |
| `pressed` | pointer가 누른 상태로 hit area 안에 있음 | `motion.cardTap.scale`을 적용하고 전체 opacity를 0.86으로 표시한다. release/cancel 즉시 해제한다. Reduce Motion에서는 scale을 생략하고 opacity 변화만 사용한다. |
| `disabled` | 선행 조건 미충족으로 action 불가 | 전체 opacity 0.40, 입력·햅틱·사운드 없음. 접근성 tree에는 사유가 필요한 경우 남기되 disabled semantic을 제공한다. |
| `loading` | action을 접수했고 중복 실행을 막는 중 | 기존 너비와 종류별 배경을 유지하고 label 위치에 progress indicator를 표시한다. 추가 입력·햅틱·사운드를 차단하며 완료·실패 후 `enabled` 또는 `disabled`로 전환한다. |

- `pressed` 전환은 최대 `motion.standard.duration` 안에 보여야 한다.
- `loading` indicator는 label과 같은 전경색을 사용한다. 무한 loading에는 사용자 취소 또는 명확한 종료 경로가 있어야 한다.
- `loading` 접근성 이름은 원래 action을 유지하고 상태를 현지화된 “진행 중”으로 제공한다. 상태를 반복 announcement하지 않는다.
- 포커스 ring은 상태 효과보다 바깥에 그리고 다른 색·outline으로 식별 가능하게 유지한다.

## 4. RoutineCard

`RoutineCard`는 같은 routine 정보를 보여주는 공통 모델이며, 화면 목적에 따라 `focus`와 `list` presentation을 사용한다. 두 presentation은 같은 `routineId`, localized title, visual asset, user-selected card color, schedule 및 completion 상태를 입력으로 받는다.

### 4.1 Focus RoutineCard

구성 요소:

1. 카드 전체 interactive container.
2. 128x128 상세 아이콘 또는 사용자 사진을 보여주는 visual slot.
3. `font.child.cardTitle`을 사용하는 localized 활동명.
4. 현재 행동 또는 완료 결과를 설명하는 짧은 hint/status 문구.
5. 완료 시 check 같은 비색상 상태 표시.

크기와 배치:

- 휴대폰에서는 사용 가능 폭을 채우되 `focusCard.phone.maxWidth`를 넘지 않는다.
- 태블릿에서는 `focusCard.tablet.maxWidth`를 넘지 않는다.
- Figma 기준 phone specimen은 345x448, tablet landscape specimen은 640x520이다. 폭은 위 max token을 따르고 높이는 콘텐츠와 글자 크기에 따라 늘어날 수 있다.
- 내부 gap은 `space.lg`, radius는 `radius.card`를 사용한다. 카드 전체 hit area는 `touch.child.minimum` 이상이다.

상태:

| 상태 | 내용 | 동작 |
|---|---|---|
| `current` | 원래 카드 색상, 활동명, 완료 방법 hint | 아이 모드 포커스 뷰에서 한 번 탭하면 해당 routine을 완료한다. |
| `completed` | 활동명과 명시적 완료 문구, check, `color.success` outline | 완료 피드백 동안만 표시한 뒤 다음 미완료 routine 또는 전체 완료 화면으로 이동한다. 추가 탭으로 중복 완료하지 않는다. |

- `current`만 completion action을 가진다.
- `completed`는 색상 변경만으로 표현하지 않는다.
- 완료 후 Undo는 카드 상태를 직접 토글하는 기능이 아니라 별도 Undo Snackbar action으로 제공한다.

### 4.2 List RoutineCard

구성 요소:

1. 최소 높이 104의 카드 전체 navigation container.
2. 64x64 icon well 안의 48x48 아이콘 또는 사진 thumbnail.
3. `font.child.listTitle`의 localized 활동명. 최대 두 줄이며 남은 가로 공간을 우선 사용한다.
4. 순서, 예정 시각 또는 상태를 보여주는 meta label.
5. check 또는 disclosure indicator를 보여주는 trailing status.

상태:

| 공통 상태 | Figma variant | 필수 표시 | 카드 탭 결과 |
|---|---|---|---|
| `current` | `Current` | “지금” 문구와 disclosure indicator | 해당 routine의 Focus View로 이동 |
| `completed` | `Completed` | “완료” 문구와 check, 접근성 완료 상태 | 해당 routine의 Focus View로 이동하되 목록에서 완료 처리하지 않음 |
| `upcoming` | `Scheduled` | 순서와 예정 시각이 있으면 시각, 없으면 순서만 표시; disclosure indicator | 해당 routine의 Focus View로 이동 |

- Figma의 `Scheduled`는 데이터 모델의 “예정 시각 존재”가 아니라 아직 current/completed가 아닌 `upcoming` presentation 이름으로 해석한다.
- 사용자 선택 카드 색상은 모든 상태에서 유지할 수 있다. check, 상태 문구 및 접근성 값이 실제 상태의 기준이다.
- 목록 카드는 navigation만 수행한다. 목록에서 completion action을 실행하지 않는다.
- 태블릿 왼쪽 pane에서는 폭 320을 기준으로 보이지만 pane 너비에 따라 유연하게 줄어든다. icon well과 trailing status를 먼저 보존하고 title 영역을 reflow한다.

### 4.3 상태 계산

한 routine의 presentation 상태는 다음 순서로 결정한다.

1. 오늘의 DailyLog가 completed이면 `completed`.
2. completed가 아니고 현재 포커스 대상이면 `current`.
3. 그 외 활성 routine은 `upcoming`.

진행률은 `{completed}/{total}` 텍스트와 시각 표시를 함께 제공한다. 접근성 값은 `a11y.progress`를 사용하며 색상 dot만 읽지 않는다.

## 5. 반응형 레이아웃

### 5.1 공통 원칙

- safe area 안에서 배치하고 화면 가장자리 padding은 아이 모드 `screen.child.padding`, 보호자 모드 `screen.guardian.padding`을 사용한다.
- 기기 이름만으로 고정 좌표를 선택하지 않는다. 방향과 실제 사용 가능 크기를 함께 판단한다.
- 콘텐츠 폭이 줄면 title/meta를 reflow하고 세로 scroll을 허용한다. touch target, 글꼴 최소 크기, icon 크기를 먼저 줄이지 않는다.
- 시스템 bar 높이와 Figma의 393x852 canvas 좌표는 레이아웃 상수가 아니다.

### 5.2 휴대폰

- iPhone과 Android phone은 portrait만 지원하며 landscape는 portrait로 고정한다.
- 아이 포커스 화면은 single pane이다. 진행률, Focus RoutineCard, 목록 진입 동작을 세로로 배치한다.
- Focus RoutineCard는 중앙 정렬하고 사용 가능 폭을 채우되 `focusCard.phone.maxWidth`를 넘지 않는다.
- 루틴 목록은 한 열이며 List RoutineCard가 사용 가능 폭을 채운다.

### 5.3 태블릿 세로 및 좁은 멀티태스킹 창

- single pane을 사용한다.
- Focus RoutineCard를 중앙 정렬하고 `focusCard.tablet.maxWidth`를 넘지 않는다.
- 큰 빈 공간을 채우기 위해 카드나 글꼴을 무제한 확대하지 않는다.
- 태블릿이라도 멀티태스킹으로 가용 폭이 split layout 최소 폭보다 작으면 이 규칙으로 fallback한다.

### 5.4 태블릿 가로 Split View

- 충분한 가용 폭에서는 왼쪽 Routine List와 오른쪽 Focus View를 동시에 표시한다.
- 왼쪽 pane은 `split.list.width`인 360을 기준으로 하고 `color.background.secondary`를 사용한다.
- 오른쪽 pane은 남은 폭을 채우며 Focus RoutineCard를 중앙 정렬하고 `focusCard.tablet.maxWidth`인 640을 넘지 않는다.
- pane 경계에는 `stroke.divider`와 `color.border.subtle`을 사용한다.
- split layout 최소 폭은 `split.list.width + stroke.divider + (2 * space.xl) + focusCard.phone.maxWidth`, 즉 905pt/dp-equivalent다. 그보다 좁으면 태블릿 single pane으로 전환한다.
- 왼쪽 목록에서 항목을 선택하면 오른쪽 focus 내용과 선택 상태만 갱신한다. 선택 자체가 완료를 발생시키지 않는다.
- 시각 순서와 스크린 리더 traversal은 왼쪽 목록에서 오른쪽 포커스 영역 순서로 일치해야 한다.

## 6. 접근성 요구사항

### 6.1 이름, 역할, 상태

- Focus RoutineCard는 하나의 Button으로 노출하고 이름에 routine title, 값/상태에 current 또는 completed를 제공한다.
- List RoutineCard는 하나의 navigation Button으로 노출한다. 내부의 장식 아이콘, 반복 title, disclosure indicator는 개별 focus 대상에서 제외한다.
- 카드의 사진이나 아이콘이 활동명과 같은 의미라면 장식 요소로 처리해 중복 낭독하지 않는다. 사진에 별도 정보가 꼭 필요하면 카드의 통합 접근성 이름에 포함한다.
- 완료 상태는 `a11y.status.completed`, 미완료 상태는 `a11y.status.notCompleted`를 사용한다.
- 진행률은 `a11y.progress` 형식으로 전체 수와 완료 수를 읽는다.
- Button은 종류가 아니라 실제 action label로 이름을 제공하고 disabled/loading 상태를 semantic으로 노출한다.
- 완료, 오류 및 중요한 loading 결과는 VoiceOver announcement 또는 TalkBack live region으로 한 번 전달한다.

### 6.2 터치와 입력

- 아이 모드의 모든 interactive surface는 최소 80x80 pt/dp-equivalent다.
- 보호자 컨트롤은 최소 44x44 pt/dp-equivalent이며 Figma의 48 `size/touch-min`은 보호자용으로만 허용한다.
- Focus RoutineCard completion은 짧은 탭 한 번으로 실행한다. List RoutineCard 탭은 navigation만 실행한다.
- Switch Control/Switch Access, 키보드 및 외부 입력으로 primary action에 도달할 수 있어야 한다.
- 키보드 또는 switch focus에는 `color.focus.ring`과 `stroke.focus`를 사용하며 pressed/completed outline과 구분한다.
- reorder와 swipe에는 접근 가능한 대체 action 또는 플랫폼 reorder semantic을 제공한다.

### 6.3 글자, 대비 및 확대

- iOS Dynamic Type과 Android Font Scale을 모두 지원한다.
- 최대 접근성 글자 크기에서 활동명, Button label 및 상태 문구를 자르지 않는다.
- 글자가 커지면 고정 높이를 해제하고 세로로 reflow한다. 주요 action은 scroll 후에도 도달 가능해야 한다.
- 일반 텍스트와 배경은 WCAG AA를 만족한다. interactive outline과 상태 indicator도 가능한 범위에서 AA를 만족한다.
- Dark mode와 시스템 대비 설정에서도 raw color로 토큰을 덮어쓰지 않는다.

### 6.4 모션, 소리 및 인지

- 완료 피드백은 0.4초 이내 시작하고 1.5초 이내 끝낸다.
- Reduce Motion 또는 animation scale 0에서는 scale, 이동, 장식 애니메이션을 제거하고 check·문구·outline으로 상태를 유지한다.
- flashing content를 사용하지 않는다.
- 소리와 햅틱/진동 없이도 모든 상태를 이해할 수 있어야 하며 사용자의 feedback 설정을 따른다.
- 아이 포커스 화면에는 한 번에 하나의 current routine만 제시한다.
- loading 또는 자동 전환이 아이의 task completion을 막는 auto-dismiss를 만들면 안 된다.

## 7. 플랫폼 구현 체크리스트

- 같은 공통 state 입력이 iOS와 Android에서 같은 label, action 가능 여부 및 접근성 상태를 만드는가.
- raw hex, Figma spacing suffix 또는 플랫폼 기본 색을 공통 token 대신 사용하지 않았는가.
- List RoutineCard 탭이 completion을 발생시키지 않는가.
- Button loading 중 중복 action이 차단되는가.
- 최대 글자 크기에서 카드와 Button이 확장되고 모든 텍스트와 action에 도달 가능한가.
- VoiceOver와 TalkBack이 routine title, 상태, 진행률을 중복 없이 읽는가.
- Reduce Motion/animation scale 0에서도 pressed와 completed 상태가 구분되는가.
- phone portrait, tablet portrait, tablet landscape 및 좁은 tablet 멀티태스킹 창에서 규칙에 맞는 pane 구성이 선택되는가.
