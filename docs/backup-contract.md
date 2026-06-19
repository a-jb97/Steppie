# Steppie 백업 계약

> 기준 문서: `docs/planning/steppie_planning_document.md`  
> 버전: 0.1.0  
> 범위: iCloud/CloudKit과 Google Drive에서 공통으로 사용할 백업/복원 포맷.

## 1. 목표

- 사용자는 같은 플랫폼에서 Steppie 데이터를 복원할 수 있다.
- 백업 내용은 iOS와 Android에서 같은 의미 구조를 가진다.
- 백업은 별도 커스텀 서버 없이 동작한다.
- 백업에는 원본 PIN, 원본 복구 코드, 분석 식별자, 플랫폼 권한 상태를 포함하지 않는다.

## 2. 백업 패키지

백업 패키지는 zip archive다.

```text
steppie-backup-v1.zip
├── manifest.json
├── data.json
└── assets/
    └── routine-photo-{uuid}.jpg
```

클라우드 위치:

| 플랫폼 | 저장소 |
|---|---|
| iOS / iPadOS | iCloud / CloudKit private user scope |
| Android | Google Drive app data folder 또는 사용자 선택 Drive 파일 |

## 3. 파일 이름

기본 백업 파일 이름:

```text
steppie-backup-{yyyyMMdd-HHmmss}.zip
```

규칙:

- timestamp는 기기 로컬 timezone을 사용한다.
- 마지막 성공 백업은 플랫폼 UI에서 "latest"로 노출할 수 있다.
- 사용자가 명시적으로 확인하지 않는 한 기존 백업을 덮어쓰지 않는다.

## 4. manifest.json

```json
{
  "app": "Steppie",
  "backupSchemaVersion": 1,
  "createdAt": "2026-06-17T09:00:00+09:00",
  "sourcePlatform": "ios",
  "appVersion": "1.0.0",
  "dataFile": "data.json",
  "assetDirectory": "assets",
  "checksum": {
    "algorithm": "sha256",
    "dataJson": "..."
  }
}
```

| 필드 | 필수 | 설명 |
|---|---:|---|
| `app` | 예 | 반드시 `Steppie` |
| `backupSchemaVersion` | 예 | `1`에서 시작 |
| `createdAt` | 예 | ISO 8601 timestamp |
| `sourcePlatform` | 예 | `ios`, `android` |
| `appVersion` | 예 | 사용자에게 보이는 앱 버전 |
| `dataFile` | 예 | 일반적으로 `data.json` |
| `assetDirectory` | 예 | 일반적으로 `assets` |
| `checksum` | 예 | 최소한 `data.json`을 검증 |

## 5. data.json

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-06-17T09:00:00+09:00",
  "routineSets": [],
  "routines": [],
  "dailyLogs": [],
  "appSettings": {
    "feedbackIntensity": "normal",
    "soundEnabled": true,
    "ttsEnabled": true,
    "ttsRate": 1.0,
    "ttsVolume": 1.0,
    "hapticEnabled": true,
    "undoDurationSeconds": 5,
    "notificationLeadTimes": [10, 5],
    "quietHoursStart": null,
    "quietHoursEnd": null,
    "locale": null
  }
}
```

규칙:

- 엔티티 필드는 `docs/data-contract.md`를 따른다.
- `guardianPinHash`와 `recoveryCodeHash`는 같은 플랫폼에서 안전하게 복원 가능한 해시 형식일 때만 포함할 수 있다.
- 크로스 플랫폼 복원 시에는 복원 후 보호자가 새 PIN을 만들도록 요구한다.
- 누락된 선택 필드는 `data-contract.md`의 기본값을 사용한다.

## 6. 에셋 규칙

| 에셋 타입 | 형식 | 규칙 |
|---|---|---|
| 루틴 사진 | JPEG | 권장 백업 형식 |
| 투명 배경이 필요한 루틴 사진 | PNG | 원본 특성이 필요할 때 허용 |
| 내장 아이콘 | 없음 | export하지 않고 아이콘 이름으로 복원 |

제약:

- 개별 에셋 최대 크기: 5 MB.
- v1.0 목표 백업 패키지 최대 크기: 200 MB.
- 에셋 파일명은 `IconRef.backupAssetName`에서 참조해야 한다.
- 복원 중 에셋이 없으면 안전한 내장 placeholder 아이콘을 사용하고 루틴은 유지한다.

## 7. 복원 동작

복원 모드:

| 모드 | 동작 |
|---|---|
| Replace | 현재 앱 데이터를 지운 뒤 백업을 가져온다 |
| Merge | v1.0에서는 지원하지 않는다 |

복원 단계:

1. zip 구조를 검증한다.
2. `manifest.json`을 읽고 검증한다.
3. checksum을 검증한다.
4. `data.json`을 파싱한다.
5. 필수 엔티티와 참조 관계를 검증한다.
6. 보호자 PIN으로 destructive replace를 확인한다.
7. 트랜잭션 안에서 데이터를 가져온다.
8. 에셋을 가져온다.
9. 알림 스케줄을 다시 만든다.
10. 활성 RoutineSet이 있는 아이 모드로 돌아간다.

실패 규칙:

- 복원은 all-or-nothing이다. 필수 단계 중 하나라도 실패하면 기존 앱 데이터를 변경하지 않는다.

## 8. 충돌과 무결성 규칙

- 같은 백업 안의 중복 ID는 invalid다.
- 모든 Routine의 RoutineSet 참조는 존재해야 한다.
- DailyLog의 Routine 참조는 해당 Routine이 삭제되었고 백업에 포함된 경우를 제외하면 존재해야 한다.
- 활성/미삭제 RoutineSet은 정확히 하나가 바람직하다. 여러 개면 가장 최근 수정된 세트를 사용하고 나머지는 비활성화한다.
- 활성 RoutineSet이 없으면 가장 최근 수정된 미삭제 RoutineSet을 활성화한다.

## 9. 개인정보

백업에는 아이의 루틴 이름과 사진이 포함될 수 있다. 앱은 다음을 지켜야 한다.

- 첫 백업 전에 백업에 포함되는 내용을 설명한다.
- 보호자 액션 또는 명시적인 플랫폼 클라우드 설정 없이 백업을 업로드하지 않는다.
- 분석 식별자를 포함하지 않는다.
- 원본 PIN, 원본 복구 코드, 기기 notification token을 포함하지 않는다.

## 10. 테스트 fixture

양 플랫폼은 다음 fixture 범주를 공유하거나 동일하게 재현해야 한다.

- 빈 앱 백업.
- morning 템플릿 백업.
- 커스텀 사진이 있는 백업.
- DailyLog 완료 이력이 있는 백업.
- 선택 필드가 누락된 백업.
- checksum이 손상된 백업.
- 사진 에셋이 누락된 백업.
