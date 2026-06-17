# Steppie 디자인 토큰

> 기준 문서: `docs/planning/steppie_planning_document.md`  
> 버전: 0.1.0  
> 범위: SwiftUI와 Jetpack Compose가 공유할 시각, 레이아웃, 모션, 아이콘 명명 규칙.

## 1. 원칙

- 아이 모드 UI는 단순하고 예측 가능하며 자극이 낮아야 한다.
- 카드는 단색만 사용한다. 패턴 배경은 사용하지 않는다.
- 텍스트와 컨트롤은 WCAG AA 대비를 만족해야 한다.
- 아이 모드 최소 터치 영역은 80x80 pt/dp-equivalent다.
- Dynamic Type과 Android Font Scale을 지원해야 한다.

## 2. 색상 토큰

코드에서는 semantic token을 사용한다. Hex 값은 기본 light mode 값이다.

| 토큰 | Light | Dark | 용도 |
|---|---|---|---|
| `color.background.primary` | `#FFFFFF` | `#121212` | 기본 배경 |
| `color.background.secondary` | `#F5F7FA` | `#1D1F22` | 보호자 화면 surface |
| `color.text.primary` | `#1F2933` | `#F4F6F8` | 기본 텍스트 |
| `color.text.secondary` | `#52606D` | `#C9D1D9` | 보조 텍스트 |
| `color.border.subtle` | `#D9E2EC` | `#3A3F45` | 구분선, outline |
| `color.focus.ring` | `#2563EB` | `#8AB4F8` | 포커스 표시 |
| `color.success` | `#2F855A` | `#68D391` | 완료 상태 |
| `color.warning` | `#B7791F` | `#F6AD55` | 경고 |
| `color.danger` | `#C53030` | `#FC8181` | 보호자 destructive action |
| `color.card.sky` | `#D8ECFF` | `#17456B` | 루틴 카드 |
| `color.card.mint` | `#DDF7E8` | `#1F5A3D` | 루틴 카드 |
| `color.card.lemon` | `#FFF3B8` | `#665200` | 루틴 카드 |
| `color.card.peach` | `#FFE0CC` | `#6B3B24` | 루틴 카드 |
| `color.card.lavender` | `#E8DEFF` | `#47306B` | 루틴 카드 |
| `color.card.rose` | `#FFDCE5` | `#6B263A` | 루틴 카드 |

필수 조합:

- Light mode 카드 라벨은 대비가 실패하지 않는 한 `color.text.primary`를 사용한다.
- Dark mode 카드 라벨은 `color.text.primary`를 사용한다.
- 플랫폼은 시스템 접근성 대비 설정 적용 후에도 contrast를 검증해야 한다.

## 3. 타이포그래피 토큰

| 토큰 | 크기 | 굵기 | 용도 |
|---|---:|---|---|
| `font.child.cardTitle` | 32 | bold | 포커스 카드 활동명 |
| `font.child.listTitle` | 24 | semibold | 루틴 목록 카드 라벨 |
| `font.child.progress` | 22 | semibold | 진행률 텍스트 |
| `font.guardian.title` | 28 | bold | 보호자 화면 제목 |
| `font.guardian.section` | 20 | semibold | 섹션 제목 |
| `font.guardian.body` | 17 | regular | 본문/컨트롤 텍스트 |
| `font.guardian.caption` | 14 | regular | 보조 텍스트 |
| `font.button` | 17 | semibold | 버튼 |

폰트 패밀리:

- iOS: system font, Apple SD Gothic Neo와 호환되는 한국어 fallback.
- Android: system font, Noto Sans KR와 호환되는 한국어 fallback.

규칙:

- 아이 카드 라벨의 최소 렌더링 크기는 24 pt/sp다.
- 루틴 카드 텍스트는 두 줄까지 줄바꿈할 수 있다.
- 텍스트를 한 줄에 강제로 넣기 위해 접근성 최소 크기 아래로 줄이지 않는다.

## 4. 간격 토큰

| 토큰 | 값 | 용도 |
|---|---:|---|
| `space.2xs` | 4 | 미세 간격 |
| `space.xs` | 8 | 촘촘한 gap |
| `space.sm` | 12 | 관련 컨트롤 사이 |
| `space.md` | 16 | 기본 간격 |
| `space.lg` | 24 | 섹션 간격 |
| `space.xl` | 32 | 화면 padding |
| `space.2xl` | 48 | 큰 아이 모드 간격 |

## 5. Radius와 Stroke

| 토큰 | 값 | 용도 |
|---|---:|---|
| `radius.card` | 16 | 아이 루틴 카드 |
| `radius.control` | 12 | 버튼, 입력 |
| `radius.sheet` | 24 | modal/sheet 상단 모서리 |
| `stroke.focus` | 3 | 키보드/switch 포커스 |
| `stroke.divider` | 1 | 구분선 |

## 6. 레이아웃 토큰

| 토큰 | 값 | 설명 |
|---|---:|---|
| `touch.child.minimum` | 80 | pt/dp-equivalent |
| `touch.guardian.minimum` | 44 | pt/dp-equivalent |
| `focusCard.phone.maxWidth` | 480 | 큰 phone width에서 중앙 정렬 |
| `focusCard.tablet.maxWidth` | 640 | tablet portrait |
| `split.list.width` | 360 | tablet landscape 루틴 목록 |
| `screen.child.padding` | 24 | phone 아이 모드 |
| `screen.guardian.padding` | 20 | phone 보호자 모드 |

화면 방향:

- Phone landscape는 지원하지 않고 portrait로 고정한다.
- Tablet landscape는 좌측 루틴 목록, 우측 포커스 카드의 split view를 사용한다.

## 7. 모션 토큰

| 토큰 | 값 | 용도 |
|---|---:|---|
| `motion.complete.startWithin` | 0.4s | 완료 피드백 시작 제한 |
| `motion.complete.maxDuration` | 1.5s | 완료 피드백 전체 길이 |
| `motion.cardTap.scale` | 0.96 | press feedback scale |
| `motion.standard.duration` | 0.2s | 일반 UI transition |

Reduce Motion:

- iOS Reduce Motion 또는 Android animation scale 0이 켜져 있으면 장식성 모션을 제거한다.
- 상태 변화는 모션만이 아니라 색상, 텍스트, 레이아웃으로도 인지 가능해야 한다.

## 8. 피드백 토큰

| 강도 | 애니메이션 | 사운드 | 햅틱/진동 |
|---|---|---|---|
| `strong` | 전체 완료 애니메이션 | 설정이 켜져 있으면 on | medium |
| `normal` | scale + check feedback | 설정이 켜져 있으면 on | light |
| `quiet` | check feedback만 | off | off |
| `off` | 장식성 피드백 없음 | off | off |

## 9. 내장 아이콘 이름

아이콘 라이브러리는 안정적인 이름을 제공해야 한다. 초기 필수 범주:

| 범주 | 필수 이름 |
|---|---|
| Morning | `wake-up`, `wash-face`, `brush-teeth`, `get-dressed`, `breakfast`, `pack-bag` |
| School | `school`, `book`, `pencil`, `lunch`, `playground`, `bus` |
| Bedtime | `bath`, `pajamas`, `story-book`, `toilet`, `sleep`, `star` |
| General | `home`, `meal`, `snack`, `medicine`, `walk`, `therapy`, `music`, `art`, `clean-up`, `timer` |

규칙:

- 아이콘 이름은 lowercase kebab-case다.
- 내장 아이콘은 단순한 flat 스타일과 명확한 외곽선을 가져야 한다.
- 출시에는 내장 아이콘 60개 이상이 필요하다.

## 10. 컴포넌트 최소 요구사항

| 컴포넌트 | 요구사항 |
|---|---|
| Focus card | 큰 아이콘/이미지, 활동 라벨, 카드 전체 tap target |
| Routine list card | 순서, 아이콘/이미지, 제목, 완료 상태 |
| Progress | 완료/전체 상태가 보이며 색상 단독 표현 금지 |
| Undo snackbar | 메시지와 undo action, 설정 시간 동안 표시 |
| PIN keypad | 큰 숫자 키, 숨겨진 작은 컨트롤 없음 |
| Guardian form controls | 라벨은 항상 보이고 플랫폼 접근성 라벨 설정 |
