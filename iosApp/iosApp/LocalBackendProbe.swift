import Foundation
import SharedApp

// Local preview fallback and native lifetime test probe. Never a Firebase UID.
final class LocalBackendProbe: NSObject, NativeBackend {
    var subscribed = 0
    var cancelled = 0
    var onCancel: (() -> Void)?

    func observeUser(observer: BackendUserObserver) -> CancelHandle {
        subscribed += 1
        observer.onUser(userId: "local-preview")
        return BackendCancellation { [weak self] in
            self?.cancelled += 1
            self?.onCancel?()
        }
    }
}

final class BackendCancellation: NSObject, CancelHandle {
    private var action: (() -> Void)?
    private let lock = NSLock()
    init(_ action: @escaping () -> Void) { self.action = action }
    func cancel() {
        lock.lock()
        let current = action
        action = nil
        lock.unlock()
        current?()
    }
}
