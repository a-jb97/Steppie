import Testing
@testable import Steppie

@MainActor
struct FireworkBurstAnimationTimelineTests {
    @Test("폭죽 timeline은 시작 지연 중 취소되면 animation event를 전달하지 않는다")
    func cancellationDuringInitialDelayStopsAnimationEvents() async {
        var events: [FireworkBurstAnimationTimeline.Event] = []
        let task = Task { @MainActor in
            await FireworkBurstAnimationTimeline.run(initialDelay: 10) { event in
                events.append(event)
            }
        }

        task.cancel()
        await task.value

        #expect(events.isEmpty)
    }
}
