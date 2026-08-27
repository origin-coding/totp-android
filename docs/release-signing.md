# Release signing

Android requires every APK to be signed. The first public release establishes the signing identity that must also sign every future update with the same application ID. Losing the release key prevents existing installations from being upgraded, so create and back up the keystore before publishing any APK.

## 1. Generate the release keystore

Use the `keytool` included with JDK 17. Run this command in PowerShell and enter the passwords and certificate identity interactively so they are not saved in shell history:

```powershell
New-Item -ItemType Directory -Path "$env:USERPROFILE\.android-keystores" -Force

keytool -genkeypair -v `
    -keystore "$env:USERPROFILE\.android-keystores\totp-release.jks" `
    -storetype JKS `
    -alias totp-release `
    -keyalg RSA `
    -keysize 4096 `
    -sigalg SHA256withRSA `
    -validity 10000
```

If `keytool` is not on `PATH`, run the copy from the JDK selected by Android Studio. The JDK path is shown under **Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK**.

Suggested certificate identity:

- First and last name / CN: the maintainer's legal name or the consistently used `origin-coding` identity
- Organizational unit / OU: optional
- Organization / O: `OriginCoding` or the maintainer's name
- City, state, and country: the maintainer's actual details, or leave optional fields empty where `keytool` permits it

The certificate identity is not a login account. The keystore password and private-key password are the secrets that protect the signing key. Store both in a password manager.

## 2. Verify and back up the keystore

Inspect the generated entry:

```powershell
keytool -list -v `
    -keystore "$env:USERPROFILE\.android-keystores\totp-release.jks" `
    -alias totp-release
```

Confirm that the alias is `totp-release`, the entry type is `PrivateKeyEntry`, and the certificate has a SHA-256 fingerprint. Keep a record of that fingerprint for comparison with signed APKs.

The official release certificate for this project has the following SHA-256 fingerprint:

```text
0F:FF:9B:18:58:01:C7:A0:52:77:BE:01:9D:22:BF:44:
BC:3F:C3:35:A4:5C:E7:0F:04:74:C9:A8:AB:23:9E:27
```

The release workflow rejects an APK whose signer does not match this fingerprint.

Create at least two encrypted backups in separate locations. Back up all of the following together:

- `totp-release.jks`
- the keystore password
- the alias (`totp-release`)
- the private-key password
- the certificate SHA-256 fingerprint

Never commit the keystore, passwords, `signing.properties`, or a Base64 copy of the keystore. The repository already ignores `*.jks` and `signing.properties`.

## 3. Test release signing locally

Copy the example configuration:

```powershell
Copy-Item signing.properties.example signing.properties
```

Edit the ignored `signing.properties` file with the real values. Use forward slashes in the Windows path:

```properties
storeFile=C:/Users/your-name/.android-keystores/totp-release.jks
storePassword=your-keystore-password
keyAlias=totp-release
keyPassword=your-private-key-password
```

Build the signed APK:

```powershell
.\gradlew.bat assembleRelease
```

The output is written under `app/build/outputs/apk/release/`. Verify it with the `apksigner` from the installed Android SDK Build Tools:

```powershell
& "$env:ANDROID_HOME\build-tools\36.0.0\apksigner.bat" verify `
    --verbose `
    --print-certs `
    app\build\outputs\apk\release\app-release.apk
```

If `ANDROID_HOME` is not set, use the SDK path displayed by Android Studio's SDK Manager. Compare the signer SHA-256 digest with the fingerprint recorded from `keytool`.

## 4. Configure GitHub Actions secrets

Open the GitHub repository and go to **Settings > Secrets and variables > Actions**. Create these repository secrets:

| Secret | Value |
| --- | --- |
| `ANDROID_RELEASE_KEYSTORE_BASE64` | Base64 representation of the entire keystore file |
| `ANDROID_RELEASE_STORE_PASSWORD` | Keystore password |
| `ANDROID_RELEASE_KEY_ALIAS` | `totp-release` |
| `ANDROID_RELEASE_KEY_PASSWORD` | Private-key password |

Generate the Base64 value without writing another copy to disk:

```powershell
$keystorePath = "$env:USERPROFILE\.android-keystores\totp-release.jks"
$keystoreBytes = [System.IO.File]::ReadAllBytes($keystorePath)
[Convert]::ToBase64String($keystoreBytes) | Set-Clipboard
Remove-Variable keystoreBytes
```

Paste the clipboard value into `ANDROID_RELEASE_KEYSTORE_BASE64`, save the secret, and then clear the clipboard:

```powershell
Set-Clipboard -Value ""
```

GitHub Actions secrets cannot be read back after creation. Their names remain visible, but their values do not.

## 5. Release workflow behavior

`.github/workflows/release.yml` runs when a `v*` tag is pushed or when it is manually dispatched for an existing tag. It:

1. checks the release tag format;
2. restores the keystore from GitHub Actions secrets;
3. runs `test`, `lint`, `assembleDebug`, and `assembleRelease`;
4. verifies the APK signature with `apksigner`;
5. generates a SHA-256 checksum;
6. uploads the signed files as workflow artifacts; and
7. creates or updates the corresponding GitHub Release.

A tag containing a suffix, such as `v1.0.0-rc.1`, is published as a pre-release. A tag such as `v1.0.0` is published as the latest stable release.

Do not create the first public tag until the locally signed APK has been installed and the signing fingerprint has been verified.

## References

- [Sign your app](https://developer.android.com/studio/publish/app-signing)
- [Prepare your app for release](https://developer.android.com/studio/publish/preparing)
- [Using secrets in GitHub Actions](https://docs.github.com/actions/security-guides/using-secrets-in-github-actions)
