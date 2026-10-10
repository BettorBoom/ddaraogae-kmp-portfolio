#if DEBUG && DDARAOGAE_FIREBASE_TESTS
import SwiftUI
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import SharedApp

private final class SmokeObserver: NSObject, BackendUserObserver {
    private(set) var calls = 0
    private var latest: String?
    private var waiting: (String?, CheckedContinuation<Void, Never>)?
    func onUser(userId: String?) {
        precondition(Thread.isMainThread) // Official Auth listener contract; share the waiter's executor.
        calls += 1; latest = userId
        if let (expected, continuation) = waiting, expected == userId {
            waiting = nil; continuation.resume()
        }
    }
    // A nonisolated async waiter can miss a callback between checking and registration.
    @MainActor func waitFor(_ uid: String?) async {
        if calls > 0 && latest == uid { return }
        await withCheckedContinuation { waiting = (uid, $0) }
    }
}

struct FirebaseSmokeProbe: View {
    var cloud = false
    @State private var result = "Firebase smoke running"
    var body: some View {
        Text(result).padding().task {
            do { try await run(); result = "Firebase smoke passed" }
            catch {
                let error = error as NSError
                var status = ""
                if error.domain == AuthErrorDomain && error.code == AuthErrorCode.keychainError.rawValue,
                   let reason = error.localizedFailureReason,
                   let range = reason.range(of: #"\(-?\d+\)"#, options: .regularExpression) {
                    status = " / " + String(reason[range])
                }
                result = "Firebase smoke failed: \(error.domain) / \(error.code)\(status)"
            }
        }
    }
    @MainActor private func require(_ value: Bool) throws {
        if !value { throw NSError(domain: "SmokeAssertion", code: 1) }
    }
    @MainActor private func denied(_ action: () async throws -> Void) async throws {
        do { try await action() }
        catch {
            let error = error as NSError
            try require(error.domain == FirestoreErrorDomain && error.code == FirestoreErrorCode.permissionDenied.rawValue)
            return
        }
        throw NSError(domain: "UnexpectedPermission", code: 1)
    }
    @MainActor private func run() async throws {
        let options: FirebaseOptions
        if cloud {
            // Match the running host as well as the config; a renamed Xcode target is not this app.
            guard Bundle.main.bundleIdentifier == "dev.ddaraogae.preview",
                  let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
                  let configured = FirebaseOptions(contentsOfFile: path), configured.projectID == "ddaraogae-kmp",
                  configured.bundleID == "dev.ddaraogae.preview" else {
                throw NSError(domain: "CloudProjectMismatch", code: 1)
            }
            options = configured
        } else {
            options = FirebaseOptions(googleAppID: "1:123456789:ios:0123456789abcdef", gcmSenderID: "123456789")
            options.projectID = "demo-ddaraogae-kmp"
            options.apiKey = "AIza" + String(repeating: "0", count: 35)
        }
        FirebaseApp.configure(name: "uid-smoke", options: options)
        let app = FirebaseApp.app(name: "uid-smoke")!
        let auth = Auth.auth(app: app)
        if !cloud { auth.useEmulator(withHost: "127.0.0.1", port: 9099) }
        let db = Firestore.firestore(app: app)
        let settings = db.settings
        if !cloud { settings.host = "127.0.0.1:8080"; settings.isSSLEnabled = false }
        settings.cacheSettings = MemoryCacheSettings()
        db.settings = settings
        let backend = FirebaseBackend(auth: auth)
        let observer = SmokeObserver()
        let handle = backend.observeUser(observer: observer)
        defer { handle.cancel(); try? auth.signOut() }
        func signInTestUser() async throws -> String {
            if !cloud { return try await auth.signInAnonymously().user.uid }
            return try await auth.createUser(withEmail: "m04-\(UUID().uuidString)@example.invalid",
                                             password: UUID().uuidString).user.uid
        }
        result = "Firebase smoke running: initial auth"
        try auth.signOut()
        await observer.waitFor(nil)
        result = "Firebase smoke running: sign in A"
        let alice = try await signInTestUser()
        result = "Firebase smoke running: auth callback A"
        await observer.waitFor(alice)
        let path = db.document("users/\(alice)")
        result = "Firebase smoke running: owner write"
        try await path.setData(["uid": alice, "schemaVersion": 1, "lifecycle": "active"])
        result = "Firebase smoke running: server read"
        let own = try await path.getDocument(source: .server)
        try require(own.get("uid") as? String == alice)
        let firstCancel = Task.detached { handle.cancel() }
        let secondCancel = Task.detached { handle.cancel() }
        await firstCancel.value; await secondCancel.value
        handle.cancel()
        let stoppedAt = observer.calls
        let barrier = SmokeObserver()
        let second = backend.observeUser(observer: barrier)
        defer { second.cancel() }
        result = "Firebase smoke running: signed out callback"
        try auth.signOut()
        await barrier.waitFor(nil)
        result = "Firebase smoke running: unauthenticated denial"
        try await denied { _ = try await path.getDocument(source: .server) }
        result = "Firebase smoke running: sign in B"
        let bob = try await signInTestUser()
        result = "Firebase smoke running: auth callback B"
        await barrier.waitFor(bob)
        try require(bob != alice && observer.calls == stoppedAt)
        try await denied { _ = try await path.getDocument(source: .server) }
        // Keep A's document valid so only B's ownership can reject the write.
        try await denied { try await path.setData(["uid": alice, "schemaVersion": 1, "lifecycle": "active"]) }
        try await db.terminate()
    }
}
#endif
