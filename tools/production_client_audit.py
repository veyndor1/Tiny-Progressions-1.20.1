"""Run the reobfuscated player jar in an isolated, installed Forge client.

Uses only installation metadata and a local QA identity, never launcher accounts.
Install Forge into migration/production-client and build productionAuditJar first.
"""
import argparse
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
INSTALL = ROOT / 'migration/production-client'
GAME = INSTALL / 'run'
CACHE = Path.home() / '.gradle/caches/forge_gradle'
VERSION = '1.20.1-forge-47.4.10'
JAVA = (Path(os.environ['JAVA_HOME']) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')
        if os.environ.get('JAVA_HOME') else Path(shutil.which('java') or 'java'))


def sha1(path):
    return hashlib.sha1(path.read_bytes()).hexdigest()


def ensure_file(info, target, candidate=None):
    expected = info['sha1']
    if target.is_file() and sha1(target) == expected:
        return
    target.parent.mkdir(parents=True, exist_ok=True)
    if candidate and candidate.is_file() and sha1(candidate) == expected:
        shutil.copyfile(candidate, target)
    else:
        with urllib.request.urlopen(info['url'], timeout=60) as response:
            data = response.read()
        if hashlib.sha1(data).hexdigest() != expected:
            raise RuntimeError(f'Hash mismatch: {target.name}')
        target.write_bytes(data)


def allowed(entry):
    rules = entry.get('rules')
    if not rules:
        return True
    result = False
    for rule in rules:
        platform = rule.get('os', {})
        matches = (platform.get('name', 'windows') == 'windows'
                   and platform.get('arch', 'amd64') in ('amd64', 'x86_64')
                   and ('version' not in platform or re.search(platform['version'], '10.0'))
                   and not any(rule.get('features', {}).values()))
        if matches:
            result = rule['action'] == 'allow'
    return result


def expand_args(entries, values):
    result = []
    for entry in entries:
        if isinstance(entry, dict):
            if not allowed(entry):
                continue
            entry = entry['value']
        for value in entry if isinstance(entry, list) else [entry]:
            for key, replacement in values.items():
                value = value.replace('${' + key + '}', str(replacement))
            if '${' in value:
                raise RuntimeError(f'Unresolved launch argument: {value}')
            result.append(value)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server', help='Connect to the isolated dedicated QA server on loopback.')
    parser.add_argument('--expected-growth-ticks', type=int, default=37)
    args = parser.parse_args()
    if args.server and not re.fullmatch(r'(127\.0\.0\.1|localhost):[0-9]{1,5}', args.server):
        parser.error('The dedicated QA identity is restricted to a loopback server address.')
    vanilla_path = CACHE / 'minecraft_repo/versions/1.20.1/version.json'
    vanilla = json.loads(vanilla_path.read_text())
    assert vanilla['id'] == '1.20.1'
    forge = json.loads((INSTALL / f'versions/{VERSION}/{VERSION}.json').read_text())
    GAME.mkdir(parents=True, exist_ok=True)
    options = GAME / 'options.txt'
    if not options.exists():
        options.write_text('onboardAccessibility:false\n', encoding='utf-8')
    (INSTALL / 'migration').mkdir(exist_ok=True)
    natives = INSTALL / 'natives'
    natives.mkdir(exist_ok=True)
    libraries = INSTALL / 'libraries'
    selected = {}
    # A Forge library takes precedence over the same vanilla coordinate.
    for library in vanilla['libraries'] + forge['libraries']:
        if allowed(library):
            coordinate = library['name'].split(':')
            selected[tuple(coordinate[:2] + coordinate[3:])] = library
    jobs = []
    classpath = []
    for library in selected.values():
        info = library['downloads']['artifact']
        target = libraries / info['path']
        jobs.append((info, target, CACHE / 'maven_downloader' / info['path']))
        classpath.append(str(target))
    client_jar = INSTALL / 'versions/1.20.1/1.20.1.jar'
    jobs.append((vanilla['downloads']['client'], client_jar, CACHE / 'minecraft_repo/versions/1.20.1/client.jar'))
    classpath.append(str(client_jar))
    assets = CACHE / 'assets'
    asset_info = vanilla['assetIndex']
    index = assets / 'indexes' / (asset_info['id'] + '.json')
    # Use the existing developer asset cache; every selected asset is hash checked.
    ensure_file(asset_info, index)
    objects = json.loads(index.read_text())['objects']
    for obj in {obj['hash']: obj for obj in objects.values()}.values():
        digest = obj['hash']
        jobs.append(({'sha1': digest, 'url': f'https://resources.download.minecraft.net/{digest[:2]}/{digest}'},
                     assets / 'objects' / digest[:2] / digest, None))
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        list(pool.map(lambda job: ensure_file(*job), jobs))
    print(f'Validated {len(selected)} libraries and {len(objects)} assets.', flush=True)
    mods = GAME / 'mods'
    mods.mkdir(exist_ok=True)
    player = ROOT / 'build/libs/tp-4.0.0-alpha.2.jar'
    driver = ROOT / 'build/libs/tp-4.0.0-alpha.2-audit.jar'
    for source in (player, driver):
        shutil.copyfile(source, mods / source.name)
    values = {
        'natives_directory': natives, 'library_directory': libraries,
        'classpath_separator': os.pathsep, 'classpath': os.pathsep.join(classpath),
        'launcher_name': 'TinyProgressionsQA', 'launcher_version': '1',
        'auth_player_name': 'PortQA', 'auth_uuid': '00000000000000000000000000000001',
        'auth_access_token': '0', 'clientid': '0', 'auth_xuid': '0', 'user_type': 'mojang',
        'version_name': VERSION, 'version_type': 'release', 'game_directory': GAME,
        'assets_root': assets, 'assets_index_name': asset_info['id'],
    }
    if args.server:
        command = [str(JAVA), '-Xmx3G', '-Dtp.auditNetwork=true', '-Dtp.auditServer=' + args.server,
                   '-Dtp.auditExpectedGrowthTicks=' + str(args.expected_growth_ticks)]
    else:
        command = [str(JAVA), '-Xmx3G', '-Dtp.auditModels=true', '-Dtp.auditGameplay=true']
    command += expand_args(vanilla['arguments']['jvm'] + forge['arguments']['jvm'], values)
    # Inherited profiles use the vanilla jar basename in the launcher's ignore list.
    # Forge loads its patched Minecraft module from installed Maven coordinates.
    command = [arg + ',' + client_jar.name if arg.startswith('-DignoreList=') else arg
               for arg in command]
    command += [forge['mainClass']]
    command += expand_args(vanilla['arguments']['game'] + forge['arguments']['game'], values)
    mode = 'network' if args.server else 'client'
    (INSTALL / ('launch-network-manifest.json' if args.server else 'launch-manifest.json')).write_text(json.dumps({
        'minecraft': vanilla['id'], 'forge': VERSION, 'java': str(JAVA),
        'player_jar_sha256': hashlib.sha256(player.read_bytes()).hexdigest(),
        'audit_jar_sha256': hashlib.sha256(driver.read_bytes()).hexdigest(),
        'game_directory': str(GAME), 'main_class': forge['mainClass'],
        'launch_target': 'forgeclient', 'libraries_verified': len(selected),
        'assets_verified': len(objects), 'classpath': classpath,
    }, indent=2) + '\n')
    log = ROOT / f'production-{mode}-audit.log'
    started = time.time()
    with log.open('w', encoding='utf-8') as output:
        result = subprocess.run(command, cwd=GAME, stdout=output, stderr=subprocess.STDOUT, timeout=240)
    errors = []
    if result.returncode:
        errors.append(f'Client exit code: {result.returncode}')
    text = log.read_text(encoding='utf-8', errors='replace')
    if re.search(r'\[(?:ERROR|FATAL)\]|/(?:ERROR|FATAL)\]', text):
        errors.append('Runtime ERROR/FATAL log entries')
    if args.server:
        network_path = INSTALL / 'migration/client-network-audit.json'
        network = {}
        if not network_path.is_file() or network_path.stat().st_mtime < started:
            errors.append('Fresh dedicated connection report missing')
        else:
            network = json.loads(network_path.read_text())
            errors.extend(network.get('errors', []))
            if not network.get('dedicated_connection') or network.get('registered_items') != 352:
                errors.append('Dedicated connection or full registry missing')
        screenshot = INSTALL / 'migration/client-qa/dedicated-world.png'
        if not screenshot.is_file() or screenshot.stat().st_mtime < started:
            errors.append('Fresh dedicated world screenshot missing')
        summary = {'exit_code': result.returncode, 'player_jar_sha256': hashlib.sha256(player.read_bytes()).hexdigest(),
                   'network': network, 'errors': errors}
        (ROOT / 'migration/production-network-audit.json').write_text(json.dumps(summary, indent=2) + '\n')
        print(json.dumps(summary, indent=2), flush=True)
        raise SystemExit(bool(errors))
    report_path = INSTALL / 'migration/client-model-audit.json'
    if not report_path.is_file() or report_path.stat().st_mtime < started:
        errors.append('Fresh model report missing')
        models = {}
    else:
        models = json.loads(report_path.read_text())
        if models.get('block_states_checked') != 401 or models.get('items_checked') != 352:
            errors.append('Model audit did not cover the full registry')
        if models.get('missing_models') or models.get('missing_particle_textures'):
            errors.append('Missing client models or textures')
    images = [f'content-{page:02}.png' for page in range(1, 7)] + [
        'iron-furnace.png', 'pouch.png', 'worn-armor.png',
        'world-blocks-and-fluid.png', 'fluid-surface.png']
    for name in images:
        path = INSTALL / 'migration/client-qa' / name
        if not path.is_file() or path.stat().st_mtime < started:
            errors.append('Fresh screenshot missing: ' + name)
    if 'logged in with entity id' not in text or 'Stopping server' not in text:
        errors.append('World join or clean server shutdown missing')
    summary = {'exit_code': result.returncode, 'player_jar_sha256': hashlib.sha256(player.read_bytes()).hexdigest(),
               'models': models, 'screenshots': images, 'errors': errors}
    (ROOT / 'migration/production-client-audit.json').write_text(json.dumps(summary, indent=2) + '\n')
    print(f'Production client exit={result.returncode}; log={log}', flush=True)
    print(json.dumps(summary, indent=2), flush=True)
    raise SystemExit(bool(errors))


if __name__ == '__main__':
    main()
