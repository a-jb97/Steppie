# Steppie 현지화 계약

> 기준 문서: `docs/planning/steppie_planning_document.md`  
> 버전: 0.1.0  
> 범위: iOS와 Android가 공유하는 한국어/영어 localization key.

## 1. 지원 locale

| Locale | 상태 | 설명 |
|---|---|---|
| `ko` | 필수 | 출시 기본 언어 |
| `en` | 필수 | 출시 언어 |

Fallback 순서:

1. 사용자가 선택한 앱 locale이 있으면 우선 사용.
2. 지원되는 경우 시스템 언어.
3. 한국어(`ko`).
4. 사용 가능한 첫 번째 localized value.

## 2. Key 규칙

- Key는 lowercase dot notation을 사용한다.
- Key에 플랫폼 이름을 넣지 않는다.
- 아이 모드 문구는 짧고 구체적으로 작성한다.
- 아이에게 보이는 문구에는 관용적 표현을 피한다.
- 동적 값이 있는 문자열은 named placeholder를 사용한다.
- 화면 문구와 음성 안내 문구가 달라야 하면 TTS 전용 key를 둔다.

Placeholder 스타일:

```text
{routineTitle}
{completed}
{total}
{minutes}
```

## 3. 앱 공통 문자열

| Key | ko | en | 설명 |
|---|---|---|---|
| `app.name` | 차례차례 | Steppie | 앱 표시 이름 |
| `mode.child` | 아이 모드 | Child mode | 보호자 화면에서 표시 |
| `mode.guardian` | 보호자 모드 | Guardian mode | 보호자 화면에서 표시 |
| `action.done` | 완료 | Done | 공통 |
| `action.cancel` | 취소 | Cancel | 공통 |
| `action.save` | 저장 | Save | 공통 |
| `action.delete` | 삭제 | Delete | 보호자 전용 |
| `action.edit` | 편집 | Edit | 보호자 전용 |
| `action.next` | 다음 | Next | 온보딩/폼 |
| `action.back` | 뒤로 | Back | 내비게이션 |

## 4. 아이 모드 문자열

| Key | ko | en | 설명 |
|---|---|---|---|
| `child.focus.title` | 지금 할 일 | Now |  |
| `child.progress.label` | {completed}/{total} 완료 | {completed}/{total} done |  |
| `child.complete.button` | 다 했어요 | I did it |  |
| `child.undo.message` | {routineTitle} 완료 | {routineTitle} done |  |
| `child.undo.action` | 되돌리기 | Undo |  |
| `child.allDone.title` | 오늘 일과 끝 | All done today |  |
| `child.allDone.message` | 잘했어요 | Great job |  |
| `child.list.title` | 오늘의 순서 | Today's steps |  |
| `child.empty.title` | 오늘 할 일이 없어요 | No steps today |  |

## 5. TTS 문자열

| Key | ko | en | 설명 |
|---|---|---|---|
| `tts.routine.focus` | {routineTitle} 할 시간이에요 | Time for {routineTitle} |  |
| `tts.routine.completed` | {routineTitle} 완료! 잘했어요! | {routineTitle} done. Great job! |  |
| `tts.allDone` | 오늘 일과를 모두 마쳤어요. 잘했어요! | All steps are done today. Great job! |  |

규칙:

- TTS는 `ttsEnabled`를 따라야 한다.
- TTS가 겹치지 않도록 플랫폼 기능에 맞게 중지하거나 queue 처리한다.
- 모든 음성 안내에는 대응되는 화면 텍스트가 있어야 한다.

## 6. 보호자 모드 문자열

| Key | ko | en | 설명 |
|---|---|---|---|
| `guardian.home.title` | 보호자 모드 | Guardian mode |  |
| `guardian.enterPin.title` | PIN 입력 | Enter PIN |  |
| `guardian.enterPin.error` | PIN이 맞지 않아요 | PIN does not match |  |
| `guardian.pin.create.title` | PIN 만들기 | Create PIN |  |
| `guardian.pin.change.title` | PIN 변경 | Change PIN |  |
| `guardian.recovery.title` | 복구 코드 | Recovery code |  |
| `guardian.routineSets.title` | 루틴 세트 | Routine sets |  |
| `guardian.routine.add` | 카드 추가 | Add card |  |
| `guardian.routine.edit` | 카드 편집 | Edit card |  |
| `guardian.routine.titleField` | 활동 이름 | Activity name |  |
| `guardian.routine.timeField` | 예정 시각 | Scheduled time |  |
| `guardian.settings.title` | 환경 설정 | Settings |  |
| `guardian.settings.feedback` | 피드백 | Feedback |  |
| `guardian.settings.notification` | 알림 | Notifications |  |
| `guardian.history.title` | 진행 기록 | Progress history |  |

## 7. 설정 값 문자열

| Key | ko | en |
|---|---|---|
| `feedback.strong` | 강함 | Strong |
| `feedback.normal` | 보통 | Normal |
| `feedback.quiet` | 조용함 | Quiet |
| `feedback.off` | 없음 | Off |
| `setting.sound` | 효과음 | Sound |
| `setting.tts` | 음성 안내 | Voice guidance |
| `setting.haptic` | 햅틱 | Haptics |
| `setting.undoDuration` | 되돌리기 시간 | Undo duration |
| `setting.quietHours` | 방해 금지 시간 | Quiet hours |

## 8. 알림 문자열

| Key | ko | en | 설명 |
|---|---|---|---|
| `notification.upcoming.title` | 곧 할 일이 있어요 | A step is coming up |  |
| `notification.upcoming.body` | {minutes}분 뒤 {routineTitle} 시간이에요 | {routineTitle} starts in {minutes} minutes |  |
| `notification.now.title` | 지금 할 일 | Time now |  |
| `notification.now.body` | {routineTitle} 할 시간이에요 | Time for {routineTitle} |  |

## 9. 템플릿 문자열

| Key | ko | en |
|---|---|---|
| `template.morning.name` | 아침 루틴 | Morning routine |
| `template.school.name` | 학교 루틴 | School routine |
| `template.bedtime.name` | 취침 루틴 | Bedtime routine |
| `routine.wakeUp` | 일어나기 | Wake up |
| `routine.washFace` | 세수하기 | Wash face |
| `routine.brushTeeth` | 양치하기 | Brush teeth |
| `routine.getDressed` | 옷 입기 | Get dressed |
| `routine.breakfast` | 아침 먹기 | Eat breakfast |
| `routine.packBag` | 가방 챙기기 | Pack bag |
| `routine.goSchool` | 학교 가기 | Go to school |
| `routine.readBook` | 책 읽기 | Read book |
| `routine.lunch` | 점심 먹기 | Eat lunch |
| `routine.play` | 놀이하기 | Play |
| `routine.bath` | 목욕하기 | Take a bath |
| `routine.pajamas` | 잠옷 입기 | Put on pajamas |
| `routine.sleep` | 잠자기 | Sleep |

## 10. 접근성 문자열

| Key | ko | en | 설명 |
|---|---|---|---|
| `a11y.routine.card` | {routineTitle}, {status} | {routineTitle}, {status} |  |
| `a11y.status.completed` | 완료됨 | completed |  |
| `a11y.status.notCompleted` | 아직 안 함 | not completed |  |
| `a11y.progress` | 전체 {total}개 중 {completed}개 완료 | {completed} of {total} steps completed |  |
| `a11y.guardianEntryHint` | 보호자 모드로 들어가려면 길게 누르세요 | Long press to enter guardian mode |  |

## 11. 리뷰 체크리스트

- 한국어와 영어 key가 모두 존재한다.
- 아이 모드 문구는 큰 글자 설정에서도 충분히 짧다.
- locale 간 placeholder가 일치한다.
- TTS 문자열에는 대응되는 화면 문구가 있다.
- 아이 모드에는 destructive action 문구가 노출되지 않는다.
