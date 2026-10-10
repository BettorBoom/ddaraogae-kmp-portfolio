import SwiftUI
import SharedApp
import FirebaseCore
import FirebaseAuth

@main
struct DdaraogaeApp: App {
    @StateObject private var model = AppModel()
    @Environment(\.scenePhase) private var scenePhase
    var body: some Scene {
        WindowGroup {
            #if DEBUG && DDARAOGAE_FIREBASE_TESTS
            if ProcessInfo.processInfo.arguments.contains("--firebase-smoke") || ProcessInfo.processInfo.arguments.contains("--firebase-cloud-smoke") {
                FirebaseSmokeProbe(cloud: ProcessInfo.processInfo.arguments.contains("--firebase-cloud-smoke"))
            } else { productScreen }
            #else
            #if DDARAOGAE_SCENARIOS
            if ProcessInfo.processInfo.arguments.contains("--scenario") {
                ScenarioScreen().ignoresSafeArea()
                    .preferredColorScheme(ProcessInfo.processInfo.arguments.contains("--dark") ? .dark : .light)
            } else {
                productScreen
            }
            #else
            productScreen
            #endif
            #endif
        }
    }
    @ViewBuilder private var productScreen: some View {
            if let runtime = model.runtime {
                SharedScreen(runtime: runtime).ignoresSafeArea()
                    .onChange(of: scenePhase) { phase in
                        if phase == .background { runtime.pauseForBackground() }
                    }
            } else {
                VStack(spacing: 20) {
                    Text("기록 저장 공간을 열지 못했어요.")
                    Button("다시 시도") { model.prepare() }
                }.padding()
            }
    }
}

#if DDARAOGAE_SCENARIOS
private struct ScenarioScreen: UIViewControllerRepresentable {
    private func argument(_ name: String, fallback: String) -> String {
        let args = ProcessInfo.processInfo.arguments
        guard let index = args.firstIndex(of: name), index + 1 < args.count else { return fallback }
        return args[index + 1]
    }
    func makeUIViewController(context: Context) -> UIViewController {
        // The gallery captures static states; native transition frames are
        // excluded just as in Android instrumented scenario checks (ADR-006).
        UIView.setAnimationsEnabled(false)
        return ScenarioIosEntryKt.scenarioViewController(screen: argument("--scenario", fallback: "gallery"),
            stateName: argument("--state", fallback: "ready"),
            dark: ProcessInfo.processInfo.arguments.contains("--dark"),
            fontScale: Float(argument("--font-scale", fallback: "0")) ?? 0)
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
#endif

private final class AppModel: ObservableObject {
    @Published private(set) var runtime: AppRuntime?
    init() {
        #if DEBUG && DDARAOGAE_FIREBASE_TESTS
        if ProcessInfo.processInfo.arguments.contains("--firebase-smoke") || ProcessInfo.processInfo.arguments.contains("--firebase-cloud-smoke") { return }
        #endif
        prepare()
    }
    func prepare() {
        let directory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        do { try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true) }
        catch { return }
        #if DDARAOGAE_SCENARIOS
        let demo = ProcessInfo.processInfo.arguments.contains("--demo-route")
        #else
        let demo = false
        #endif
        let path = directory.appendingPathComponent(demo ? "walks-demo.db" : "walks.db").path
        #if DEBUG
        // Test reset is absent from Release (ADR-006).
        if ProcessInfo.processInfo.arguments.contains("--reset-preview") {
            for suffix in ["", "-wal", "-shm"] { try? FileManager.default.removeItem(atPath: path + suffix) }
        }
        #endif
        if FirebaseApp.app() == nil, Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
        }
        let backend: NativeBackend = LocalBackendProbe()
        #if DDARAOGAE_SCENARIOS
        if demo {
            runtime = ScenarioIosEntryKt.createIosDemoRuntime(databasePath: path, backend: backend)
            return
        }
        #endif
        runtime = IosEntryKt.createIosRuntime(databasePath: path, backend: backend)
    }
    deinit { runtime?.close() }
}

private struct SharedScreen: UIViewControllerRepresentable {
    let runtime: AppRuntime
    func makeUIViewController(context: Context) -> UIViewController {
        IosEntryKt.mainViewController(runtime: runtime)
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
