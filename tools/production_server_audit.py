"""Check the packaged mod with a real loopback dedicated server and client.

Requires the user's existing EULA acceptance. This script never accepts the EULA.
The disposable world, logs and audit reports stay under the migration workspace.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / 'migration/production-server'
JAVA = (Path(os.environ['JAVA_HOME']) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')
        if os.environ.get('JAVA_HOME') else Path(shutil.which('java') or 'java'))
ARGUMENTS = 'libraries/net/minecraftforge/forge/1.20.1-47.4.10/win_args.txt'
PLAYER = ROOT / 'build/libs/tp-4.0.0-alpha.2.jar'


def accepted_eula():
    path = SERVER / 'eula.txt'
    return path.is_file() and bool(re.search(r'^\s*eula\s*=\s*true\s*$', path.read_text(), re.M | re.I))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='Read-only prerequisite and approval check.')
    parser.add_argument('--port', type=int, default=25575)
    args = parser.parse_args()
    if not 1024 <= args.port <= 65535:
        parser.error('QA port must be between 1024 and 65535.')
    ready = JAVA.is_file() and (SERVER / ARGUMENTS).is_file() and PLAYER.is_file()
    accepted = accepted_eula()
    preflight = {'installation_ready': ready, 'eula_accepted': accepted,
                 'player_jar_sha256': hashlib.sha256(PLAYER.read_bytes()).hexdigest() if PLAYER.is_file() else None,
                 'address': f'127.0.0.1:{args.port}',
                 'gate': None if accepted else 'User Minecraft EULA acceptance is required.'}
    print(json.dumps(preflight, indent=2), flush=True)
    if args.check:
        return 0 if ready else 1
    # No file mutations, sockets, server launch or client launch precede acceptance.
    if not accepted:
        return 2
    if not ready:
        return 1
    with socket.socket() as probe:
        probe.bind(('127.0.0.1', args.port))
    settings = {
        'server-ip': '127.0.0.1', 'server-port': str(args.port),
        'online-mode': 'false', 'enforce-secure-profile': 'false',
        'level-name': 'tp-network-audit-' + str(int(time.time() * 1000)),
        'level-type': 'minecraft:flat', 'generate-structures': 'false',
        'generator-settings': json.dumps({
            'layers': [{'height': 1, 'block': 'minecraft:bedrock'},
                       {'height': 2, 'block': 'minecraft:dirt'},
                       {'height': 1, 'block': 'minecraft:grass_block'}],
            'biome': 'minecraft:plains', 'lakes': False, 'features': False,
            'structure_overrides': [],
        }, separators=(',', ':')),
        'gamemode': 'creative', 'difficulty': 'peaceful', 'spawn-protection': '0',
        'view-distance': '3', 'simulation-distance': '3', 'max-players': '2',
    }
    # Authentication is disabled only in this isolated, loopback-bound QA installation.
    properties = SERVER / 'server.properties'
    template = SERVER / 'defaultconfigs/tinyprogressions-server.toml'
    backups = {path: path.read_bytes() if path.exists() else None for path in (properties, template)}
    errors = []
    process = None
    clean_shutdown = False
    log = ROOT / 'production-server-audit.log'
    try:
        properties.write_text(''.join(f'{key}={value}\n' for key, value in settings.items()), encoding='utf-8')
        template.parent.mkdir(parents=True, exist_ok=True)
        template.write_text('[ConfigHandler]\nBlockGrowthTicks = 37\n', encoding='utf-8')
        mods = SERVER / 'mods'
        mods.mkdir(exist_ok=True)
        shutil.copyfile(PLAYER, mods / PLAYER.name)
        with log.open('w', encoding='utf-8') as output:
            process = subprocess.Popen([str(JAVA), '-Xmx3G', '@user_jvm_args.txt', '@' + ARGUMENTS, 'nogui'],
                                       cwd=SERVER, stdin=subprocess.PIPE, stdout=output,
                                       stderr=subprocess.STDOUT, text=True, encoding='utf-8')
            print(f'Dedicated QA process pid={process.pid}', flush=True)
            deadline = time.monotonic() + 180
            while 'Done (' not in log.read_text(encoding='utf-8', errors='replace'):
                if process.poll() is not None:
                    raise RuntimeError(f'Dedicated server exited before readiness: {process.returncode}')
                if time.monotonic() >= deadline:
                    raise RuntimeError('Dedicated server readiness timed out')
                time.sleep(1)
            print('Dedicated world ready; starting the QA client.', flush=True)
            client = subprocess.run([sys.executable, str(ROOT / 'tools/production_client_audit.py'),
                                     '--server', f'127.0.0.1:{args.port}', '--expected-growth-ticks', '37'],
                                    cwd=ROOT, timeout=330)
            if client.returncode:
                errors.append(f'Dedicated client audit failed: {client.returncode}')
            if process.poll() is None:
                process.stdin.write('stop\n')
                process.stdin.flush()
            process.wait(timeout=60)
            text = log.read_text(encoding='utf-8', errors='replace')
            clean_shutdown = process.returncode == 0 and 'All dimensions are saved' in text
            if not clean_shutdown:
                errors.append('Dedicated server did not save and stop cleanly')
            if 'logged in with entity id' not in text:
                errors.append('Server did not observe the QA client login')
            if re.search(r'\[(?:ERROR|FATAL)\]|/(?:ERROR|FATAL)\]', text):
                errors.append('Dedicated server ERROR/FATAL entries')
    except Exception as error:
        errors.append(str(error))
    finally:
        if process and process.poll() is None:
            try:
                process.stdin.write('stop\n')
                process.stdin.flush()
                process.wait(timeout=60)
            except (BrokenPipeError, OSError, subprocess.TimeoutExpired):
                process.kill()
                process.wait(timeout=20)
        # Restore settings after the owned server process has stopped.
        for path, content in backups.items():
            if content is None:
                path.unlink(missing_ok=True)
            else:
                path.write_bytes(content)
    report = {'player_jar_sha256': preflight['player_jar_sha256'], 'eula_accepted': accepted,
              'server_exit_code': process.returncode if process else None,
              'clean_shutdown': clean_shutdown, 'world': settings['level-name'], 'errors': errors}
    (ROOT / 'migration/production-server-audit.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2), flush=True)
    return bool(errors)


if __name__ == '__main__':
    raise SystemExit(main())
