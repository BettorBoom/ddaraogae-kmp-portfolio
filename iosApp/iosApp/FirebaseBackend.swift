import Foundation
import FirebaseAuth
import SharedApp

final class FirebaseBackend: NSObject, NativeBackend {
    private let auth: Auth
    init(auth: Auth) { self.auth = auth }
    func observeUser(observer: BackendUserObserver) -> CancelHandle {
        let lock = NSRecursiveLock()
        var closed = false
        let listener = auth.addStateDidChangeListener { _, user in
            lock.lock(); defer { lock.unlock() }
            if !closed { observer.onUser(userId: user?.uid) }
        }
        return FirebaseCancellation { [auth] in
            lock.lock()
            let remove = !closed
            closed = true
            lock.unlock()
            // Cancel can race a queued SDK callback; guard emission as well.
            if remove { auth.removeStateDidChangeListener(listener) }
        }
    }
}

private final class FirebaseCancellation: NSObject, CancelHandle {
    private let action: () -> Void
    init(_ action: @escaping () -> Void) { self.action = action }
    // Every caller must close the emission gate before returning. A generic
    // take-and-clear action can let the second caller return before the first closes it.
    func cancel() { action() }
}
