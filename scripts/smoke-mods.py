#!/usr/bin/env python3
import argparse
import os
from pathlib import Path
import queue
import shutil
import subprocess
import tempfile
import threading
import time
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[1]
SMOKE = ROOT / ".smoke"
FABRIC_MAVEN = "https://maven.fabricmc.net"
NEOFORGE_MAVEN = "https://maven.neoforged.net/releases"
FORGE_MAVEN = "https://maven.minecraftforge.net"
QUILT_INSTALLER = (
    "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer"
    "/0.15.1/quilt-installer-0.15.1.jar"
)
JVM_ARGS = ["-Xms256M", "-Xmx1G", "-XX:ActiveProcessorCount=2"]


def properties(path):
    return dict(
        line.split("=", 1)
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    )


def download(url, filename):
    target = SMOKE / "downloads" / filename
    if not target.is_file():
        target.parent.mkdir(parents=True, exist_ok=True)
        with tempfile.NamedTemporaryFile(dir=target.parent, delete=False) as partial:
            partial_path = Path(partial.name)
            try:
                request = Request(url, headers={"User-Agent": "Memory-Leak-smoke-test/1.0"})
                with urlopen(request, timeout=60) as response:
                    shutil.copyfileobj(response, partial)
                partial.close()
                partial_path.replace(target)
            finally:
                partial.close()
                partial_path.unlink(missing_ok=True)
    return target


def install(java, cwd, command):
    log_path = cwd / "install.log"
    with log_path.open("wb") as log:
        result = subprocess.run(
            [java, *JVM_ARGS, "-jar", *command], cwd=cwd,
            stdout=log, stderr=subprocess.STDOUT, timeout=900,
        )
    if result.returncode:
        raise RuntimeError(f"Installer exited with status {result.returncode}. See {log_path}")


def server_args(java, cwd, node, mc, loader):
    if loader in ("fabric", "quilt"):
        api = node.get(f"fabric_api_version_{mc}", node["fabric_api_version"])
        api_name = f"fabric-api-{api}.jar"
        api_jar = download(f"{FABRIC_MAVEN}/net/fabricmc/fabric-api/fabric-api/{api}/{api_name}", api_name)
        shutil.copy2(api_jar, cwd / "mods")
    if loader == "fabric":
        version = node.get(f"loader_version_{mc}", node["loader_version"])
        url = f"https://meta.fabricmc.net/v2/versions/loader/{mc}/{version}/1.1.2/server/jar"
        return ["-jar", download(url, f"fabric-server-{mc}-{version}.jar"), "nogui"]
    if loader == "quilt":
        installer = download(QUILT_INSTALLER, "quilt-installer-0.15.1.jar")
        version = node["quilt_loader_version"]
        install(java, cwd, [installer, "install", "server", mc, version, "--install-dir=.", "--download-server"])
        return ["-jar", "quilt-server-launch.jar", "nogui"]

    maven, artifact = {
        "neoforge": (NEOFORGE_MAVEN, "net/neoforged/neoforge"),
        "forge": (FORGE_MAVEN, "net/minecraftforge/forge"),
    }[loader]
    version = node.get(f"loader_version_{mc}", node["loader_version"])
    name = f"{loader}-{version}-installer.jar"
    installer = download(f"{maven}/{artifact}/{version}/{name}", name)
    install(java, cwd, [installer, "--installServer"])
    args_file = "win_args.txt" if os.name == "nt" else "unix_args.txt"
    return [f"@libraries/{artifact}/{version}/{args_file}", "nogui"]


def exercise(java, cwd, args):
    lines = queue.Queue()
    with (
        (cwd / "console.log").open("w", encoding="utf-8") as log,
        subprocess.Popen(
            [java, *JVM_ARGS, *args], cwd=cwd, stdin=subprocess.PIPE,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            text=True, encoding="utf-8", errors="replace",
        ) as server,
    ):
        def read_output():
            for line in server.stdout:
                log.write(line)
                log.flush()
                lines.put(line)
            lines.put(None)

        reader = threading.Thread(target=read_output)
        reader.start()

        def expect(message, timeout):
            deadline = time.monotonic() + timeout
            while (remaining := deadline - time.monotonic()) > 0:
                try:
                    line = lines.get(timeout=remaining)
                except queue.Empty:
                    break
                if line is None:
                    raise RuntimeError(f"Server exited while waiting for {message!r}")
                if message in line:
                    return
            raise RuntimeError(f"Timed out waiting for {message!r}")

        def send(command):
            server.stdin.write(command + "\n")
            server.stdin.flush()

        try:
            expect('For help, type "help"', 240)
            send("leak start 64 2")
            expect("Started leaking 64 MB over 2 seconds", 20)
            time.sleep(4)
            send("leak status")
            expect("No active leak. Total leaked: 64 MB", 20)
            send("stop")
            if server.wait(timeout=60):
                raise RuntimeError(f"Server exited with status {server.returncode}")
        finally:
            if server.poll() is None:
                server.kill()
                server.wait()
            reader.join()


def main():
    parser = argparse.ArgumentParser(description="Smoke-test built Memory Leak jars on dedicated servers.")
    parser.add_argument("nodes", nargs="*", help="Specific matrix nodes. Defaults to all nodes.")
    parser.add_argument("--loader", choices=("fabric", "quilt", "forge", "neoforge"),
                        help="Run only cases for this loader.")
    parser.add_argument("--java", action="append", default=[], metavar="LEVEL=PATH",
                        help="Java executable for a Java level. Defaults to JAVA_HOME_<LEVEL>/bin/java.")
    parser.add_argument("--accept-eula", action="store_true", required=True,
                        help="Accept the Minecraft EULA for these isolated test servers.")
    args = parser.parse_args()
    javas = {level: str(Path(path).resolve()) for level, path in
             (value.split("=", 1) for value in args.java)}
    matrix = {path.parent.name: path for path in sorted((ROOT / "versions").glob("*/gradle.properties"))}
    unknown = set(args.nodes) - matrix.keys()
    if unknown:
        parser.error(f"Unknown nodes: {', '.join(sorted(unknown))}")

    mod_version = properties(ROOT / "gradle.properties")["version"]
    failures = []
    passed = 0
    SMOKE.mkdir(exist_ok=True)
    for name in args.nodes or matrix:
        config = matrix[name]
        node = properties(config)
        loader = node["mod_loader"]
        loaders = [loader]
        if node.get("quilt_loader_version"):
            loaders.append("quilt")
        jar = config.parent / "build" / "libs" / f"memory-leak-{mod_version}+mc{name}.jar"
        for mc in node["minecraft_versions"].split(","):
            for runtime_loader in loaders:
                if args.loader and runtime_loader != args.loader:
                    continue
                label = f"{name} on {runtime_loader} {mc}"
                cwd = Path(tempfile.mkdtemp(prefix=f"{name}-{runtime_loader}-{mc}-", dir=SMOKE))
                print(f"RUN  {label}", flush=True)
                try:
                    level = node["java_version"]
                    java = javas.get(level) or str(Path(os.environ[f"JAVA_HOME_{level}"]) / "bin" / "java")
                    (cwd / "mods").mkdir()
                    shutil.copy2(jar, cwd / "mods")
                    (cwd / "eula.txt").write_text("eula=true\n", encoding="utf-8")
                    (cwd / "server.properties").write_text(
                        "server-ip=127.0.0.1\nserver-port=0\nlevel-type=minecraft:flat\n"
                        "generate-structures=false\nview-distance=3\nsimulation-distance=3\n",
                        encoding="utf-8",
                    )
                    exercise(java, cwd, server_args(java, cwd, node, mc, runtime_loader))
                except (OSError, RuntimeError, subprocess.SubprocessError, KeyError) as error:
                    failures.append(label)
                    print(f"FAIL {label}: {error}. Logs: {cwd}", flush=True)
                else:
                    passed += 1
                    print(f"PASS {label}. Logs: {cwd}", flush=True)
    print(f"{passed} passed, {len(failures)} failed.", flush=True)
    return int(bool(failures) or passed == 0)


if __name__ == "__main__":
    raise SystemExit(main())
