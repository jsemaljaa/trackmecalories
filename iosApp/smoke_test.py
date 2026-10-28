"""CI smoke test: builds a debug app for the simulator, launches it in a fresh iPhone simulator and
checks it's still running after a while. Whatever fails, the step, the app's console output, the
crash report and its system log go into one annotation (readable on the run page without logs).

Run from the repo root on macOS, after `xcodegen` (see .github/workflows/ios.yml).
"""
import glob
import json
import os
import subprocess
import sys
import time

BUNDLE_ID = 'com.jsemalja.trackmecalories'
WAIT_S = 30
report = []
step = 'start'


def run(*cmd, check=True):
    out = subprocess.run(cmd, capture_output=True, text=True)
    if check and out.returncode != 0:
        raise RuntimeError(f'{" ".join(cmd[:3])}... exited {out.returncode}\n{(out.stdout + out.stderr)[-3000:]}')
    return out.stdout


def annotate(level, title, lines):
    msg = '\n'.join(lines).replace('%', '%25').replace('\r', '').replace('\n', '%0A') or '(nothing)'
    # No ':' or ',' in the title: they'd end the annotation's properties.
    print(f'::{level} title={title}::{msg[:60000]}')


udid = None
ok = False
try:
    step = 'debug build for the simulator'
    run('xcodebuild', '-project', 'iosApp/TrackMeCalories.xcodeproj', '-target', 'TrackMeCalories',
        '-configuration', 'Debug', '-sdk', 'iphonesimulator', 'ARCHS=arm64', 'ONLY_ACTIVE_ARCH=YES',
        f'SYMROOT={os.getcwd()}/iosApp/build', 'CODE_SIGNING_ALLOWED=NO', 'build')

    step = 'picking a simulator'
    devices = json.loads(run('xcrun', 'simctl', 'list', 'devices', 'available', '-j'))['devices']
    udid = next(d['udid'] for runtime, ds in sorted(devices.items(), reverse=True) if 'iOS' in runtime
                for d in ds if d['name'].startswith('iPhone'))

    step = 'booting the simulator'
    run('xcrun', 'simctl', 'boot', udid, check=False)  # fails harmlessly if already booted
    run('xcrun', 'simctl', 'bootstatus', udid, '-b')

    step = 'installing'
    run('xcrun', 'simctl', 'install', udid, 'iosApp/build/Debug-iphonesimulator/TrackMeCalories.app')

    # A just-booted simulator can still be starting SpringBoard; launching then can stall or
    # fail, so it's retried. The app's own output goes to app.log.
    step = 'launching'
    log_path = os.path.join(os.getcwd(), 'app.log')
    for attempt in range(1, 4):
        try:
            out = subprocess.run(['xcrun', 'simctl', 'launch', f'--stdout={log_path}', f'--stderr={log_path}', udid, BUNDLE_ID],
                                 capture_output=True, text=True, timeout=120)
            report.append(f'launch attempt {attempt}: exit {out.returncode} {(out.stdout + out.stderr).strip()[-500:]}')
            if out.returncode == 0:
                break
        except subprocess.TimeoutExpired:
            report.append(f'launch attempt {attempt}: timed out after 120 s')
        time.sleep(15)
    else:
        raise RuntimeError('simctl launch never succeeded')

    step = f'running for {WAIT_S} s after launch'
    time.sleep(WAIT_S)
    run('xcrun', 'simctl', 'io', udid, 'screenshot', 'smoke.png', check=False)
    ok = BUNDLE_ID in run('xcrun', 'simctl', 'spawn', udid, 'launchctl', 'list', check=False)
    if not ok:
        report.append('The app is not running.')
except Exception as e:  # noqa: BLE001 - everything goes into the report
    report.append(str(e))

report.insert(0, f'step: {step}')
if os.path.exists('app.log'):
    report += ['--- console (last 40 lines)'] + open('app.log', errors='replace').read().splitlines()[-40:]
if not ok:
    crashes = sorted(glob.glob(os.path.expanduser('~/Library/Logs/DiagnosticReports/TrackMeCalories*')), key=os.path.getmtime)
    if crashes:
        report += ['--- crash report ' + os.path.basename(crashes[-1])] + open(crashes[-1], errors='replace').read()[:15000].splitlines()
    if udid:
        log = run('xcrun', 'simctl', 'spawn', udid, 'log', 'show', '--last', '3m', '--style', 'compact',
                  '--predicate', 'process == "TrackMeCalories"', check=False)
        report += ['--- system log (last 60 lines)'] + log.splitlines()[-60:]

annotate('notice' if ok else 'error', f'Simulator - {"running after %d s" % WAIT_S if ok else "failed"}', report)
sys.exit(0 if ok else 1)
