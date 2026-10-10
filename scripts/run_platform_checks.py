"""Run actual platform suites; reject missing, stale, zero or skipped results."""
import hashlib
import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import uuid
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / ".workflow/M03"
settings = (ROOT / "local.properties").read_text() if (ROOT / "local.properties").exists() else ""
BUILD_ROOT = next((Path(line.split("=", 1)[1].strip()) for line in settings.splitlines()
                   if line.startswith("ddaraogae.buildRoot=")), None)


def build_dir(module):
    return BUILD_ROOT / module if BUILD_ROOT else ROOT / module / "build"


def xml_summary(directories, expected):
    groups, reports = [], []
    for directory, count in zip(directories, expected, strict=True):
        files = list(directory.glob("**/TEST-*.xml"))
        totals = dict(tests=0, skipped=0, failures=0, errors=0)
        for path in files:
            suite = ET.parse(path).getroot()
            for key in totals:
                totals[key] += int(suite.get(key, 0))
            reports.append({"path": str(path), "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
        groups.append({**totals, "expected": count})
    return {
        "tests_run": sum(g["tests"] for g in groups), "skipped": sum(g["skipped"] for g in groups),
        "complete": bool(groups) and all(g["tests"] == g["expected"] and g["tests"] > 0 for g in groups),
        "success": all(g["failures"] == g["errors"] == g["skipped"] == 0 for g in groups),
        "groups": groups, "xml": reports,
    }


def test_count(module, sets):
    return sum(len(re.findall(r"@Test\b", p.read_text()))
               for name in sets for p in (ROOT / module / "src" / name).glob("**/*.kt"))


def run(argv, profile, env=None):
    print("Running:", " ".join(argv), flush=True)
    with (WORK / f"{profile}.log").open("w") as log:
        result = subprocess.run(argv, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode:
        print((WORK / f"{profile}.log").read_text()[-7000:])
        raise SystemExit(result.returncode)


def main(profile, ticket="M03", scenarios=False, firebase=False, firebase_cloud=False, location=False):
    global WORK
    WORK = ROOT / f".workflow/{ticket}" / ("cloud" if firebase_cloud else "")
    WORK.mkdir(parents=True, exist_ok=True)
    # Product and scenario suites use different compile inputs. Preserve both
    # receipts so a later product check cannot erase scenario evidence.
    report = WORK / (f"{profile}-location-tests.json" if location else
                     f"{profile}-scenarios-tests.json" if scenarios and profile in {"android", "swift"}
                     else f"{profile}-tests.json")
    report.unlink(missing_ok=True)
    env = os.environ.copy()
    if sys.platform == "darwin" and not env.get("JAVA_HOME"):
        env["JAVA_HOME"] = subprocess.check_output(["/usr/libexec/java_home", "-v", "17"], text=True).strip()
    if profile in {"kmp", "jvm", "android"}:
        tasks, directories, expected = [], [], []
        if profile in {"kmp", "jvm"}:
            for module in ["domain", "data", "designsystem", "feature/walk", "feature/dogs", "feature/history", "sharedApp"]:
                targets = [("jvmTest", "jvmTest")]
                if profile == "kmp":
                    targets.append(("iosSimulatorArm64Test", "iosTest"))
                for target, source in targets:
                    count = test_count(module, ["commonTest", source])
                    # A UI-only module can have no unit tests. Its code still
                    # compiles in the Android APK and iOS framework below.
                    if count == 0:
                        continue
                    directory = build_dir(module) / "test-results" / target
                    shutil.rmtree(directory, ignore_errors=True)
                    directories.append(directory)
                    expected.append(count)
                    tasks.append(f":{module.replace('/', ':')}:{target}")
            if profile == "kmp":
                tasks += [":androidApp:assembleDebug", ":sharedApp:linkDebugFrameworkIosSimulatorArm64"]
            tasks.append("architectureCheck")
        else:
            directory = build_dir("androidApp") / "outputs/androidTest-results/connected"
            shutil.rmtree(directory, ignore_errors=True)
            shutil.rmtree(build_dir("androidApp") / "outputs/connected_android_test_additional_output", ignore_errors=True)
            directories.append(directory)
            expected.append(test_count("androidApp", ["androidTest", *(["scenarioAndroidTest"] if scenarios else []), *(["firebaseAndroidTest"] if firebase else [])]))
            tasks = [":androidApp:connectedDebugAndroidTest"]
        run(["./gradlew", *(["-Pddaraogae.scenarios=true"] if scenarios else []), *(["-Pddaraogae.firebaseTests=true"] if firebase else []),
             *(["-Pandroid.testInstrumentationRunnerArguments.firebaseCloudProject=ddaraogae-kmp"] if firebase_cloud else []), *tasks, "--console=plain"], profile, env)
        summary = xml_summary(directories, expected)
    elif profile == "swift":
        # The Xcode host consumes the Kotlin framework directly. Build the
        # matching variant here so a preceding normal/scenario run cannot
        # silently leave Xcode with the wrong exported Swift symbols.
        run(["./gradlew", *(["-Pddaraogae.scenarios=true"] if scenarios else []),
             ":sharedApp:linkDebugFrameworkIosSimulatorArm64", "--console=plain"], profile, env)
        devices = json.loads(subprocess.check_output(["xcrun", "simctl", "list", "devices", "available", "-j"], text=True))
        phones = [d for runtime, entries in devices["devices"].items() if "iOS" in runtime
                  for d in entries if d["name"].startswith("iPhone")]
        if not phones:
            raise SystemExit("No installed iPhone Simulator is available")
        device = os.environ.get("DDARAOGAE_IOS_DEVICE") or next((d["udid"] for d in phones if d["state"] == "Booted"), phones[0]["udid"])
        result = WORK / f"swift-{uuid.uuid4()}.xcresult"
        xcode_args = ["xcodebuild", "-project", "iosApp/iosApp.xcodeproj", "-scheme", "Ddaraogae", "-configuration", "Debug",
             "-destination", f"platform=iOS Simulator,id={device}", "-derivedDataPath", str(build_dir("iosHost")),
             f"FRAMEWORK_SEARCH_PATHS=$(inherited) {build_dir('sharedApp')}/bin/iosSimulatorArm64/debugFramework",
             # Simulator ad-hoc signing needs no Apple team and preserves Keychain entitlements.
             "CODE_SIGNING_ALLOWED=YES", "CODE_SIGN_IDENTITY=-",
             # Preserve SWIFT_PACKAGE in package targets, or Firebase imports its CocoaPods module names.
             *(["SWIFT_ACTIVE_COMPILATION_CONDITIONS=$(inherited) DEBUG DDARAOGAE_SCENARIOS"] if scenarios else []),
             *(["SWIFT_ACTIVE_COMPILATION_CONDITIONS=$(inherited) DEBUG DDARAOGAE_FIREBASE_TESTS" + (" DDARAOGAE_FIREBASE_CLOUD_TESTS" if firebase_cloud else "")] if firebase else []),
             *(["SWIFT_ACTIVE_COMPILATION_CONDITIONS=$(inherited) DEBUG DDARAOGAE_LOCATION_TESTS"] if location else [])]
        if location:
            run([*xcode_args, "clean", "build-for-testing"], "swift-location-build")
            app = build_dir("iosHost") / "Build/Products/Debug-iphonesimulator/Ddaraogae.app"
            subprocess.run(["xcrun", "simctl", "install", device, str(app)], check=True)
            subprocess.run(["xcrun", "simctl", "privacy", device, "grant", "location", "dev.ddaraogae.preview"], check=True)
            try:
                # The route lasts well beyond XCTest startup at walking speed.
                subprocess.run(["xcrun", "simctl", "location", device, "start", "--speed=1.4", "--interval=1",
                                "37.5665,126.9780", "37.6000,127.0000"], check=True)
                run([*xcode_args, "-resultBundlePath", str(result),
                     "-only-testing:DdaraogaeUITests/BootstrapTests/testSimulatorCoreLocationReachesSavedWalk",
                     "test-without-building"], "swift-location")
            finally:
                subprocess.run(["xcrun", "simctl", "location", device, "clear"], check=True)
        else:
            run([*xcode_args, "-resultBundlePath", str(result), "clean", "test"], profile)
        raw = json.loads(subprocess.check_output(["xcrun", "xcresulttool", "get", "test-results", "summary",
                                                "--path", str(result), "--format", "json"], text=True))
        expected = (1 if location else sum(len(re.findall(r"func test\w+\(", p.read_text().split("#if DDARAOGAE_LOCATION_TESTS")[0]))
                       for p in (ROOT / "iosApp/iosAppUITests").glob("*.swift")
                       if (scenarios or p.name != "ScenarioTests.swift") and (firebase or p.name != "FirebaseSmokeTests.swift")))
        summary = {"tests_run": raw["totalTestCount"], "skipped": raw["skippedTests"],
                   "success": raw["result"] == "Passed" and raw["failedTests"] == raw["expectedFailures"] == 0,
                   "complete": raw["totalTestCount"] == expected and expected > 0,
                   "result_bundle": str(result.relative_to(ROOT)), "xcode": raw}
    else:
        raise SystemExit("Usage: run_platform_checks.py kmp|jvm|android|swift")
    if profile in {"kmp", "jvm", "android"}:
        archive = WORK / f"{profile}-reports-{uuid.uuid4()}"
        archive.mkdir()
        artifacts = []
        for index, item in enumerate(summary["xml"]):
            source = Path(item["path"])
            target = archive / f"{index}-{source.name}"
            shutil.copyfile(source, target)
            artifacts.append({"path": str(target.relative_to(ROOT)), "sha256": item["sha256"]})
        summary["artifacts"] = artifacts
    else:
        summary["artifacts"] = [{"path": str(p.relative_to(ROOT)), "sha256": hashlib.sha256(p.read_bytes()).hexdigest()}
                                for p in result.rglob("*") if p.is_file()]
    if firebase:
        summary["firebase_project"] = "ddaraogae-kmp" if firebase_cloud else "demo-ddaraogae-kmp"
    report.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({k: summary[k] for k in ["tests_run", "skipped", "complete", "success"]}, ensure_ascii=False))
    return 0 if summary["success"] and summary["complete"] and summary["skipped"] == 0 else 1


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("profile", choices=["kmp", "jvm", "android", "swift"])
    parser.add_argument("--ticket", choices=["M03", "M04", "M05"], default="M03")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--scenarios", action="store_true")
    mode.add_argument("--firebase", action="store_true")
    mode.add_argument("--firebase-cloud", action="store_true", help="Explicit real new-project smoke; creates synthetic test accounts")
    mode.add_argument("--location", action="store_true", help="Drive iOS Simulator Core Location and assert stored walk")
    args = parser.parse_args()
    if args.firebase_cloud and (args.profile not in {"android", "swift"} or args.ticket != "M04"):
        parser.error("Cloud smoke requires android|swift --ticket M04")
    if args.location and (args.profile != "swift" or args.ticket != "M05"):
        parser.error("Location smoke requires swift --ticket M05")
    raise SystemExit(main(args.profile, args.ticket, args.scenarios, args.firebase or args.firebase_cloud, args.firebase_cloud, args.location))
