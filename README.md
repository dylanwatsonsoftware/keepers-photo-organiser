# Keepers Photo Organiser

Keepers is an Android proof of concept for one critical question: can an app help select photos and then reliably apply those selections to the **existing** Google Photos items without creating duplicate uploads?

This build deliberately contains no AI ranking yet. The Google Photos handoff must pass on a real Pixel before the larger product is worth building.

> [!WARNING]
> The **Batch album experiment** uses Android's multi-file sharing mechanism. Google Photos may interpret it as an upload. Use expendable test photos and check for duplicate cloud items afterwards.

## What the proof tests

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
- The current build tests the final official route: direct access to native local camera MediaStore items. Its Google Photos behaviour still needs to be tested.

## Pass criteria

Do not proceed to photo ranking unless the on-device test establishes that:

- A selected, already-backed-up item opens as the existing Google Photos item.
- Its Google Photos Favourite state can be changed with an acceptable number of taps.
- Several selected originals can be added to an existing Google Photos album.
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

## Current limitations

- Google Photos provides no public API for changing the Favourite state or album membership of arbitrary existing personal-library items.
- The Android favourite request changes `MediaStore` state; whether Google Photos synchronises it is intentionally an empirical test.
- The system document picker may return a URI that `MediaStore.createFavoriteRequest` cannot modify. The app reports that as a failed integration result.
- The batch share surface may upload copies rather than target existing cloud items.
- Google Photos UI changes may alter the observed behaviour at any time.

No photos leave the device through Keepers itself. External behaviour occurs only when you deliberately launch Google Photos from one of the experiment buttons.
