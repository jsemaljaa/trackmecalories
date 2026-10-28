"""Writes a SideStore/AltStore source (the app list SideStore checks for updates) for a release.

Usage (CI, see .github/workflows/ios.yml): make_source.py <tag> <path/to/TrackMeCalories.ipa>
Reads the version and permission texts from the built app's Info.plist. The source is attached
to the release, so https://github.com/<repo>/releases/latest/download/source.json always lists
the newest version.
"""
import datetime
import json
import os
import plistlib
import sys
import zipfile

tag, ipa = sys.argv[1], sys.argv[2]
repo = os.environ.get('GITHUB_REPOSITORY', 'jsemaljaa/trackmecalories')
sha = os.environ.get('GITHUB_SHA', 'main')

with zipfile.ZipFile(ipa) as z:
    info = plistlib.loads(z.read('Payload/TrackMeCalories.app/Info.plist'))

privacy = {k: v for k, v in info.items() if k.startswith('NS') and k.endswith('UsageDescription')}
source = {
    'name': 'TrackMe Calories',
    'identifier': 'com.jsemalja.trackmecalories.source',
    'sourceURL': f'https://github.com/{repo}/releases/latest/download/source.json',
    'apps': [{
        'name': 'TrackMe Calories',
        'bundleIdentifier': info['CFBundleIdentifier'],
        'developerName': repo.split('/')[0],
        'subtitle': 'An ad-free, offline-first calorie and macro tracker.',
        'localizedDescription': 'Log food by search, barcode or quick add; macros, weekly budget, '
                                'weight trend, fasting timer and streaks. Your data stays on the phone.',
        'iconURL': f'https://raw.githubusercontent.com/{repo}/{sha}/iosApp/TrackMeCalories/Assets.xcassets/AppIcon.appiconset/icon-1024.png',
        'tintColor': '0E6B45',
        'category': 'lifestyle',
        'versions': [{
            'version': info['CFBundleShortVersionString'],
            'buildVersion': info['CFBundleVersion'],
            'date': datetime.date.today().isoformat(),
            'downloadURL': f'https://github.com/{repo}/releases/download/{tag}/TrackMeCalories.ipa',
            'size': os.path.getsize(ipa),
            'minOSVersion': info.get('MinimumOSVersion', '16.0'),
        }],
        # The same version in the older single-version fields, which some SideStore builds read.
        'version': info['CFBundleShortVersionString'],
        'versionDate': datetime.date.today().isoformat(),
        'downloadURL': f'https://github.com/{repo}/releases/download/{tag}/TrackMeCalories.ipa',
        'size': os.path.getsize(ipa),
        'appPermissions': {
            'entitlements': ['com.apple.developer.healthkit'],
            'privacy': privacy,
        },
    }],
    'news': [],
}
print(json.dumps(source, indent=2, ensure_ascii=False))
