#if DDARAOGAE_SCENARIOS
import XCTest
import CoreGraphics
import ImageIO

final class ScenarioTests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }
    private func bannerIsPainted(_ png: Data, frame: CGRect, screenWidth: CGFloat) -> Bool {
        guard let source = CGImageSourceCreateWithData(png as CFData, nil),
              let image = CGImageSourceCreateImageAtIndex(source, 0, nil), screenWidth > 0 else { return false }
        let scale = CGFloat(image.width) / screenWidth
        guard let crop = image.cropping(to: frame.applying(CGAffineTransform(scaleX: scale, y: scale)).integral),
              crop.width >= 3, crop.height > 0 else { return false }
        var pixels = [UInt8](repeating: 0, count: crop.width * crop.height * 4)
        return pixels.withUnsafeMutableBytes { (raw: UnsafeMutableRawBufferPointer) in
            guard let context = CGContext(data: raw.baseAddress, width: crop.width, height: crop.height,
                bitsPerComponent: 8, bytesPerRow: crop.width * 4, space: CGColorSpaceCreateDeviceRGB(),
                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { return false }
            context.draw(crop, in: CGRect(x: 0, y: 0, width: crop.width, height: crop.height))
            // ponytail: solid debug-banner background only; use a visual baseline
            // if the banner gains graphics. Require text in all thirds.
            for section in 0..<3 {
                var ink = 0
                for y in 0..<crop.height {
                    for x in (crop.width * section / 3)..<(crop.width * (section + 1) / 3) {
                        let index = (y * crop.width + x) * 4
                        if (0..<3).contains(where: { abs(Int(raw[index + $0]) - Int(raw[$0])) > 8 }) { ink += 1 }
                    }
                }
                if ink < 20 { return false }
            }
            return true
        }
    }
    @MainActor private func paintedCapture(banner: XCUIElement, app: XCUIApplication) -> Data {
        // Decode, check and attach the same frozen PNG bytes; XCUIScreenshot's
        // image and attachment representations did not give consistent evidence.
        var capture = XCUIScreen.main.screenshot().pngRepresentation
        let painted = NSPredicate { _, _ in
            capture = XCUIScreen.main.screenshot().pngRepresentation
            return self.bannerIsPainted(capture, frame: banner.frame, screenWidth: app.frame.width)
        }
        let expectation = XCTNSPredicateExpectation(predicate: painted, object: nil)
        XCTAssertEqual(XCTWaiter.wait(for: [expectation], timeout: 10), .completed, "Banner exists in accessibility but is absent from pixels")
        return capture
    }
    @MainActor private func waitForStableFrame(_ element: XCUIElement) {
        var previous = CGRect.null
        let stable = NSPredicate { _, _ in
            guard element.exists else { return false }
            let current = element.frame
            defer { previous = current }
            return current.width > 0 && current.height > 0
                && abs(current.midY - previous.midY) < 1 && abs(current.height - previous.height) < 1
        }
        let expectation = XCTNSPredicateExpectation(predicate: stable, object: nil)
        XCTAssertEqual(XCTWaiter.wait(for: [expectation], timeout: 10), .completed)
    }
    @MainActor private func reveal(_ element: XCUIElement, in app: XCUIApplication) {
        // Compose accessibility can report isHittable for an offscreen node.
        // Require its whole frame inside the viewport before sending a touch.
        for _ in 0..<10 {
            if element.exists {
                let frame = element.frame
                if element.isHittable && frame.height >= 48 && frame.minY > app.frame.minY + 120 && frame.maxY < app.frame.maxY - 40 {
                    // XCUITest idling does not wait for Compose scroll inertia.
                    // Observe a stable frame before choosing the touch position.
                    waitForStableFrame(element)
                    let settled = element.frame
                    if settled.height >= 48 && element.isHittable && settled.minY > app.frame.minY + 120 && settled.maxY < app.frame.maxY - 40 { return }
                }
            }
            app.swipeUp()
        }
        XCTFail("Action could not be revealed: \(app.debugDescription)")
    }
    @MainActor
    func testScenarioMatrixAndRetry() {
        let app = XCUIApplication()
        for screen in ["home", "walk", "record"] {
            for state in ["ready", "loading", "empty", "error", "denied", "long"] {
                for dark in [false, true] {
                    app.launchArguments = ["--scenario", screen, "--state", state, "--font-scale", "2"] + (dark ? ["--dark"] : [])
                    app.launch()
                    // Heading semantics can expose a different native element type.
                    let title = app.descendants(matching: .any).matching(identifier: "screen-title").firstMatch
                    XCTAssertTrue(title.waitForExistence(timeout: 15), "\(screen)/\(state)/\(dark)\n\(app.debugDescription)")
                    let banner = app.staticTexts["개발용 예시 데이터 · \(state)"]
                    XCTAssertTrue(banner.waitForExistence(timeout: 15))
                    waitForStableFrame(banner)
                    XCTAssertGreaterThanOrEqual(banner.frame.minY, 40)
                    let capture = XCTAttachment(data: paintedCapture(banner: banner, app: app), uniformTypeIdentifier: "public.png")
                    capture.name = "\(screen)-\(state)-\(dark ? "dark" : "light")-2x"
                    capture.lifetime = .keepAlways
                    add(capture)
                    if state == "error" {
                        let retry = app.buttons["다시 시도"]
                        reveal(retry, in: app)
                        XCTAssertTrue(retry.isHittable)
                        print("M05 revealed retry: \(retry.frame)")
                        let visible = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
                        visible.name = "retry-visible-\(screen)-\(dark)"; visible.lifetime = .keepAlways; add(visible)
                        retry.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
                        // The fixed debug banner reports state even after the error
                        // card scrolls offscreen; its disappearance alone proves nothing.
                        XCTAssertTrue(app.staticTexts["개발용 예시 데이터 · ready"].waitForExistence(timeout: 5), app.debugDescription)
                    }
                    app.terminate()
                }
            }
        }
    }
    @MainActor
    func testBasicScreenCaptures() {
        let app = XCUIApplication()
        for screen in ["home", "walk", "record"] {
            app.launchArguments = ["--scenario", screen, "--state", "ready", "--font-scale", "1"]
            app.launch()
            let banner = app.staticTexts["개발용 예시 데이터 · ready"]
            XCTAssertTrue(banner.waitForExistence(timeout: 15))
            waitForStableFrame(banner)
            XCTAssertGreaterThanOrEqual(banner.frame.minY, 40)
            let capture = XCTAttachment(data: paintedCapture(banner: banner, app: app), uniformTypeIdentifier: "public.png")
            capture.name = "\(screen)-ready-light-1x"; capture.lifetime = .keepAlways; add(capture)
            app.terminate()
        }
    }
    @MainActor
    func testDebugSyntheticRouteIsMarkedAndReopens() {
        let app = XCUIApplication()
        app.launchArguments = ["--demo-route", "--reset-preview"]
        app.launch()
        XCTAssertTrue(app.staticTexts["개발용 합성 경로 · 실제 GPS가 아니에요."].waitForExistence(timeout: 15))
        let history = app.buttons["전체 보기"]
        reveal(history, in: app)
        history.tap()
        let detail = app.buttons["1번째 산책 자세히 보기"]
        reveal(detail, in: app)
        detail.tap()
        XCTAssertTrue(app.staticTexts["샘플 지도 위 실제 좌표의 상대 이동선 · 지리적 위치 불일치"].waitForExistence(timeout: 10))
        app.terminate()
        app.launchArguments = ["--demo-route"]
        app.launch()
        XCTAssertTrue(app.staticTexts["개발용 합성 경로 · 실제 GPS가 아니에요."].waitForExistence(timeout: 15))
        let historyAgain = app.buttons["전체 보기"]
        reveal(historyAgain, in: app)
        historyAgain.tap()
        XCTAssertTrue(app.buttons["1번째 산책 자세히 보기"].waitForExistence(timeout: 10))
    }
}
#endif
