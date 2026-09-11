import assert from 'node:assert/strict';
import test from 'node:test';
import { decodeSnapshotDocument, mergeSnapshotDocuments } from './feedback-export.mjs';

function document(sourceId, sequence, payload) {
  return {
    name: `projects/test/databases/(default)/documents/recommendationFeedbackSources/${sourceId}`,
    fields: {
      sourceId: { stringValue: sourceId },
      snapshotSequence: { integerValue: String(sequence) },
      payloadJson: { stringValue: JSON.stringify(payload) },
    },
  };
}

test('decodes a Firestore feedback snapshot', () => {
  assert.deepEqual(decodeSnapshotDocument(document('phone-a', 4, { feedback: [1] })), {
    sourceId: 'phone-a',
    snapshotSequence: 4,
    payload: { feedback: [1] },
  });
});

test('keeps only the newest snapshot from each source', () => {
  const merged = mergeSnapshotDocuments([
    document('phone-a', 2, { marker: 'old' }),
    document('phone-b', 1, { marker: 'other' }),
    document('phone-a', 3, { marker: 'new' }),
  ]);

  assert.equal(merged.length, 2);
  assert.equal(merged.find(item => item.sourceId === 'phone-a').payload.marker, 'new');
});
