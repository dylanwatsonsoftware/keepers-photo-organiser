# Keepers Photo Organiser

Keepers is an Android prototype for reviewing recent Pixel camera photos, choosing the best images without hiding the rest, and applying approved choices to the **existing** Google Photos items without creating duplicate uploads.

> [!IMPORTANT]
> **Feasibility result: passed through explicit Accessibility automation.** Testing on a Pixel 7 confirmed that Keepers can one-shot Favourite an open existing Google Photos item and add it to an exact existing album. Google provides no supported public API for these operations, so the integration depends on visible Google Photos controls and may break when its UI changes.

> [!WARNING]
> The **Batch album experiment** uses Android's multi-file sharing mechanism. Google Photos may interpret it as an upload. Use expendable test photos and check for duplicate cloud items afterwards.

## Current review prototype

Tap **Review recent photos** to load the first 60 recent images that still exist locally in `DCIM/Camera`. Use **Load 60 more** repeatedly to work backward through as many locally available images as needed without loading full-resolution files into memory.

- Photos appear in a compact three-column grid inspired by Google Photos.
- Tap an image to mark it as a keeper; tap again to undo.
- Once a keeper is chosen, other images fade but always remain visible and selectable.
- Keepers clusters visually similar photos across the loaded batch, keeps the strongest technical frame from each near-duplicate cluster, and recommends visually distinct shots rather than excluding them because they were taken close together.
- AI suggestions are not confirmations: only photos you tap receive a blue keeper heart.
- Tap a thumbnail to inspect it in a large preview, see whether it is recommended, swipe left or right through adjacent photos, swipe down to return, and use the explicit heart control to confirm or remove a keeper.
- Keeper choices persist locally across app restarts and can be cleared at any time.
- **Google Photos** imports one explicitly selected Picker item into a private, read-only
  Keepers review cache. Cloud items participate in the same on-device ranking, keeper selection,
  face analysis, and album-suggestion screens, including when local-library permission is denied.
- Cloud-only album suggestions are deliberately excluded from the Google Photos automation queue:
  handing Keepers' private review copy back to Photos could create a duplicate upload.
- No image is moved, hidden, deleted, uploaded, or modified by the review screen.

The current ranking is entirely on-device and technical: capture time, perceptual similarity, and image detail. It does not yet identify people, assess expressions, learn personal taste, or navigate Google Photos to the corresponding photo automatically. Those are subsequent milestones built on the now-proven one-shot actions.

## Earlier integration experiments

The app lets you select 3–5 photos and run three experiments:

1. **Open first existing photo** — sends one selected content URI directly to Google Photos using `ACTION_VIEW`.
2. **Request Android favourite** — asks Android's `MediaStore` to mark the selected local items as favourites, behind the system confirmation dialog. The test is whether Google Photos reflects and syncs that state.
3. **Batch album experiment** — sends several selected URIs to Google Photos using `ACTION_SEND_MULTIPLE`. This is explicitly treated as a duplicate-risk experiment.

The current build requests local photo access and directly queries the five newest items in `DCIM/Camera`. This produces item-specific MediaStore URIs instead of the Photo Picker's restricted read-only URIs.

## Observations so far

Tested on a Pixel 7 running Android 17:

- Opening a previously selected document URI in Google Photos displayed the correct photo, but in a restricted viewer without the normal Favourite control.
- `MediaStore.createFavoriteRequest` rejected document-provider URIs, as Android requires item-specific MediaStore URIs.
- Android Photo Picker URIs were also read-only: Google Photos showed a black viewer and the Favourite request failed.
- Multi-file sharing opened Google Photos' upload screen, so it is not a safe existing-album handoff.
- Direct access to native local camera MediaStore items also failed: Google Photos did not provide a useful existing-item handoff, and Android's Favourite state did not produce the required Google Photos result.

## Decision

The official integration gate is closed as a **no-go**. Remaining options require changing the product constraint:

- Automate the Google Photos UI with an Android Accessibility service, accepting powerful permissions and ongoing fragility.
- Keep favourites and child collections inside Keepers instead of Google Photos.
- Use a photo library with an API that supports modifying existing albums and favourites.

## Accessibility experiment

**Result on Pixel 7: Favourite automation succeeded.** After explicit arming and manual navigation to a Google Photos item, Keepers located and activated the real Favourite control.

The current build includes an explicitly armed, one-shot Accessibility test:

1. Tap **Enable Keepers Accessibility service** and enable only the Keepers service.
2. Return to Keepers and tap **Arm one-shot Favourite**.
3. Keepers opens Google Photos. Manually open one expendable photo within two minutes.
4. Keepers clicks only an exact `Favourite`, `Favorite`, `Add to favourites`, or `Add to favorites` accessibility label, disarming before the click.

It ignores `Unfavourite`/`Remove from favorites`, listens only to Google Photos, expires after two minutes, and never runs continuously unless explicitly armed again.

The build also includes a one-shot album action. Enter the exact existing album name, arm **Add to album**, and manually open one test photo. Keepers follows the observed Google Photos flow—exact `Add to`, exact `Album`, then the exact configured album name—and expires after two minutes.

## Pass criteria

The Pixel 7 test established that:

- A selected, already-backed-up item opens as the existing Google Photos item.
- Its Google Photos Favourite state can be changed through an explicitly armed one-shot action.
- An existing item can be added to an exact existing Google Photos album through an explicitly armed one-shot action.
- No duplicate cloud items are created.
- The cloud state remains correct after local copies are removed.

Check the results at [photos.google.com](https://photos.google.com/) or on a second device rather than trusting only the Pixel's local display.

## Install on a Pixel

Enable **Developer options** and **USB debugging** on the Pixel, connect it by USB, and accept the computer's debugging prompt. Then run:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The packaged debug APK is at [`app/build/outputs/apk/debug/app-debug.apk`](app/build/outputs/apk/debug/app-debug.apk).

## Build and test

Requirements:

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools 35

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The tests cover the safety classification, intent contracts, initial UI state and duplicate-risk labelling.

## Optional Firebase feedback sync

Recommendation feedback can be shared automatically when a network is available after the
person using the app explicitly enables it in Settings. The implementation sends only the
privacy-safe feedback export—not photos or local photo identifiers—and keeps each phone's
versioned snapshot independent. See [Firebase feedback sync setup](docs/firebase-feedback-sync.md)
for the remaining Firebase console steps, CI secret setup, Firestore rules, and the administrative
export command used to retrieve feedback for recommendation development.

## Current limitations

- Google Photos provides no public API for changing the Favourite state or album membership of arbitrary existing personal-library items.
- The Android favourite request changes `MediaStore` state; whether Google Photos synchronises it is intentionally an empirical test.
- The system document picker may return a URI that `MediaStore.createFavoriteRequest` cannot modify. The app reports that as a failed integration result.
- The batch share surface may upload copies rather than target existing cloud items.
- Google Photos UI changes may alter the observed behaviour at any time.

No photos leave the device through Keepers itself. If automatic feedback sharing is explicitly
enabled and Firebase has been configured, anonymous recommendation signals and optional comments
can leave the device; photo bytes and local photo identifiers do not. Google Photos integration
still occurs only after deliberate user actions.
