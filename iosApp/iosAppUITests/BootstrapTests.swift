import XCTest
import SharedApp

final class BootstrapTests: XCTestCase {
    @MainActor private func tap(_ button: XCUIElement, in app: XCUIApplication) {
        XCTAssertTrue(button.waitForExistence(timeout: 15))
        // Compose can report an offscreen button as hittable on iOS. Recheck
        // its viewport position before touching it.
        for _ in 0..<10 {
            let frame = button.frame
            if button.isHittable && frame.minY >= app.frame.minY + 60 &&
                frame.maxY <= app.frame.maxY - 30 { break }
            if frame.minY < app.frame.minY + 60 { app.swipeDown() }
            else { app.swipeUp() }
        }
        let frame = button.frame
        XCTAssertTrue(button.isHittable && frame.minY >= app.frame.minY + 60 &&
            frame.maxY <= app.frame.maxY - 30, app.debugDescription)
        var previous = CGRect.null
        let stable = NSPredicate { _, _ in
            let current = button.frame
            defer { previous = current }
            return current.width > 0 && abs(current.midY - previous.midY) < 1 &&
                abs(current.height - previous.height) < 1
        }
        XCTAssertEqual(XCTWaiter.wait(for: [XCTNSPredicateExpectation(predicate: stable, object: nil)],
                                 timeout: 10), .completed)
        let settled = button.frame
        XCTAssertTrue(button.isHittable && settled.minY >= app.frame.minY + 60 &&
            settled.maxY <= app.frame.maxY - 30, app.debugDescription)
        button.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
    }
    @MainActor
    func testWalkSurvivesNavigationAndRelaunch() {
        let app = XCUIApplication()
        app.launchArguments = ["--reset-preview"]
        app.launch()
        tap(app.buttons["반려견 이름 등록하기"], in: app)
        let dogName = app.descendants(matching: .any).matching(identifier: "dog-name-field").firstMatch
        XCTAssertTrue(dogName.waitForExistence(timeout: 10))
        dogName.tap()
        app.typeText("Bori")
        tap(app.buttons["이름 저장하기"], in: app)
        let home = app.buttons["산책"]
        XCTAssertTrue(home.waitForExistence(timeout: 15))
        home.tap()
        let start = app.buttons["산책 시작하기"]
        XCTAssertTrue(start.waitForExistence(timeout: 10))
        tap(start, in: app)
        let finish = app.buttons["산책 마치기"]
        // Time/distance cards push the action below the viewport once recording starts.
        for _ in 0..<8 where !finish.exists { app.swipeUp() }
        XCTAssertTrue(finish.exists, app.debugDescription)
        // Scaled/scrolling layouts need explicit movement back to the top too.
        for _ in 0..<8 where !app.buttons["홈으로"].isHittable { app.swipeDown() }
        tap(app.buttons["홈으로"], in: app)
        home.tap()
        for _ in 0..<8 where !finish.exists { app.swipeUp() }
        XCTAssertTrue(finish.exists, app.debugDescription)
        app.terminate()
        app.launchArguments = []
        app.launch()
        XCTAssertTrue(home.waitForExistence(timeout: 15))
        home.tap()
        let resume = app.buttons["산책 이어가기"]
        for _ in 0..<8 where !resume.exists { app.swipeUp() }
        tap(resume, in: app)
        for _ in 0..<8 where !finish.exists { app.swipeUp() }
        XCTAssertTrue(finish.exists, app.debugDescription)
        tap(finish, in: app)
        XCTAssertTrue(start.waitForExistence(timeout: 10))
        tap(app.buttons["산책 기록 보기"], in: app)
        XCTAssertTrue(app.staticTexts["함께한 시간을 저장했어요."].waitForExistence(timeout: 10))
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "M03 completed walk"
        screenshot.lifetime = .keepAlways
        add(screenshot)
        tap(app.buttons["primary-action"], in: app)
        XCTAssertTrue(app.buttons["1번째 산책 자세히 보기"].waitForExistence(timeout: 10))
        tap(app.buttons["1번째 산책 자세히 보기"], in: app)
        XCTAssertTrue(app.staticTexts["위치·지도 연결 전이에요. 실제 경로는 아직 표시하지 않아요."].waitForExistence(timeout: 10))
        let recordCapture = XCTAttachment(screenshot: app.screenshot())
        recordCapture.name = "M05 iOS local record without GPS"
        recordCapture.lifetime = .keepAlways
        add(recordCapture)
        tap(app.buttons["목록으로"], in: app)
        tap(app.buttons["홈으로"], in: app)
        XCTAssertTrue(home.waitForExistence(timeout: 10))
    }

    @MainActor
    func testSwiftSubscriptionReleasedOnRuntimeClose() {
        let probe = LocalBackendProbe()
        let cancelled = expectation(description: "Swift callback handle cancelled")
        probe.onCancel = { cancelled.fulfill() }
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try! FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let runtime = IosEntryKt.createIosRuntime(databasePath: directory.appendingPathComponent("probe.db").path, backend: probe)
        // Subscription registration is scheduled on the Kotlin Main dispatcher.
        let registered = expectation(description: "native subscription registration")
        DispatchQueue.main.async { registered.fulfill() }
        wait(for: [registered], timeout: 5)
        XCTAssertEqual(probe.subscribed, 1)
        runtime.close()
        wait(for: [cancelled], timeout: 5)
        runtime.close()
        XCTAssertEqual(probe.cancelled, 1)
        XCTAssertTrue(runtime.isClosed)
        try? FileManager.default.removeItem(at: directory)
    }
}
