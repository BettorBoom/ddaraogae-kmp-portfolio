"""Archive actual M05 captures; missing state/theme captures are a failure."""
import hashlib
import json
import re
import shutil
import subprocess
import uuid
from pathlib import Path
from run_platform_checks import ROOT, build_dir


def main():
    work = ROOT / ".workflow/M05"
    archive = work / f"screens-{uuid.uuid4()}"
    archive.mkdir()
    expected = {f"{screen}-{state}-{theme}-2x" for screen in ("home", "walk", "record")
                for state in ("ready", "loading", "empty", "error", "denied", "long") for theme in ("light", "dark")}
    expected |= {f"{screen}-ready-light-1x" for screen in ("home", "walk", "record")}
    android_receipt = json.loads((work / "android-scenarios-tests.json").read_text())
    if not android_receipt["success"] or not android_receipt["complete"] or android_receipt["skipped"]:
        raise SystemExit("Android suite incomplete: screenshots cannot prove completion")
    android = {p.stem: p for p in (build_dir("androidApp") / "outputs/connected_android_test_additional_output").glob("**/m05/*.png")}
    swift = json.loads((work / "swift-scenarios-tests.json").read_text())
    if not swift["success"] or not swift["complete"] or swift["skipped"]:
        raise SystemExit("Swift suite incomplete: screenshots cannot prove completion")
    exported = archive / "xcattachments"
    subprocess.run(["xcrun", "xcresulttool", "export", "attachments", "--path", str(ROOT / swift["result_bundle"]),
                    "--output-path", str(exported)], check=True, capture_output=True)
    ios = {}
    for test in json.loads((exported / "manifest.json").read_text()):
        for attachment in test["attachments"]:
            match = re.match(r"(home|walk|record)-(ready|loading|empty|error|denied|long)-(light|dark)-(1x|2x)",
                             attachment["suggestedHumanReadableName"])
            if match:
                name = match.group(0)
                if name in ios: raise SystemExit(f"Duplicate iOS capture: {name}")
                ios[name] = exported / attachment["exportedFileName"]
    artifacts = []
    for platform, sources in (("android", android), ("ios", ios)):
        if set(sources) != expected:
            raise SystemExit(f"{platform}: missing={sorted(expected - sources.keys())}, extra={sorted(sources.keys() - expected)}")
        target = archive / platform
        target.mkdir()
        for name, source in sources.items():
            if not source.read_bytes().startswith(b"\x89PNG\r\n\x1a\n"):
                raise SystemExit(f"Not an actual PNG capture: {source}")
            copy = target / f"{name}.png"
            shutil.copyfile(source, copy)
            artifacts.append({"path": str(copy.relative_to(ROOT)), "sha256": hashlib.sha256(copy.read_bytes()).hexdigest()})
            # Keep a small, readable selection; the complete matrix stays in evidence.
            if name in {"home-ready-light-1x", "walk-ready-light-1x", "record-ready-light-1x",
                        "home-long-dark-2x", "walk-error-dark-2x", "record-empty-dark-2x"}:
                visible = ROOT / "docs/evidence/M05" / platform / f"{name}.png"
                visible.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(copy, visible)
    (work / "screens.json").write_text(json.dumps({"captures_per_platform": len(expected), "archive": str(archive.relative_to(ROOT)),
                                                   "artifacts": artifacts}, indent=2) + "\n")
    print("Actual PNG matrix archived: Android 39, iOS 39")


if __name__ == "__main__": main()
