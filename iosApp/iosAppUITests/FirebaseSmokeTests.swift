#if DEBUG && DDARAOGAE_FIREBASE_TESTS
import XCTest

final class FirebaseSmokeTests: XCTestCase {
    @MainActor func testOfficialSdkUidIsolationAndCancellation() {
        let app = XCUIApplication()
        #if DDARAOGAE_FIREBASE_CLOUD_TESTS
        app.launchArguments = ["--firebase-cloud-smoke"]
        #else
        app.launchArguments = ["--firebase-smoke"]
        #endif
        app.launch()
        let outcome = app.staticTexts.matching(NSPredicate(format:
            "label BEGINSWITH 'Firebase smoke passed' OR label BEGINSWITH 'Firebase smoke failed'")).firstMatch
        XCTAssertTrue(outcome.waitForExistence(timeout: 90), app.debugDescription)
        XCTAssertEqual(outcome.label, "Firebase smoke passed")
        let proof = XCTAttachment(screenshot: app.screenshot())
        proof.name = "M04 official Firebase SDK UID smoke"
        proof.lifetime = .keepAlways
        add(proof)
    }
}
#endif
