"""Compare the real generated ObjC surface against the recorded API baseline."""
import difflib
from pathlib import Path
from run_platform_checks import build_dir

root = Path(__file__).resolve().parents[1]
actual = build_dir("sharedApp") / "bin/iosSimulatorArm64/debugFramework/SharedApp.framework/Headers/SharedApp.h"
baseline = root / "docs/api/SharedApp.h"
current = actual.read_text()
for token in ["WalkViewModel", "RoomDatabase", "Koin", "Kotlinx_coroutines_core"]:
    if token in current:
        raise SystemExit(f"Unexpected Swift surface: {token}")
if current != baseline.read_text():
    print("".join(difflib.unified_diff(baseline.read_text().splitlines(True), current.splitlines(True),
                                    fromfile="baseline", tofile="generated")))
    raise SystemExit("Swift API changed: inspect the change before updating the baseline")
print("Generated Swift API matches baseline; no Room/Koin/ViewModel/Coroutine protocols exposed")
