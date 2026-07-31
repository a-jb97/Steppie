import Foundation

struct ChildRoutinePlanResolution {
    let activeRoutineSet: RoutineSet?
    let routines: [Routine]
    let nextScheduledRoutineSet: RoutineSet?
    let nextScheduledStartDate: Date?
    let selectedRoutineID: UUID?
}

enum ChildRoutinePolicy {
    static func currentRoutine(
        in routines: [Routine],
        completedRoutineIDs: Set<UUID>
    ) -> Routine? {
        routines.first { !completedRoutineIDs.contains($0.id) }
    }

    static func isAllCompleted(
        plannedRoutines: [Routine],
        completedRoutineIDs: Set<UUID>
    ) -> Bool {
        !plannedRoutines.isEmpty
            && plannedRoutines.allSatisfy { completedRoutineIDs.contains($0.id) }
    }

    static func nextRoutineAfterFeedback(
        completionFeedbackRoutineID: UUID?,
        routines: [Routine],
        completedRoutineIDs: Set<UUID>
    ) -> Routine? {
        guard let completionFeedbackRoutineID,
              let completedRoutine = routines.first(where: { $0.id == completionFeedbackRoutineID }) else {
            return nil
        }
        return routines.first {
            $0.order > completedRoutine.order && !completedRoutineIDs.contains($0.id)
        }
    }

    static func nextRoutineSetAfterFeedback(
        activeRoutineSet: RoutineSet?,
        routines: [Routine],
        plannedRoutineSets: [RoutineSet],
        plannedRoutines: [Routine],
        completedRoutineIDs: Set<UUID>
    ) -> (routineSet: RoutineSet, firstRoutine: Routine)? {
        guard let activeRoutineSet,
              routines.allSatisfy({ completedRoutineIDs.contains($0.id) }),
              let currentIndex = plannedRoutineSets.firstIndex(where: { $0.id == activeRoutineSet.id })
        else {
            return nil
        }
        for routineSet in plannedRoutineSets.dropFirst(currentIndex + 1) {
            if let firstRoutine = plannedRoutines.first(where: {
                $0.routineSetID == routineSet.id && !completedRoutineIDs.contains($0.id)
            }) {
                return (routineSet, firstRoutine)
            }
        }
        return nil
    }

    static func cardState(
        for routine: Routine,
        currentRoutineID: UUID?,
        completionFeedbackRoutineID: UUID?,
        completedRoutineIDs: Set<UUID>
    ) -> RoutineCardState {
        if routine.id == completionFeedbackRoutineID || completedRoutineIDs.contains(routine.id) {
            return .completed
        }
        if completionFeedbackRoutineID != nil {
            return .upcoming
        }
        return routine.id == currentRoutineID ? .current : .upcoming
    }

    static func resolvePlan(
        routineSets: [RoutineSet],
        routines: [Routine],
        completedRoutineIDs: Set<UUID>,
        selectedRoutineID: UUID?,
        at date: Date,
        calendar: Calendar
    ) -> ChildRoutinePlanResolution {
        for routineSet in routineSets {
            let setRoutines = routines.filter { $0.routineSetID == routineSet.id }
            guard !setRoutines.isEmpty,
                  setRoutines.contains(where: { !completedRoutineIDs.contains($0.id) }) else {
                continue
            }

            if let startTime = routineSet.dailyStartTime,
               let startDate = scheduledDate(for: startTime, on: date, calendar: calendar),
               startDate > date {
                return ChildRoutinePlanResolution(
                    activeRoutineSet: nil,
                    routines: [],
                    nextScheduledRoutineSet: routineSet,
                    nextScheduledStartDate: startDate,
                    selectedRoutineID: nil
                )
            }

            let currentRoutine = currentRoutine(
                in: setRoutines,
                completedRoutineIDs: completedRoutineIDs
            )
            let resolvedSelection: UUID?
            if setRoutines.contains(where: { $0.id == selectedRoutineID })
                && selectedRoutineID.map(completedRoutineIDs.contains) != true {
                resolvedSelection = selectedRoutineID
            } else {
                resolvedSelection = currentRoutine?.id
            }
            return ChildRoutinePlanResolution(
                activeRoutineSet: routineSet,
                routines: setRoutines,
                nextScheduledRoutineSet: nil,
                nextScheduledStartDate: nil,
                selectedRoutineID: resolvedSelection
            )
        }

        guard let lastSet = routineSets.last else {
            return ChildRoutinePlanResolution(
                activeRoutineSet: nil,
                routines: [],
                nextScheduledRoutineSet: nil,
                nextScheduledStartDate: nil,
                selectedRoutineID: nil
            )
        }
        return ChildRoutinePlanResolution(
            activeRoutineSet: lastSet,
            routines: routines.filter { $0.routineSetID == lastSet.id },
            nextScheduledRoutineSet: nil,
            nextScheduledStartDate: nil,
            selectedRoutineID: nil
        )
    }

    static func scheduledDate(
        for time: LocalTime,
        on date: Date,
        calendar: Calendar
    ) -> Date? {
        var components = calendar.dateComponents([.year, .month, .day], from: date)
        components.hour = time.hour
        components.minute = time.minute
        components.second = 0
        return calendar.date(from: components)
    }

    static func routineSetScheduleSort(_ lhs: RoutineSet, _ rhs: RoutineSet) -> Bool {
        guard let lhsTime = lhs.dailyStartTime, let rhsTime = rhs.dailyStartTime else {
            return lhs.dailyStartTime != nil
        }
        if lhsTime == rhsTime { return lhs.createdAt < rhs.createdAt }
        return (lhsTime.hour, lhsTime.minute) < (rhsTime.hour, rhsTime.minute)
    }
}
