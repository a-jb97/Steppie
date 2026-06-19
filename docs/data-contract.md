# Steppie 데이터 계약

> 기준 문서: `docs/planning/steppie_planning_document.md`  
> 버전: 0.1.0  
> 범위: iOS SwiftData와 Android Room은 같은 데이터 의미, 기본값, 검증 규칙을 구현한다.

## 1. 공통 규칙

- 영속 저장되는 모든 ID는 UUID v4 문자열을 사용한다.
- 모든 타임스탬프는 timezone offset을 포함한 ISO 8601 문자열을 사용한다.
- 로컬 날짜는 `YYYY-MM-DD` 형식을 사용한다.
- 로컬 시간은 24시간제 `HH:mm` 형식을 사용한다.
- 화면 표시 텍스트는 플랫폼 리소스 ID가 아니라 `LocalizedText`로 저장한다.
- 진행 기록에 남을 수 있는 사용자 생성 데이터는 물리 삭제보다 소프트 삭제를 우선한다.
- 양 플랫폼에 영향을 주는 데이터 변경은 구현보다 이 문서를 먼저 수정한다.

## 2. 타입 매핑

| 계약 타입 | iOS | Android | JSON |
|---|---|---|---|
| UUID | `UUID` | `String` 또는 UUID wrapper | string |
| DateTime | `Date` | `Instant` 또는 epoch millis wrapper | ISO 8601 string |
| LocalDate | `DateComponents` 또는 string wrapper | `LocalDate` 또는 string wrapper | `YYYY-MM-DD` |
| LocalTime | string wrapper | string wrapper | `HH:mm` |
| Bool | `Bool` | `Boolean` | boolean |
| Int | `Int` | `Int` | number |
| String | `String` | `String` | string |

## 3. 엔티티

### 3.1 RoutineSet

RoutineSet은 순서가 있는 루틴 카드 묶음이다. 초기 설정 이후에는 활성 RoutineSet이 정확히 하나 있어야 한다.

| 필드 | 타입 | 필수 | 기본값 | 설명 |
|---|---:|---:|---|---|
| `id` | UUID | 예 | generated | 백업/복원 후에도 유지되는 ID |
| `name` | LocalizedText | 예 | 없음 | 사용자에게 보이는 루틴 세트 이름 |
| `isActive` | Bool | 예 | `false` | 활성 세트는 하나만 허용 |
| `createdAt` | DateTime | 예 | now | 생성 시각 |
| `updatedAt` | DateTime | 예 | now | 하위 루틴 순서/내용 변경 시 갱신 |
| `deletedAt` | DateTime? | 아니오 | `null` | 소프트 삭제 표시 |

검증 규칙:

- 활성 RoutineSet은 소프트 삭제될 수 없다.
- 빈 RoutineSet은 생성/편집 중에만 허용한다.
- 사용자에게 보이는 목록에서는 `deletedAt != null`인 레코드를 숨긴다.

### 3.2 Routine

Routine은 아이 모드에 표시되는 단일 활동 카드다.

| 필드 | 타입 | 필수 | 기본값 | 설명 |
|---|---:|---:|---|---|
| `id` | UUID | 예 | generated | 안정적인 루틴 ID |
| `routineSetId` | UUID | 예 | 없음 | 상위 RoutineSet |
| `titleKey` | String? | 아니오 | `null` | 템플릿 기반 루틴의 내장 현지화 키 |
| `title` | LocalizedText | 예 | 없음 | 사용자가 수정할 수 있는 제목 |
| `icon` | IconRef | 예 | default icon | 내장 아이콘 또는 로컬 사진 참조 |
| `colorToken` | String | 예 | `color.card.sky` | 디자인 토큰에 존재해야 함 |
| `order` | Int | 예 | next index | 세트 안의 0 기반 순서 |
| `scheduledTime` | LocalTime? | 아니오 | `null` | 선택 예정 시각 |
| `isActive` | Bool | 예 | `true` | 비활성 루틴은 아이 흐름에서 숨김 |
| `createdAt` | DateTime | 예 | now | 생성 시각 |
| `updatedAt` | DateTime | 예 | now | 마지막 수정 시각 |
| `deletedAt` | DateTime? | 아니오 | `null` | 소프트 삭제 표시 |

검증 규칙:

- `order`는 같은 세트의 활성/미삭제 루틴 사이에서 유일해야 한다.
- `title`은 최소 하나 이상의 비어 있지 않은 locale 값을 가져야 한다.
- `colorToken`은 WCAG AA를 만족하는 카드 색상 조합으로 해석되어야 한다.
- `scheduledTime`은 알림 설정이 켜진 경우에만 알림을 생성할 수 있다.

### 3.3 DailyLog

DailyLog는 특정 로컬 날짜의 루틴 상태를 기록한다.

| 필드 | 타입 | 필수 | 기본값 | 설명 |
|---|---:|---:|---|---|
| `id` | UUID | 예 | generated | 기록 행 ID |
| `date` | LocalDate | 예 | today | 기기 로컬 날짜 |
| `routineId` | UUID | 예 | 없음 | 완료 당시의 Routine |
| `routineSetId` | UUID | 예 | 없음 | 상위 세트 스냅샷 키 |
| `status` | LogStatus | 예 | `undone` | `completed` 또는 `undone` |
| `completedAt` | DateTime? | 아니오 | `null` | 완료 상태일 때 필수 |
| `createdAt` | DateTime | 예 | now | 행 생성 시각 |
| `updatedAt` | DateTime | 예 | now | 행 수정 시각 |

검증 규칙:

- `date + routineId` 조합당 DailyLog는 최대 하나다.
- `status == completed`이면 `completedAt`은 null이 아니어야 한다.
- `status == undone`이면 `completedAt`은 null이어야 한다.
- 하루 리셋은 UTC 날짜가 아니라 로컬 날짜 기준이다.

### 3.4 AppSettings

AppSettings는 기기 로컬 동작 설정을 저장한다. 플랫폼 전용 권한 상태를 제외하고 백업에 포함한다.

| 필드 | 타입 | 필수 | 기본값 | 설명 |
|---|---:|---:|---|---|
| `id` | String | 예 | `singleton` | 단일 행 |
| `guardianPinHash` | String | 설정 후 예 | 없음 | 원본 PIN 저장 금지 |
| `recoveryCodeHash` | String | 설정 후 예 | 없음 | 6자리 복구 코드 해시 |
| `feedbackIntensity` | FeedbackIntensity | 예 | `normal` | `strong`, `normal`, `quiet`, `off` |
| `soundEnabled` | Bool | 예 | `true` | 효과음 |
| `ttsEnabled` | Bool | 예 | `true` | 음성 안내 |
| `ttsRate` | Double | 예 | `1.0` | 범위 `0.5...1.5` |
| `ttsVolume` | Double | 예 | `1.0` | 범위 `0.0...1.0` |
| `hapticEnabled` | Bool | 예 | `true` | 햅틱/진동 |
| `undoDurationSeconds` | Int | 예 | `5` | 허용값: `3`, `5`, `10` |
| `notificationLeadTimes` | Int[] | 예 | `[10, 5]` | 예정 시각 전 알림 시간(분) |
| `quietHoursStart` | LocalTime? | 아니오 | `null` | 선택 방해 금지 시작 |
| `quietHoursEnd` | LocalTime? | 아니오 | `null` | 선택 방해 금지 종료 |
| `locale` | String? | 아니오 | `null` | null이면 시스템 locale 사용 |
| `createdAt` | DateTime | 예 | now | 생성 시각 |
| `updatedAt` | DateTime | 예 | now | 수정 시각 |

보안 규칙:

- PIN은 해시 전 항상 정확히 4자리 숫자다.
- 복구 코드는 해시 전 항상 정확히 6자리 숫자다.
- 플랫폼에 적합한 비밀번호 해시 또는 Keychain/Keystore 기반 비밀 저장 방식을 사용한다.

## 4. 값 객체

### LocalizedText

```json
{
  "ko": "양치하기",
  "en": "Brush teeth"
}
```

규칙:

- Locale 키는 BCP 47 언어 태그를 사용한다.
- 내장 템플릿은 `ko`와 `en`을 필수로 가진다.
- 사용자 생성 커스텀 텍스트는 현재 앱 locale만 저장할 수 있다.
- fallback 순서: 현재 앱 locale -> 시스템 언어 -> `ko` -> 첫 번째 사용 가능 값.

### IconRef

```json
{
  "type": "builtin",
  "name": "toothbrush"
}
```

```json
{
  "type": "photo",
  "localAssetId": "8A5B6B1A-1C47-4F43-A4A3-8B8A64B56921",
  "backupAssetName": "routine-photo-8A5B6B1A.jpg"
}
```

규칙:

- `type`은 `builtin` 또는 `photo`다.
- 내장 아이콘 이름은 `docs/design-tokens.md`를 따른다.
- 사진 에셋은 로컬 우선으로 동작하며, 가능하면 백업에 포함한다.

## 5. Enum

| Enum | 값 |
|---|---|
| `LogStatus` | `completed`, `undone` |
| `FeedbackIntensity` | `strong`, `normal`, `quiet`, `off` |
| `Mode` | `child`, `guardian` |

## 6. 파생 상태

다음 값은 원천 데이터로 저장하지 않고 파생한다.

- `isCompletedToday`: 현재 로컬 날짜의 DailyLog에서 계산한다.
- `currentRoutine`: 활성 RoutineSet 안에서 오늘 완료되지 않은 첫 번째 활성 루틴.
- `progressCount`: 오늘 완료된 활성 루틴 수.
- `progressTotal`: 활성 RoutineSet 안의 활성/미삭제 루틴 수.

## 7. 초기 데이터 요구사항

양 플랫폼은 같은 초기 데이터를 제공해야 한다.

- 내장 템플릿 3종: morning, school, bedtime.
- 출시 전 내장 아이콘 60개 이상.
- 모든 내장 템플릿 루틴에 한국어/영어 라벨 제공.
- 온보딩 중 기본 활성 RoutineSet은 morning 템플릿으로 만들 수 있다.

## 8. 마이그레이션 규칙

- 스키마 버전은 `1`에서 시작한다.
- 마이그레이션은 결정적이고 오프라인에서 가능해야 한다.
- 하위 호환 필드는 nullable 필드 또는 기본값을 사용한다.
- breaking schema change는 `backup-contract.md`를 갱신하고 복원 테스트를 추가해야 한다.
