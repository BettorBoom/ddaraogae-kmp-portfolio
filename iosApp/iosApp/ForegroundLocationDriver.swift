import CoreLocation
import SharedApp

final class ForegroundLocationDriver: NSObject, CLLocationManagerDelegate, ActiveWalkObserver {
    private let runtime: AppRuntime
    private let manager = CLLocationManager()
    private var subscription: CancelHandle?
    private var activeWalk: (id: String, segment: Int32)?
    private var foreground = false
    private var updating = false
    private var requestedAuthorization = false
    private var closed = false

    init(runtime: AppRuntime) {
        self.runtime = runtime
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.distanceFilter = 3
        subscription = runtime.observeActiveWalk(observer: self)
    }

    func onActiveWalk(walkId: String?, segment: Int32) {
        guard !closed else { return }
        let next = walkId.map { (id: $0, segment: segment) }
        if activeWalk?.id == next?.id && activeWalk?.segment == next?.segment { return }
        activeWalk = next
        syncLocation()
    }

    func setForeground(_ value: Bool) {
        guard !closed else { return }
        if foreground == value { return }
        foreground = value
        syncLocation()
    }

    private func syncLocation() {
        guard foreground, activeWalk != nil else { stopLocation(); return }
        switch manager.authorizationStatus {
        case .notDetermined:
            if !requestedAuthorization {
                requestedAuthorization = true
                runtime.setLocationNotice(message: "위치 권한을 허용하면 산책 거리와 경로를 기록해요.")
                manager.requestWhenInUseAuthorization()
            }
        case .authorizedWhenInUse, .authorizedAlways:
            if !updating { manager.startUpdatingLocation(); updating = true }
            runtime.setLocationNotice(message: manager.accuracyAuthorization == .reducedAccuracy
                ? "대략적 위치가 허용됐어요. 정확도가 낮으면 경로가 기록되지 않을 수 있어요." : nil)
        case .denied, .restricted:
            stopLocation()
            runtime.setLocationNotice(message: "위치 권한이 없어 시간만 기록해요. 설정에서 권한을 바꿀 수 있어요.")
        @unknown default:
            stopLocation()
            runtime.setLocationNotice(message: "위치 권한을 확인하지 못해 시간만 기록해요.")
        }
    }

    private func stopLocation() {
        if updating { manager.stopUpdatingLocation(); updating = false }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        guard !closed else { return }
        syncLocation()
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard !closed, foreground, updating, let walk = activeWalk else { return }
        var receivedValidCoordinate = false
        for location in locations where location.horizontalAccuracy >= 0 {
            let timestamp = location.timestamp.timeIntervalSince1970
            guard timestamp.isFinite, timestamp >= 0, timestamp < Double(Int64.max) / 1_000 else { continue }
            receivedValidCoordinate = true
            runtime.submitNativeLocation(walkId: walk.id, segment: walk.segment,
                latitude: location.coordinate.latitude, longitude: location.coordinate.longitude,
                recordedAtMillis: Int64(timestamp * 1_000),
                accuracyMeters: location.horizontalAccuracy)
        }
        if receivedValidCoordinate {
            runtime.setLocationNotice(message: manager.accuracyAuthorization == .reducedAccuracy
                ? "대략적 위치가 허용됐어요. 정확도가 낮으면 경로가 기록되지 않을 수 있어요." : nil)
        }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        guard !closed, foreground, activeWalk != nil, updating else { return }
        let code = (error as? CLError)?.code
        runtime.setLocationNotice(message: code == .denied
            ? "위치 서비스를 사용할 수 없어 시간만 기록해요."
            : "위치를 잠시 가져오지 못했어요. 시간 기록은 계속돼요.")
    }

    func close() {
        if closed { return }
        closed = true
        foreground = false
        activeWalk = nil
        subscription?.cancel()
        subscription = nil
        stopLocation()
        manager.delegate = nil
    }
}
