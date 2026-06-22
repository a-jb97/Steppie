# 차례차례 / Steppie 기획문서

> 발달장애·자폐 스펙트럼 사용자를 위한 하루 루틴 시각화 앱  
> v1.0 개발 전략: iOS / Android 네이티브 병렬 개발 + 공통 계약 기반 구현

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [타겟 사용자 정의](#2-타겟-사용자-정의)
3. [핵심 기능 정의](#3-핵심-기능-정의)
4. [화면 구조](#4-화면-구조)
5. [UX/UI 가이드라인](#5-uxui-가이드라인)
6. [기술 스택 & 아키텍처](#6-기술-스택--아키텍처)
7. [Codex 병렬 개발 전략](#7-codex-병렬-개발-전략)
8. [공통 계약 문서](#8-공통-계약-문서)
9. [접근성 요구사항](#9-접근성-요구사항)
10. [보호자/관리자 기능](#10-보호자관리자-기능)
11. [알림 & 피드백 시스템](#11-알림--피드백-시스템)
12. [출시 전략 & 향후 로드맵](#12-출시-전략--향후-로드맵)

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 앱 이름 | 차례차례 / Steppie |
| 한 줄 소개 | 발달장애·자폐 스펙트럼 사용자가 하루 일과를 그림과 색상으로 쉽게 따라갈 수 있도록 돕는 루틴 시각화 앱 |
| 플랫폼 | iOS, iPadOS, Android Phone, Android Tablet |
| 주 사용자 | 발달장애·자폐 스펙트럼 아동 및 성인 |
| 보조 사용자 | 보호자, 특수교사, 치료사 |
| 핵심 가치 | 단순함 / 예측 가능성 / 불안 감소 |
| 개발 방식 | SwiftUI 앱과 Jetpack Compose 앱을 공통 계약 문서 기준으로 병렬 개발 |

---

## 2. 타겟 사용자 정의

### 2-1. 주 사용자

| 구분 | 내용 |
|------|------|
| 대상 | 발달장애·자폐 스펙트럼 아동~성인 |
| 연령 | 만 4세 ~ 성인 |
| 특성 | 텍스트보다 시각 정보에 익숙 / 일과의 예측 가능성이 중요 / 감각 민감성 개인차 큼 / 터치 정확도 개인차 있음 |
| 사용 환경 | 가정, 학교, 복지관, 이동 중, 오프라인 환경 |

### 2-2. 보조 사용자

| 구분 | 역할 |
|------|------|
| 보호자 | 루틴 생성·수정, 알림 설정, 진행 상황 확인 |
| 특수교사 | 학교 루틴 설정, 학생별 맞춤 구성 |
| 치료사 | 치료 목표에 맞는 루틴 설계 |

---

## 3. 핵심 기능 정의

### 3-1. 앱 모드 구조

```text
Steppie
├── 아이 모드
│   └── 우상단 모서리 3초 길게 누르기 + PIN 입력 -> 보호자 모드
└── 보호자 모드
    └── 완료 또는 3분 미조작 -> 아이 모드
```

PIN은 4자리 숫자다. 아이 모드에서는 설정 진입 UI를 화면에 표시하지 않는다.

### 3-2. 아이 모드

| ID | 기능 | 설명 |
|----|------|------|
| F-01 | 루틴 목록 보기 | 오늘의 일과를 카드 형태로 순서대로 표시 |
| F-02 | 포커스 뷰 | 앱 실행 시 기본 화면. 현재 해야 할 하나의 카드만 크게 표시 |
| F-03 | 완료 처리 | 포커스 카드 탭으로 완료 처리 |
| F-04 | 진행률 표시 | 전체 루틴 중 완료 수를 별, 바, 숫자 등으로 표시 |
| F-05 | 음성 안내 | 카드 진입 시 활동명을 TTS로 안내 |
| F-06 | 예고 알림 | 예정 시각이 있는 활동에 대해 10분 전 / 5분 전 알림 |
| F-07 | 루틴 완료 | 모든 항목 완료 시 전체 화면 축하 연출 |

### 3-3. 보호자 모드

| ID | 기능 | 설명 |
|----|------|------|
| F-10 | 루틴 생성/편집 | 활동 카드 추가, 순서 변경, 삭제 |
| F-11 | 아이콘/이미지 설정 | 기본 아이콘 선택 또는 사진 촬영/갤러리 업로드 |
| F-12 | 색상 설정 | 카드별 배경 색상 지정 |
| F-13 | 시간 설정 | 활동별 예정 시각 지정 |
| F-14 | 음성 설정 | TTS 음성 종류, 속도, 볼륨 조절 |
| F-15 | 피드백 설정 | 애니메이션 강도, 효과음, 햅틱 ON/OFF |
| F-16 | 루틴 템플릿 | 아침, 학교, 취침 루틴 템플릿 |
| F-17 | 진행 기록 | 날짜별 완료 현황 확인 |
| F-18 | PIN 변경 | 보호자 PIN 설정 및 변경 |

---

## 4. 화면 구조

### 4-1. 아이 모드 IA

```text
앱 실행
└── 포커스 뷰
     ├── 위로 스와이프 -> 루틴 목록 뷰
     │    └── 카드 탭 -> 포커스 뷰
     ├── 카드 탭 -> 완료 피드백
     │    └── 다음 카드 포커스 뷰
     └── 모든 카드 완료 -> 루틴 완료 화면
```

완료 처리는 항상 포커스 뷰에서만 가능하다. 루틴 목록 뷰는 확인용이다.

### 4-2. 보호자 모드 IA

```text
보호자 모드 홈
├── 루틴 관리
│   ├── 루틴 세트 목록
│   ├── 새 루틴 세트 만들기
│   ├── 카드 편집
│   └── 템플릿에서 시작하기
├── 환경 설정
│   ├── 피드백 설정
│   └── 알림 설정
├── 진행 기록
└── 앱 설정
    ├── PIN 변경
    └── 복구 코드 확인
```

### 4-3. 기기별 레이아웃

| 기기 | 방향 | 레이아웃 |
|------|------|---------|
| iPhone | 세로 | 포커스 카드 전체 화면 |
| iPhone | 가로 | 미지원. 세로 고정 |
| iPad | 세로 | 포커스 카드 중앙 배치, 양옆 여백 |
| iPad | 가로 | 좌: 루틴 목록 / 우: 포커스 카드 Split View |
| Android Phone | 세로 | 포커스 카드 전체 화면 |
| Android Phone | 가로 | 미지원. 세로 고정 |
| Android Tablet | 세로 | 포커스 카드 중앙 배치, 양옆 여백 |
| Android Tablet | 가로 | 좌: 루틴 목록 / 우: 포커스 카드 Split View |

---

## 5. UX/UI 가이드라인

| 원칙 | 내용 |
|------|------|
| 예측 가능성 | 같은 동작은 항상 같은 결과를 만든다 |
| 정보 최소화 | 한 화면에 하나의 핵심 메시지만 둔다 |
| 오류 없는 설계 | 되돌릴 수 없는 동작은 아이 모드에서 차단한다 |
| 성공 경험 | 작은 완료에도 즉각적인 긍정 피드백을 준다 |
| 자극 조절 | 애니메이션, 효과음, TTS, 햅틱을 개별 조절한다 |

### 5-1. 터치 & 인터랙션

- 카드 최소 터치 영역: 80x80pt 이상
- 완료: 짧은 탭 1회
- 실수 복구: 완료 후 보호자 설정 시간 내 언두 가능
- 스와이프: 위/아래 방향만 사용
- 아이 모드에서 삭제, 설정 변경, PIN 변경은 불가능

### 5-2. 시각 디자인

| 항목 | 기준 |
|------|------|
| 폰트 | Apple SD Gothic Neo, Noto Sans KR 계열 |
| 글자 크기 | 카드 라벨 최소 24pt |
| 아이콘 | 단순한 플랫 일러스트, 명확한 외곽선 |
| 색상 대비 | WCAG AA 이상 |
| 배경 | 카드 배경은 단색, 패턴 없음 |
| 애니메이션 | 완료 피드백은 0.4초 이내 시작, 1.5초 이내 종료 |
| 다크모드 | 시스템 설정 연동 |

---

## 6. 기술 스택 & 아키텍처

### 6-1. 플랫폼별 기술 스택

| 항목 | iOS / iPadOS | Android |
|------|--------------|---------|
| 언어 | Swift 5.9+ | Kotlin |
| UI 프레임워크 | SwiftUI | Jetpack Compose |
| 최소 버전 | iOS 18 / iPadOS 18 | Android 9, API 28 |
| 로컬 DB | SwiftData | Room |
| 아키텍처 | MVVM + @Observable | MVVM + StateFlow + AAC ViewModel |
| 화면 방향 | iPhone: 세로 고정 / iPad: 세로·가로 지원 | Phone: 세로 고정 / Tablet: 세로·가로 지원 |
| 백업 | iCloud / CloudKit | Google Drive API |

### 6-2. 주요 기능별 네이티브 API

| 기능 | iOS / iPadOS | Android |
|------|--------------|---------|
| 햅틱 | UIImpactFeedbackGenerator | VibrationEffect |
| TTS | AVSpeechSynthesizer | TextToSpeech |
| 로컬 알림 | UserNotifications | WorkManager + NotificationCompat |
| 로컬 저장 | SwiftData | Room |
| 이미지 | PhotosUI | Photo Picker API |
| 접근성 | AccessibilityLabel, AXCustomAction | ContentDescription, AccessibilityDelegate |
| 백업 | iCloud / CloudKit | Google Drive API |

### 6-3. 레이어 구조

```text
UI Layer
└── ViewModel
    └── Repository
        └── Data Layer
```

v1.0에서는 ViewModel이 Repository를 직접 호출한다. AI 기능 도입 시 UseCase 레이어를 분리한다.

### 6-4. 핵심 데이터 구조

공식 데이터 필드는 `docs/data-contract.md`를 기준으로 한다.

```text
Routine
├── id: UUID
├── titleKey: String?
├── title: LocalizedText
├── icon: IconRef
├── colorToken: String
├── order: Int
├── scheduledTime: LocalTime?
├── isCompletedToday: Bool
└── isActive: Bool

RoutineSet
├── id: UUID
├── name: LocalizedText
├── isActive: Bool
└── routines: [Routine]

DailyLog
├── id: UUID
├── date: LocalDate
├── routineId: UUID
├── status: completed | undone
└── completedAt: DateTime?
```

---

## 7. Codex 병렬 개발 전략

Steppie v1.0은 iOS와 Android를 각각 네이티브 앱으로 병렬 개발한다. 두 플랫폼은 같은 사용자 경험과 데이터 의미를 가져야 하며, 이를 위해 `docs/` 하위 공통 계약 문서를 기준으로 구현한다.

### 7-1. 저장소 구조

```text
steppie/
├── docs/
│   ├── planning
│   │   └── steppie_planning_document
│   ├── product-spec.md
│   ├── data-contract.md
│   ├── backup-contract.md
│   ├── design-tokens.md
│   ├── localization-contract.md
│   ├── accessibility-checklist.md
│   ├── test-scenarios.md
│   └── release-checklist.md
├── ios/
│   └── Steppie.xcodeproj
└── android/
    └── Steppie/
```

### 7-2. Codex 작업 흐름

| 스레드 | 역할 | 수정 가능 범위 |
|--------|------|----------------|
| 공통 명세 스레드 | 기능 요구사항, 데이터 계약, 백업 포맷, 디자인 토큰, 테스트 시나리오 관리 | docs/ |
| iOS 개발 스레드 | SwiftUI, SwiftData, iCloud, iOS 접근성 구현 | ios/ |
| Android 개발 스레드 | Compose, Room, Google Drive, Android 접근성 구현 | android/ |

플랫폼별 스레드는 `docs/`를 읽을 수 있지만, 다른 플랫폼 디렉터리는 수정하지 않는다.

### 7-3. Codex 지시 예시

```text
iOS 앱만 작업해.
docs/data-contract.md와 docs/design-tokens.md를 기준으로 Routine 모델과 포커스 뷰를 구현해.
android/ 디렉터리는 수정하지 마.
완료 후 빌드 결과와 남은 TODO를 알려줘.
```

```text
Android 앱만 작업해.
docs/data-contract.md와 docs/design-tokens.md를 기준으로 Room Entity와 Compose 포커스 화면을 구현해.
ios/ 디렉터리는 수정하지 마.
완료 후 빌드 결과와 남은 TODO를 알려줘.
```

### 7-4. 병렬 개발 순서

초기 단계에서는 양 플랫폼의 구현 기준을 먼저 고정한다. 저장소와 개발 환경을 준비한 뒤 디자인 시스템과 재사용 컴포넌트를 구현하고, 그 위에 데이터 계층과 기능 화면을 순서대로 올린다. 각 단계는 iOS와 Android worktree에서 병렬로 진행하되, 공통 계약이 바뀌면 먼저 `docs/`를 수정하고 양쪽 브랜치에 반영한다.

| 스프린트 | 공통 명세/준비 | iOS | Android |
|----------|----------------|-----|---------|
| 0 | 저장소 구조, 공통 계약 문서, Git worktree, AGENTS.md 확정 | Xcode 프로젝트 생성 및 빌드 확인 | Android Studio 프로젝트 생성 및 빌드 확인 |
| 1 | 플랫폼별 최소 의존성, 아이콘/폰트 에셋, 지원 OS 범위 확정 | Apple 기본 프레임워크와 에셋 검증 | Compose/Room/DataStore 등 기반 의존성과 drawable 검증 |
| 2 | Figma 연결, 디자인 토큰, 공통 컴포넌트 계약 확정 | Color/Typography/Spacing, Button, RoutineCard 구현 | Color/Typography/Spacing, Button, RoutineCard 구현 |
| 3 | 데이터/백업 계약과 공통 샘플 데이터 확정 | SwiftData 모델, Repository, 인메모리 테스트 | Room Entity/DAO, Repository, 인메모리 테스트 |
| 4 | 포커스 흐름과 휴대폰/태블릿 반응형 규칙 확정 | 루틴 목록/포커스 뷰, iPad 가로 레이아웃 | 루틴 목록/포커스 화면, 태블릿 가로 레이아웃 |
| 5 | 피드백 및 완료/언두 시나리오 확정 | TTS/햅틱/효과음/완료 흐름 | TTS/진동/효과음/완료 흐름 |
| 6 | 알림 권한, 예고 시각, 재예약 시나리오 확정 | UserNotifications 기반 로컬 알림 | Notification API와 AlarmManager 기반 로컬 알림 |
| 7 | 보호자 모드 계약 확정 | 루틴 편집/정렬/PIN | 루틴 편집/정렬/PIN |
| 8 | 백업/복원 및 스키마 버전 정책 확정 | iCloud/CloudKit 백업·복원 | Google Drive 백업·복원, 지연 작업은 WorkManager 사용 |
| 9 | 접근성 체크리스트와 공통 테스트 시나리오 확정 | VoiceOver/Dynamic Type 검증 | TalkBack/Font Scale 검증 |
| 10 | 출시 체크리스트 확정 | App Store 준비 | Google Play 준비 |

---

## 8. 공통 계약 문서

`docs/`는 양 플랫폼 구현의 단일 기준이다. 기능이 바뀌면 먼저 `docs/`를 수정하고, 그 다음 iOS와 Android 구현을 맞춘다.

| 문서 | 목적 |
|------|------|
| product-spec.md | 기능 범위와 화면별 요구사항 |
| data-contract.md | Routine, RoutineSet, DailyLog, Settings 데이터 구조 |
| backup-contract.md | iCloud/Google Drive에 저장할 공통 백업 파일 포맷 |
| design-tokens.md | 색상, 글자 크기, 터치 영역, 간격, 아이콘 이름 |
| localization-contract.md | 한국어/영어 문자열 키와 문구 |
| accessibility-checklist.md | VoiceOver/TalkBack 공통 통과 기준 |
| test-scenarios.md | 양 플랫폼이 동일하게 통과해야 하는 기능 시나리오 |
| release-checklist.md | 출시 전 검증 항목 |

---

## 9. 접근성 요구사항

| 영역 | 기준 |
|------|------|
| 시각 | WCAG AA 이상, 색상 단독 정보 전달 금지, 다이나믹 타입/Font Scale 지원 |
| 운동/터치 | 80x80pt 이상 터치 영역, 언두 제공, Switch Control/Switch Access 고려 |
| 청각/음성 | 모든 음성 안내는 화면 텍스트로도 제공, TTS ON/OFF 지원 |
| 인지 | 한 화면에 핵심 요소 1~3개, 타임아웃 없음, 예측 가능한 동작 |
| 애니메이션 | Reduce Motion 또는 Android 애니메이션 배율 0 설정 반영 |

---

## 10. 보호자/관리자 기능

### 10-1. 진입 방식

| 항목 | 내용 |
|------|------|
| 진입 트리거 | 아이 모드 우상단 모서리 3초 길게 누르기 |
| 인증 방식 | 4자리 PIN |
| 초기 설정 | 앱 최초 실행 시 PIN 등록 |
| PIN 분실 | 6자리 복구 코드 |
| 복귀 | 완료 버튼 또는 3분 미조작 시 자동 복귀 |

### 10-2. 루틴 관리

- 카드 추가, 편집, 삭제
- 드래그 앤 드롭 순서 변경
- 루틴 비활성화
- 복수 루틴 세트
- 활성 루틴 전환
- 기본 템플릿: 아침, 학교, 취침

### 10-3. 설정

- 칭찬 애니메이션 강도: 강함 / 보통 / 조용함 / 없음
- 효과음 ON/OFF
- TTS ON/OFF, 음성 종류, 속도, 볼륨
- 햅틱 ON/OFF
- 언두 표시 시간: 3초 / 5초 / 10초
- 예고 알림: 10분 전 / 5분 전 / 없음
- 방해 금지 시간

---

## 11. 알림 & 피드백 시스템

### 11-1. 알림

```text
시간 기반 루틴 카드
└── 예고 알림
     ├── 10분 전 알림
     └── 5분 전 알림
          └── 알림 탭 -> 해당 카드 포커스 뷰
```

알림은 보호자가 명시적으로 설정한 경우에만 발송한다.

### 11-2. 완료 피드백

```text
사용자 탭
├── 즉각 피드백: 햅틱 + 카드 스케일 애니메이션
├── 완료 처리
├── TTS: "{활동명} 완료! 잘했어요!"
├── 칭찬 화면
└── 다음 카드 포커스 뷰
```

### 11-3. 언두

```text
완료 처리 직후
└── 하단 스낵바: "{활동명} 완료  되돌리기"
     ├── 탭: 완료 취소
     └── 시간 초과: 완료 확정
```

언두는 아이 모드에서도 사용할 수 있다.

---

## 12. 출시 전략 & 향후 로드맵

### 12-1. 출시 전략

| 항목 | 내용 |
|------|------|
| 출시 형태 | iOS App Store + Google Play 동시 출시 |
| 가격 | 무료, 인앱결제 없음, 광고 없음 |
| 출시 언어 | 한국어 + 영어 |
| 타겟 국가 | 한국 + 영어권 |
| v1.0 범위 | 핵심 기능, 로컬 저장, 백업/복원, 접근성, 태블릿 가로 Split View |

### 12-2. 출시 전 체크리스트

- 아이 모드: 포커스 뷰, 루틴 목록, 완료 피드백, 전체 완료 화면
- 보호자 모드: PIN, 루틴 편집, 피드백 설정, 알림 설정, 진행 기록
- 기본 아이콘 60개 이상
- 기본 템플릿 3종
- 오프라인 100% 동작
- 자정 루틴 리셋
- iCloud / Google Drive 백업·복원
- VoiceOver / TalkBack 통과
- Reduce Motion / Android 애니메이션 배율 0 대응
- 다이나믹 타입 / Font Scale 대응
- WCAG AA 색상 대비
- iPhone, iPad 세로/가로, Android Phone, Android Tablet 세로/가로 검증
- 한국어/영어 현지화

### 12-3. 향후 로드맵

| 단계 | 내용 |
|------|------|
| v1.0 | 핵심 기능 전체 + 백업/복원 + 한국어/영어 |
| v1.1 | 사용자 피드백 반영, 버그 수정, 아이콘 추가 |
| v1.5 | 일본어 현지화 검토 |
| v2.0 | 온디바이스 AI 기능 검토 |
