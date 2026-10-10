import importlib.util
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("architecture", ROOT / "scripts/architecture_check.py")
architecture = importlib.util.module_from_spec(spec)
spec.loader.exec_module(architecture)


class ArchitectureChecks(unittest.TestCase):
    def test_rejects_forbidden_edges_and_domain_platform_imports(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            for module in ["domain", "data", "designsystem", "feature/walk", "feature/dogs", "feature/history", "sharedApp", "androidApp"]:
                directory = root / module
                directory.mkdir(parents=True)
                (directory / "build.gradle.kts").write_text("")
            (root / "feature/walk/build.gradle.kts").write_text('implementation(project(":data"))')
            source = root / "domain/src/commonMain/Bad.kt"
            source.parent.mkdir(parents=True)
            source.write_text("import androidx.room.Room\n")
            failures = architecture.violations(root)
            self.assertTrue(any("feature:walk → data" in value for value in failures))
            self.assertTrue(any("androidx.room.Room" in value for value in failures))


if __name__ == "__main__":
    unittest.main()
