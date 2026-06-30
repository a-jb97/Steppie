# Steppie 출시 체크리스트

> 기준 문서: `docs/planning/steppie_planning_document.md`  
> 버전: 0.1.0  
> 범위: iOS App Store와 Google Play 공통 출시 전 체크리스트.

## 1. 계약 동결

- [ ] `docs/data-contract.md`를 iOS/Android 구현자가 검토했다.
- [ ] `docs/backup-contract.md`를 복원 edge case까지 포함해 검토했다.
- [ ] `docs/design-tokens.md`가 양 플랫폼에 구현되었다.
- [ ] `docs/localization-contract.md`가 한국어/영어로 구현되었다.
- [ ] `docs/accessibility-checklist.md` 필수 항목을 통과했다.
- [ ] `docs/test-scenarios.md` 출시 시나리오를 통과했다.
- [ ] 플랫폼별 예외가 문서화되었다.

## 2. 제품 범위

- [ ] 아이 포커스 뷰가 구현되었다.
- [ ] 아이 루틴 목록이 읽기 전용으로 구현되었다.
- [ ] 완료 피드백이 구현되었다.
- [ ] 완료 되돌리기가 구현되었다.
- [ ] 전체 완료 화면이 구현되었다.
- [ ] 보호자 PIN 진입이 구현되었다.
- [ ] 보호자 모드 3분 미조작 종료가 구현되었다.
- [ ] 루틴 생성/편집/삭제가 구현되었다.
- [ ] 루틴 순서 변경이 구현되었다.
- [ ] 기본 아이콘 선택이 구현되었다.
- [ ] 사진 촬영/갤러리 업로드가 구현되었다.
- [ ] 루틴 템플릿 morning, school, bedtime이 구현되었다.
- [ ] 진행 기록이 구현되었다.
- [ ] 피드백 설정이 구현되었다.
- [ ] 알림 설정이 구현되었다.
- [ ] 복구 코드 확인 또는 재생성 흐름이 구현되었다.
- [ ] 백업과 복원이 구현되었다.

## 3. 데이터와 오프라인 동작

- [ ] 앱은 설치 후 완전히 오프라인으로 동작한다.
- [ ] 로컬 저장 데이터는 앱 재시작 후에도 유지된다.
- [ ] 로컬 자정 리셋이 동작한다.
- [ ] DailyLog는 이전 날짜 기록을 보존한다.
- [ ] 삭제된 루틴이 history를 깨지 않는다.
- [ ] 이전 internal build에서 schema migration을 테스트했다.
- [ ] seed data가 양 플랫폼에서 일치한다.
- [ ] 내장 아이콘 60개 이상을 제공한다.
- [ ] 사용자 사진 에셋은 로컬 저장소와 백업 참조를 가진다.

## 4. 백업과 복원

- [ ] 유효한 백업을 생성할 수 있다.
- [ ] 유효한 백업을 transactional하게 복원할 수 있다.
- [ ] 커스텀 사진이 있는 백업에서 이미지를 복원할 수 있다.
- [ ] 사진 에셋이 누락되어도 placeholder로 복원되고 데이터 손실이 없다.
- [ ] 손상된 checksum은 복원을 차단한다.
- [ ] 원본 PIN은 백업에 없다.
- [ ] 원본 복구 코드는 백업에 없다.
- [ ] 복원 후 알림이 다시 만들어진다.

## 5. 접근성

- [ ] VoiceOver 아이 흐름이 통과한다.
- [ ] TalkBack 아이 흐름이 통과한다.
- [ ] Dynamic Type 최대 크기가 통과한다.
- [ ] Android Font Scale 최대 목표가 통과한다.
- [ ] iOS Switch Control로 완료까지 도달할 수 있다.
- [ ] Android Switch Access로 완료까지 도달할 수 있다.
- [ ] iOS Reduce Motion 동작이 통과한다.
- [ ] Android animation scale off 동작이 통과한다.
- [ ] WCAG AA contrast를 검증했다.
- [ ] 상태 표현이 색상에만 의존하지 않는다.

## 6. 기기와 레이아웃

- [ ] iPhone portrait를 검증했다.
- [ ] iPhone landscape는 portrait lock 상태다.
- [ ] iPad portrait를 검증했다.
- [ ] iPad landscape split view를 검증했다.
- [ ] Android phone portrait를 검증했다.
- [ ] Android phone landscape는 portrait lock 상태다.
- [ ] Android tablet portrait를 검증했다.
- [ ] Android tablet landscape split view를 검증했다.
- [ ] 지원 레이아웃에서 텍스트가 겹치거나 잘리지 않는다.

## 7. 현지화

- [ ] 한국어 문자열이 complete 상태다.
- [ ] 영어 문자열이 complete 상태다.
- [ ] locale 간 placeholder 이름이 일치한다.
- [ ] 아이 모드 한국어/영어 문구는 짧고 구체적이다.
- [ ] TTS 문자열에는 화면 문구 대응물이 있다.
- [ ] 지원하지 않는 시스템 locale에서도 안전하게 fallback한다.

## 8. 알림과 권한

- [ ] 알림 권한은 보호자 맥락에서만 요청한다.
- [ ] 예정 알림은 루틴 시간 10분 전 및/또는 5분 전에 발송된다.
- [ ] 알림을 탭하면 관련 포커스 뷰가 열린다.
- [ ] Quiet hours는 구현 결정에 따라 알림을 억제하거나 지연한다.
- [ ] 권한 거부 상태에서도 앱은 사용 가능하다.
- [ ] 플랫폼 권한 상태는 백업에 포함하지 않는다.

## 9. 개인정보와 안전

- [ ] 광고가 없다.
- [ ] v1.0에는 인앱결제가 없다.
- [ ] 백업에 analytics identifier가 없다.
- [ ] 아이 모드에는 설정, 삭제, PIN 변경 UI가 없다.
- [ ] 보호자 destructive action은 확인을 요구한다.
- [ ] 첫 백업 전에 백업 설명을 보여준다.
- [ ] Store privacy label/data safety form이 실제 동작과 일치한다.

## 10. 스토어 준비

### iOS / iPadOS

- [ ] 필수 device class용 App Store screenshot을 준비했다.
- [ ] App privacy details를 완료했다.
- [ ] iCloud/CloudKit entitlement를 검증했다.
- [ ] Local notification permission purpose copy를 검토했다.
- [ ] 사진 업로드가 활성화된 경우 Photos permission purpose copy를 검토했다.
- [ ] TestFlight build가 smoke test를 통과했다.

### Android

- [ ] Google Play phone/tablet screenshot을 준비했다.
- [ ] Data safety form을 완료했다.
- [ ] Google Drive API configuration을 검증했다.
- [ ] 지원 Android 버전의 notification permission flow를 검증했다.
- [ ] Photo Picker 동작을 검증했다.
- [ ] Internal testing build가 smoke test를 통과했다.

## 11. 최종 출시 기준

- [ ] iOS build에 release-blocking warning 또는 crash가 없다.
- [ ] Android build에 release-blocking warning 또는 crash가 없다.
- [ ] shared test scenarios가 양 플랫폼에서 통과한다.
- [ ] accessibility checklist가 양 플랫폼에서 통과한다.
- [ ] clean install에서 backup/restore를 테스트했다.
- [ ] 한국어/영어 release notes를 준비했다.
- [ ] version number와 build number가 final이다.
