//
//  SteppieUITests.swift
//  SteppieUITests
//
//  Created by 전민돌 on 6/19/26.
//

import XCTest

final class SteppieUITests: XCTestCase {

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false

        // In UI tests it’s important to set the initial state - such as interface orientation - required for your tests before they run. The setUp method is a good place to do this.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    @MainActor
    func testFirstLaunchTutorialAppearsAndAdvances() throws {
        let app = XCUIApplication()
        app.launchArguments.append("-resetTutorialProgress")
        app.launch()

        let tutorialTitle = app.staticTexts["tutorial.title"]
        XCTAssertTrue(tutorialTitle.waitForExistence(timeout: 10))

        let nextButton = app.buttons["tutorial.primaryAction"]
        XCTAssertTrue(nextButton.exists)
        nextButton.tap()
        XCTAssertTrue(tutorialTitle.exists)
    }

    @MainActor
    func testLaunchPerformance() throws {
        // This measures how long it takes to launch your application.
        measure(metrics: [XCTApplicationLaunchMetric()]) {
            XCUIApplication().launch()
        }
    }
}
