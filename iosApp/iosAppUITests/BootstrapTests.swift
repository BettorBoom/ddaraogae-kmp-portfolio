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
    // Compose exposes testTag as the accessibility identifier; labels carry the visible text.
    @MainActor private func element(_ identifier: String, in app: XCUIApplication) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: identifier).firstMatch
    }
    @MainActor private func text(_ label: String, in app: XCUIApplication) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", label)).firstMatch
    }
    @MainActor private func registerDog(_ app: XCUIApplication) {
        tap(element("home-dog-add", in: app), in: app)
        let dogName = element("dog-name-field", in: app)
        XCTAssertTrue(dogName.waitForExistence(timeout: 10))
        dogName.tap()
        // The trailing newline is the keyboard's Done action, which also hides the keyboard.
        app.typeText("Bori\n")
        tap(element("primary-action", in: app), in: app)
        XCTAssertTrue(element("home-dog", in: app).waitForExistence(timeout: 15))
    }
    @MainActor
    func testWalkSurvivesNavigationAndRelaunch() {
        let app = XCUIApplication()
        // Keep the persistence UI test independent of the system location dialog.
        app.launchArguments = ["--ui-test", "--reset-preview", "--disable-location-for-ui-tests"]
        app.launch()
        registerDog(app)
        tap(element("tab-walk", in: app), in: app)
        let start = app.buttons["산책하기"]
        XCTAssertTrue(start.waitForExistence(timeout: 10))
        tap(start, in: app)
        let stop = app.buttons["산책 종료"]
        XCTAssertTrue(stop.waitForExistence(timeout: 10), app.debugDescription)
        tap(element("tab-home", in: app), in: app)
        XCTAssertTrue(text("반가워요!", in: app).waitForExistence(timeout: 10))
        tap(element("tab-walk", in: app), in: app)
        XCTAssertTrue(stop.waitForExistence(timeout: 10), app.debugDescription)
        app.terminate()
        app.launchArguments = ["--ui-test", "--disable-location-for-ui-tests"]
        app.launch()
        tap(element("tab-walk", in: app), in: app)
        let resume = app.buttons["산책 이어가기"]
        XCTAssertTrue(resume.waitForExistence(timeout: 15), app.debugDescription)
        tap(resume, in: app)
        XCTAssertTrue(stop.waitForExistence(timeout: 10), app.debugDescription)
        tap(stop, in: app)
        XCTAssertTrue(text("산책 완료", in: app).waitForExistence(timeout: 10), app.debugDescription)
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "M03 completed walk"
        screenshot.lifetime = .keepAlways
        add(screenshot)
        XCTAssertTrue(text("저장된 위치가 없어요. 앱이 열린 동안 위치 권한을 허용하면 경로를 기록해요.", in: app).exists)
        tap(app.buttons["산책 끝!"], in: app)
        XCTAssertTrue(start.waitForExistence(timeout: 10))
        tap(element("tab-home", in: app), in: app)
        XCTAssertTrue(text("금방 산책을 다녀왔어요!", in: app).waitForExistence(timeout: 10))
        tap(element("primary-action", in: app), in: app)
        XCTAssertTrue(text("산책 모아보기", in: app).waitForExistence(timeout: 10))
        let history = XCTAttachment(screenshot: app.screenshot())
        history.name = "M05 iOS history"
        history.lifetime = .keepAlways
        add(history)
        tap(element("back", in: app), in: app)
        XCTAssertTrue(text("반가워요!", in: app).waitForExistence(timeout: 10))
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

    #if DDARAOGAE_LOCATION_TESTS
    @MainActor
    func testSimulatorCoreLocationReachesSavedWalk() {
        let app = XCUIApplication()
        app.launchArguments = ["--location-test", "--reset-preview"]
        app.launch()
        registerDog(app)
        tap(element("tab-walk", in: app), in: app)
        tap(app.buttons["산책하기"], in: app)
        // A separate host runner grants When In Use and drives simctl location.
        // Require a nonzero stored distance, shown in the walking bar as "N m".
        let distance = app.staticTexts.matching(NSPredicate(format: "label MATCHES %@", "[1-9][0-9]* m")).firstMatch
        XCTAssertTrue(distance.waitForExistence(timeout: 90), app.debugDescription)
        tap(app.buttons["산책 종료"], in: app)
        XCTAssertTrue(text("산책 완료", in: app).waitForExistence(timeout: 10))
        let settled = NSPredicate { _, _ in self.text("산책 완료", in: app).exists && self.text("산책 완료", in: app).frame.minX < 400 }
        XCTAssertEqual(XCTWaiter.wait(for: [XCTNSPredicateExpectation(predicate: settled, object: nil)],
                                 timeout: 10), .completed, "완료 화면 전환이 끝나기 전에 캡처됨")
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label MATCHES %@", "[1-9][0-9]* m"))
            .firstMatch.waitForExistence(timeout: 10), "완료 화면의 저장 거리 누락")
        let capture = XCTAttachment(screenshot: app.screenshot())
        capture.name = "M10 iOS Core Location persisted walk"
        capture.lifetime = .keepAlways
        add(capture)
        XCTAssertFalse(text("저장된 위치가 없어요. 앱이 열린 동안 위치 권한을 허용하면 경로를 기록해요.", in: app).exists)
        app.terminate()
        app.launchArguments = ["--location-test"]
        app.launch()
        tap(element("primary-action", in: app), in: app)
        XCTAssertTrue(text("산책 모아보기", in: app).waitForExistence(timeout: 15))
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label MATCHES %@", "[1-9][0-9]* m"))
            .firstMatch.waitForExistence(timeout: 10), "재실행 후 저장 거리 복원 실패")
        tap(element("history-map", in: app), in: app)
        XCTAssertTrue(element("map-dialog", in: app).waitForExistence(timeout: 10), "재실행 후 저장 좌표 복원 실패")
    }
    #endif
}
