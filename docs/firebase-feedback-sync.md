# Firebase feedback sync setup

The app-side feedback sync is scaffolded and remains dormant until a Firebase Android
configuration is supplied. It is opt-in per installation under **Settings → Automatic
sharing**. Enabling it schedules an immediate network-constrained upload and a periodic
upload every 12 hours through WorkManager.

Only the existing privacy-safe recommendation export is uploaded: ratings, optional
free-text comments, anonymous photo-quality signals, within-stack comparisons, and hidden
photo signals. Photo URIs, filenames, image bytes, capture timestamps, and perceptual hashes
are not included. Comments are user-entered text, so they can contain personal information if
someone types it.

## One-time Firebase console setup

1. Create or choose a Firebase project.
2. Register an Android app with package name `com.keepers.photoorganiser`.
3. Download `google-services.json` and place it at `app/google-services.json`. This path is
   gitignored. Builds without the file still work, but sync reports that setup is incomplete.
4. In **Build → Authentication → Sign-in method**, enable **Anonymous** authentication.
5. Create a Cloud Firestore database. Choose the region deliberately; it cannot be changed
   later and determines where contributors' feedback is stored.
6. Install the Firebase CLI, authenticate, and deploy the checked-in write-only rules:

   ```bash
   firebase login
   firebase deploy --only firestore:rules --project YOUR_FIREBASE_PROJECT_ID
   ```

The mobile rules deny collection listing and deletion. An anonymously authenticated install
can create its own source document, read only that document for a transaction, and replace it
only with a strictly newer snapshot. Administrative credentials are required to retrieve the
combined dataset.

## Configure GitHub APK and release builds

Encode the downloaded configuration on macOS and copy the output:

```bash
base64 -i app/google-services.json | tr -d '\n'
```

Create a GitHub Actions repository secret named `GOOGLE_SERVICES_JSON_BASE64` containing that
value. Both Android workflows decode it only inside the temporary runner. Without the secret,
CI deliberately produces an APK whose cloud sync remains dormant.

## Retrieve feedback for recommendation development

An administrator or developer with Firestore read access can use Application Default
Credentials and the dependency-free export tool:

```bash
gcloud auth application-default login
node tools/feedback-export.mjs --project YOUR_FIREBASE_PROJECT_ID \
  > /tmp/keepers-feedback-snapshots.json
```

The output contains one latest snapshot per anonymous source ID. It is suitable for analysis
or for giving to Codex while improving the shared recommendation model. Newer snapshots from
the same installation replace older ones; different phones retain separate source IDs; absent
evidence means unknown rather than negative feedback.

Run the retrieval tool tests with:

```bash
node --test tools/feedback-export.test.mjs
```

## Still manual

- Creating the Firebase project and choosing its billing/region settings.
- Registering the Android app and downloading `google-services.json`.
- Enabling Anonymous Authentication.
- Deploying the Firestore rules to the chosen project.
- Adding the GitHub Actions secret.
- Reviewing retention, deletion, consent text, and privacy-policy requirements before broad
  distribution.
- Optionally enabling Firebase App Check with Play Integrity before distributing outside a
  small trusted group. Authentication and rules prevent casual public writes, while App Check
  adds stronger protection against non-app clients.
