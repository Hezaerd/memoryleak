#!/usr/bin/env python3
"""Boot each real server in a fresh directory and drive /leak through its console."""

import argparse
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from pathlib import Path
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import time
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[1]
RUNS = ROOT / "build" / "smoke-plugins"
FILL = "https://fill-data.papermc.io/v1/objects/"
SPONGE = "https://repo.spongepowered.org/repository/maven-public/org/spongepowered/spongevanilla/"
USER_AGENT = "MemoryLeak-Smoke/2 (https://github.com/Hezaerd/memory-leak)"


@dataclass(frozen=True)
class Server:
    platform: str
    build: str
    url: str
    java: int
    stop: str = "stop"


SERVERS = {
    "paper": Server("bukkit", "Paper 26.3 #259",
        FILL + "5efbc6ce4b8e128bfa77fa0d3e79132dfeaaab827abe0f87dd930535e5e489fd/paper-26.3-259.jar", 25),
    "paper-old": Server("bukkit", "Paper 1.13.2 #657",
        FILL + "11e828d0565ab76a0a0e180c056364a95de44958cfd6a6af3f9b1dc70b03e9cd/paper-1.13.2-657.jar", 8),
    "folia": Server("bukkit", "Folia 26.3 #26",
        FILL + "99263358903dbc44d4a90d45b5fbb7dfdce532154734489be89a3a94088063dc/folia-26.3-26.jar", 25),
    "purpur": Server("bukkit", "Purpur 26.3 #2660",
        "https://api.purpurmc.org/v2/purpur/26.3/2660/download", 25),
    "velocity-3": Server("velocity", "Velocity 3.5.1 #615",
        FILL + "b4e3164df5377346854dc6cb9e6a78022b1946ff69e89676313f5f6f1c6f0fb3/velocity-3.5.1-615.jar", 21, "shutdown"),
    "velocity": Server("velocity", "Velocity 4.3.1 #45",
        FILL + "0844bcabdcdb4f5b875906fd4c3365bc1bb76918e42bc595ed88ad195258dd75/velocity-4.3.1-45.jar", 25, "shutdown"),
    "bungee": Server("bungee", "BungeeCord #2111",
        "https://ci.md-5.net/job/BungeeCord/2111/artifact/bootstrap/target/BungeeCord.jar", 25, "end"),
    "waterfall": Server("bungee", "Waterfall #615",
        FILL + "5eda8bfd0691e5088701f87020c68964299586df0faba289a634122d282d598c/waterfall-1.21-615.jar", 21, "end"),
    "sponge": Server("sponge", "SpongeVanilla 1.21.10-17.0.0",
        SPONGE + "1.21.10-17.0.0/spongevanilla-1.21.10-17.0.0-universal.jar", 21),
}


def download(url, destination):
    with urlopen(Request(url, headers={"User-Agent": USER_AGENT}), timeout=60) as response:
        with destination.open("wb") as file:
            shutil.copyfileobj(response, file)


def configure(run, name, platform):
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        port = sock.getsockname()[1]
    if platform == "velocity":
        (run / "velocity.toml").write_text(f'bind = "127.0.0.1:{port}"\n', encoding="utf-8")
    elif platform == "bungee":
        if name == "waterfall":
            (run / "modules.yml").write_text("version: 2\nmodules: []\n", encoding="utf-8")
        (run / "config.yml").write_text(
            f"listeners:\n- host: 127.0.0.1:{port}\n  priorities:\n  - lobby\n", encoding="utf-8"
        )
    else:
        (run / "eula.txt").write_text("eula=true\n", encoding="utf-8")
        (run / "server.properties").write_text(
            f"server-ip=127.0.0.1\nserver-port={port}\n"
            "view-distance=3\nsimulation-distance=3\ngenerate-structures=false\n"
            "spawn-protection=0\n", encoding="utf-8"
        )


def wait_for(process, log, pattern, timeout):
    deadline = time.monotonic() + timeout
    while True:
        output = log.read_text(encoding="utf-8", errors="replace")
        if re.search(pattern, output, re.MULTILINE):
            return
        if process.poll() is not None:
            raise RuntimeError(f"server exited with code {process.returncode} before {pattern!r}")
        if time.monotonic() >= deadline:
            raise TimeoutError(f"timed out waiting for {pattern!r}")
        time.sleep(0.2)


def smoke(name, server, java, plugin):
    run = Path(tempfile.mkdtemp(prefix=f"{name}-", dir=RUNS))
    log = run / "console.log"
    print(f"RUN  {server.build}  {run}", flush=True)
    download(server.url, run / "server.jar")

    plugins = run / ("mods" if server.platform == "sponge" else "plugins")
    plugins.mkdir()
    shutil.copy2(plugin, plugins)
    configure(run, name, server.platform)
    args = [java, "-XX:ActiveProcessorCount=4", "-Xms256M", "-Xmx1536M", "-jar", "server.jar"]
    if server.stop == "stop":
        args.append("nogui")

    with log.open("wb") as output, subprocess.Popen(
        args, cwd=run, stdin=subprocess.PIPE, stdout=output, stderr=subprocess.STDOUT,
    ) as process:
        try:
            ready = r"Listening on" if server.platform == "bungee" else r"Done \("
            wait_for(process, log, ready, 360)
            process.stdin.write(b"leak start 64 2\n")
            process.stdin.flush()
            wait_for(process, log, r"Started leaking 64 MB over 2 seconds", 15)
            time.sleep(4)
            process.stdin.write(b"leak status\n")
            process.stdin.flush()
            wait_for(process, log, r"No active leak\. Total leaked: 64 MB\b", 15)
        finally:
            if process.poll() is None:
                try:
                    process.stdin.write((server.stop + "\n").encode())
                    process.stdin.flush()
                    process.wait(timeout=40)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()
                    raise RuntimeError(f"server did not stop after {server.stop!r}")
            process.stdin.close()
        if process.returncode:
            raise RuntimeError(f"server exited with code {process.returncode} after {server.stop!r}")

    print(f"PASS {server.build}", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("servers", nargs="*", metavar="SERVER", help=", ".join(SERVERS))
    parser.add_argument("--version", default="2.0.0", help="version of the built plugin jars")
    parser.add_argument("--java8", help="Java 8 executable (see :bukkit:printJava8)")
    parser.add_argument("--java21", help="Java 21 executable")
    parser.add_argument("--java25", help="Java 25 executable")
    parser.add_argument("--jobs", type=int, default=1, help="number of servers to boot at once")
    args = parser.parse_args()
    names = args.servers or list(SERVERS)
    java = {8: args.java8, 21: args.java21, 25: args.java25}
    if args.jobs < 1:
        parser.error("--jobs must be positive")

    runs = []
    for name in names:
        if name not in SERVERS:
            parser.error(f"unknown server {name!r}")
        server = SERVERS[name]
        executable = java[server.java]
        if executable is None:
            parser.error(f"--java{server.java} is required for {name}")
        executable = Path(executable).resolve()
        if not executable.is_file():
            parser.error(f"Java executable not found at {executable}")
        plugin = ROOT / server.platform / "build" / "libs" / f"memory-leak-{server.platform}-{args.version}.jar"
        if not plugin.is_file():
            parser.error(f"build :{server.platform}:jar first. Missing {plugin}")
        runs.append((name, server, executable, plugin))

    RUNS.mkdir(parents=True, exist_ok=True)
    failed = False
    with ThreadPoolExecutor(max_workers=args.jobs) as executor:
        futures = {executor.submit(smoke, *run): run[1].build for run in runs}
        for future, build in futures.items():
            try:
                future.result()
            except Exception as error:
                print(f"FAIL {build}: {error}", file=sys.stderr, flush=True)
                failed = True
    return int(failed)


if __name__ == "__main__":
    sys.exit(main())
