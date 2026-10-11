"""Check declared Gradle project edges and forbidden domain/feature imports."""
import re
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def violations(root):
    allowed = {
        "domain": set(), "data": {"domain"}, "designsystem": set(),
        "feature:walk": {"domain", "designsystem"},
        "feature:dogs": {"domain", "designsystem"},
        "feature:history": {"domain", "designsystem"},
        "feature:mypage": {"designsystem"},
        "feature:stamp": {"domain", "designsystem"},
        "feature:auth": {"designsystem"},
        "sharedApp": {"domain", "data", "designsystem", "feature:walk", "feature:dogs", "feature:history", "feature:mypage",
                      "feature:stamp", "feature:auth"},
        "androidApp": {"sharedApp", "domain"},
    }
    errors = []
    graph = {}
    for module, targets in allowed.items():
        directory = root / module.replace(":", "/")
        text = (directory / "build.gradle.kts").read_text()
        edges = set(re.findall(r'project\("\:([^"\)]+)"\)', text))
        graph[module] = edges
        for target in edges - targets:
            errors.append(f"{module} → {target}: forbidden project edge")
        if module == "domain" and re.search(r'libs\.(room|koin|lifecycle)|compose|android\.kmp', text):
            errors.append("domain: forbidden build dependency/plugin")
        for source in directory.glob("src/**/*.kt"):
            # Test fixtures may use data only inside sharedApp/data, not feature.
            imports = re.findall(r'^import\s+([\w.]+)', source.read_text(), re.M)
            forbidden = ("android.", "androidx.", "org.koin.", "com.google.firebase.") if module == "domain" else ()
            if module.startswith("feature:"):
                forbidden = ("dev.ddaraogae.data.", "com.google.firebase.", "androidx.room.")
            for name in imports:
                if name.startswith(forbidden):
                    errors.append(f"{source.relative_to(root)}: forbidden import {name}")
    def visit(node, trail):
        if node in trail:
            errors.append("dependency cycle: " + " → ".join([*trail, node]))
            return
        for child in graph.get(node, ()):
            visit(child, [*trail, node])
    for node in graph:
        visit(node, [])
    shared_images = root / "designsystem/src/commonMain/composeResources/drawable"
    if shared_images.exists():
        android_images = root / "androidApp/src/main/assets/composeResources/dev.ddaraogae.designsystem.resources/drawable"
        for source in shared_images.glob("*.png"):
            target = android_images / source.name
            if not target.is_file() or hashlib.sha256(source.read_bytes()).digest() != hashlib.sha256(target.read_bytes()).digest():
                errors.append(f"Android packaged asset differs from shared source: {source.name}")
    return sorted(set(errors))


if __name__ == "__main__":
    failures = violations(ROOT)
    print("\n".join(failures) if failures else "Architecture: declared module edges and import boundaries pass")
    raise SystemExit(bool(failures))
