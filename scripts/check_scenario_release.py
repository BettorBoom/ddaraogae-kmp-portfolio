"""Build real unsigned release outputs and reject fixture leakage."""
import argparse
import hashlib
import json
import os
import plistlib
import re
import shutil
import subprocess
import zipfile
from pathlib import Path
from run_platform_checks import ROOT, build_dir


def main(device=False):
    work = ROOT / (".workflow/M04" if device else ".workflow/M05")
    work.mkdir(parents=True, exist_ok=True)
    report = work / ("device-release.json" if device else "release.json")
    report.unlink(missing_ok=True)  # Standalone retries must not retain an earlier success.
    target = "IosArm64" if device else "IosSimulatorArm64"
    framework_task = f":sharedApp:linkReleaseFramework{target}"
    framework_path = build_dir("sharedApp") / f"bin/{target[0].lower() + target[1:]}/releaseFramework"
    env = os.environ.copy()
    if not env.get("JAVA_HOME"):
        env["JAVA_HOME"] = subprocess.check_output(["/usr/libexec/java_home", "-v", "17"], text=True).strip()
    for task in (":androidApp:preReleaseBuild", framework_task):
        result = subprocess.run(["./gradlew", "-Pddaraogae.scenarios=true", task, "--console=plain"], cwd=ROOT,
                                env=env, capture_output=True, text=True)
        output = result.stdout + result.stderr
        (work / ("release-guard-android.log" if "androidApp" in task else "release-guard-ios.log")).write_text(output)
        if result.returncode == 0 or "DDARAOGAE_SCENARIOS_RELEASE_FORBIDDEN" not in output:
            raise SystemExit(f"Release guard did not reject scenarios: {task}")
    result = subprocess.run(["./gradlew", "-Pddaraogae.firebaseTests=true", ":androidApp:preReleaseBuild", "--console=plain"],
                            cwd=ROOT, env=env, capture_output=True, text=True)
    (work / "release-guard-firebase.log").write_text(result.stdout + result.stderr)
    if result.returncode == 0 or "DDARAOGAE_FIREBASE_TESTS_RELEASE_FORBIDDEN" not in result.stdout + result.stderr:
        raise SystemExit("Release guard did not reject Firebase emulator mode")
    with (work / "release-build.log").open("w") as log:
        subprocess.run(["./gradlew", ":androidApp:assembleRelease", framework_task, "--console=plain"],
                       cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT, check=True)
    apk = build_dir("androidApp") / "outputs/apk/release/androidApp-release-unsigned.apk"
    header = framework_path / "SharedApp.framework/Headers/SharedApp.h"
    with zipfile.ZipFile(apk) as bundle:
        dex = b"".join(bundle.read(name) for name in bundle.namelist() if name.endswith(".dex"))
        for forbidden in (b"ScenarioActivity", b"DdaraogaeScenario", b"fixture-user", b"fixture-walk", b"demo-ddaraogae-kmp", b"10.0.2.2", b"FirebaseSmokeTest", b"firebaseCloudProject", b"@example.invalid"):
            if forbidden in dex: raise SystemExit(f"Android release contains fixture: {forbidden}")
    for forbidden in ("ScenarioIosEntry", "scenarioViewController", "createIosDemoRuntime", "DdaraogaeScenario"):
        if forbidden in header.read_text(): raise SystemExit(f"iOS release header contains fixture: {forbidden}")
    derived = build_dir("iosDeviceReleaseHost" if device else "iosReleaseHost")
    xcarchive = work / "device-release.xcarchive"
    xcode_args = ["xcodebuild", "-project", "iosApp/iosApp.xcodeproj", "-scheme", "Ddaraogae", "-configuration", "Release",
                  "-destination", "generic/platform=iOS" + ("" if device else " Simulator"),
                  "-derivedDataPath", str(derived), f"FRAMEWORK_SEARCH_PATHS=$(inherited) {framework_path}",
                  "CODE_SIGNING_ALLOWED=NO", "ARCHS=arm64"]
    if device:
        xcode_args += ["-archivePath", str(xcarchive), "DEBUG_INFORMATION_FORMAT=dwarf-with-dsym", "archive"]
    else:
        xcode_args += ["build"]
    with (work / "swift-release-build.log").open("w") as log:
        subprocess.run(xcode_args, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
    app = (xcarchive / "Products/Applications/Ddaraogae.app" if device else
           derived / "Build/Products/Release-iphonesimulator/Ddaraogae.app")
    binary = app / "Ddaraogae"
    for forbidden in (b"--scenario", b"--reset-preview", b"--demo-route", b"walks-demo.db", b"--ui-test", b"walks-ui-test.db", b"--location-test", b"walks-location-test.db", b"demo-route-1", b"fixture-user", b"fixture-walk", b"scenarioViewController", b"--firebase-smoke", b"--firebase-cloud-smoke", b"@example.invalid", b"demo-ddaraogae-kmp", b"Firebase smoke passed"):
        if forbidden in binary.read_bytes(): raise SystemExit(f"iOS release binary contains fixture: {forbidden}")
    details, extra_artifacts = {}, []
    if device:
        info = plistlib.loads((app / "Info.plist").read_bytes())
        macho = subprocess.check_output(["xcrun", "vtool", "-show-build", str(binary)], text=True)
        if info.get("CFBundleSupportedPlatforms") != ["iPhoneOS"] or not re.search(r"^\s*platform IOS$", macho, re.M):
            raise SystemExit("Device archive contains a non-iPhoneOS binary")
        if subprocess.check_output(["xcrun", "lipo", "-archs", str(binary)], text=True).strip() != "arm64":
            raise SystemExit("Device archive is not arm64")
        dsym = xcarchive / "dSYMs/Ddaraogae.app.dSYM/Contents/Resources/DWARF/Ddaraogae"
        uuids = [re.findall(r"UUID: ([0-9A-F-]+) \(arm64\)", subprocess.check_output(
            ["xcrun", "dwarfdump", "--uuid", str(p)], text=True)) for p in (binary, dsym)]
        if len(uuids[0]) != 1 or uuids[0] != uuids[1]:
            raise SystemExit("Device archive dSYM UUID does not match its app binary")
        signing = subprocess.run(["codesign", "-dv", str(app)], capture_output=True, text=True)
        if signing.returncode == 0 or "code object is not signed at all" not in signing.stderr:
            raise SystemExit("Expected an unsigned preparation archive")
        details = {"platform": "iPhoneOS", "architecture": "arm64", "dsym_uuid": uuids[0][0],
                   "bundle_id": info["CFBundleIdentifier"], "version": info["CFBundleShortVersionString"],
                   "build": info["CFBundleVersion"], "app_icon_configured": bool(info.get("CFBundleIcons")),
                   "archive_path": str(xcarchive.relative_to(ROOT)), "upload_ready": False}
        extra_artifacts = [app / "Info.plist", xcarchive / "Info.plist", dsym]
    archive = work / ("device-release" if device else "release")
    archive.mkdir(exist_ok=True)
    artifacts = []
    for source in (apk, header, binary, work / "swift-release-build.log", work / "release-build.log", work / "release-guard-android.log", work / "release-guard-ios.log", work / "release-guard-firebase.log"):
        target = archive / source.name
        shutil.copyfile(source, target)
        artifacts.append({"path": str(target.relative_to(ROOT)), "sha256": hashlib.sha256(target.read_bytes()).hexdigest()})
    artifacts += [{"path": str(p.relative_to(ROOT)), "sha256": hashlib.sha256(p.read_bytes()).hexdigest()}
                  for p in extra_artifacts]
    report.write_text(json.dumps({"unsigned_not_deployment": True, "release_guards_rejected": 3,
        "fixture_leaks": 0, **details, "artifacts": artifacts}, indent=2) + "\n")
    print("Unsigned Android/iOS release: fixtures absent; 3 guards rejected; " +
          ("iPhoneOS archive and dSYM verified; not signed or uploaded" if device else "Simulator build verified"))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ios-device", action="store_true", help="Build and inspect an unsigned iPhoneOS archive; never uploads")
    main(device=parser.parse_args().ios_device)
