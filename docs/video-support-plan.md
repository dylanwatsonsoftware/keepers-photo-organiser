# Video support plan

Keepers treats videos as first-class review items while keeping still-photo and video
recommendation signals separate. This avoids teaching the photo model that a representative
video frame is equivalent to a photograph.

## Stage 1 — review workflow

Status: implemented.

- Discover local camera photos and videos in capture-time order.
- Request Android photo and video library access, including selected-media access.
- Preserve media type and video duration for device-picker imports.
- Show a play marker and duration on video gallery tiles.
- Filter the gallery between all media, photos, and videos while retaining the existing
  source and workflow filters.
- Play and pause local videos in fullscreen and Quick Review.
- Apply keeper, hide, skip, navigation, and album assignment state to videos exactly as for
  photos.
- Open a video in Google Photos with `video/*` so approved Add to Albums automation can act
  on the existing video rather than treating it as an image.
- Keep videos out of photo similarity stacks and still-photo scoring.
- Display technical metadata, including video duration, without presenting an invented
  recommendation score.

Cloud-picker video import is deliberately deferred. The present Google Photos Picker import
stores a private bitmap review copy, which is suitable for photos but would discard a video's
audio and frames.

## Stage 2 — video analysis, recommendations, and feedback

Status: initial bounded analysis implemented; recommendations and feedback remain planned.

The initial pass samples three frames at 15%, 50%, and 85% of each local clip, with each
sample capped at a 384-pixel longest edge. It derives focus, detail, exposure, composition,
frame-stability, black-frame, and frozen-frame signals. Results use a separate versioned local
schema, are cached, and are shown in gallery overlays and fullscreen metadata. Still photos are
completed first; videos then run one at a time on a low-priority background thread so gallery
interaction and photo recommendations are not held behind video decoding.

1. Extend the current versioned video feature schema with face persistence, camera-facing
   attention, audio level/clipping, and rotation-aware motion smoothness.
2. Keep storing only derived anonymous signals; do not sync frames, filenames, URIs, audio,
   timestamps, or perceptual identifiers.
3. Add video-specific feedback records and exports. Keeper, rejection, hide, comment, and
   within-burst choices should identify `mediaType: "video"` and a video schema version.
4. Learn photo and video preference profiles independently, then calibrate their displayed
   recommendation scores so mixed galleries remain understandable.
5. Detect near-duplicate clips and photo/video burst relationships. Default to independent
   items until confidence is high, and never hide a photo behind a video stack automatically.
6. Generate a best-moment score and recommended poster frame for each video.
7. Add tests and device benchmarks for large 4K/HDR clips, slow motion, portrait rotation,
   missing audio, partially downloaded media, and revoked URI access.

## Stage 3 — cloud videos and richer playback

Status: planned.

- Read the Google Photos Picker media type before downloading.
- Stream or cache the original video through a private provider instead of decoding a bitmap.
- Add bounded cache eviction, download progress, cancellation, offline messaging, and retry.
- Consider moving playback to AndroidX Media3 if subtitles, audio focus, scrubbing, casting,
  HDR capability handling, or more resilient streaming become requirements.

## Guardrails

- A pass or hide never deletes media from the device or Google Photos.
- Video analysis must be cancellable and constrained by battery, thermal, storage, and network
  conditions.
- Existing photo feedback exports remain backward compatible when video schemas are added.
- Video media remains visible and manually reviewable even if analysis fails.
