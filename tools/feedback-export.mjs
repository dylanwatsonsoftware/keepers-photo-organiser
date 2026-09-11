#!/usr/bin/env node

import { execFileSync } from 'node:child_process';
import { pathToFileURL } from 'node:url';

export function decodeSnapshotDocument(document) {
  const fields = document?.fields ?? {};
  const sourceId = fields.sourceId?.stringValue;
  const sequence = Number(fields.snapshotSequence?.integerValue);
  const payloadJson = fields.payloadJson?.stringValue;
  if (!sourceId || !Number.isSafeInteger(sequence) || sequence < 1 || !payloadJson) {
    throw new Error(`Invalid feedback document: ${document?.name ?? 'unknown'}`);
  }
  return { sourceId, snapshotSequence: sequence, payload: JSON.parse(payloadJson) };
}

export function mergeSnapshotDocuments(documents) {
  const latest = new Map();
  for (const document of documents) {
    const candidate = decodeSnapshotDocument(document);
    const current = latest.get(candidate.sourceId);
    if (!current || candidate.snapshotSequence > current.snapshotSequence) {
      latest.set(candidate.sourceId, candidate);
    }
  }
  return [...latest.values()].sort((left, right) =>
    left.sourceId.localeCompare(right.sourceId));
}

function accessToken() {
  if (process.env.GOOGLE_OAUTH_ACCESS_TOKEN) return process.env.GOOGLE_OAUTH_ACCESS_TOKEN;
  try {
    return execFileSync('gcloud', ['auth', 'application-default', 'print-access-token'], {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'inherit'],
    }).trim();
  } catch {
    throw new Error('Run `gcloud auth application-default login` or set '
      + 'GOOGLE_OAUTH_ACCESS_TOKEN before exporting feedback.');
  }
}

async function fetchDocuments(projectId, token) {
  const documents = [];
  let pageToken = '';
  do {
    const url = new URL(`https://firestore.googleapis.com/v1/projects/${encodeURIComponent(projectId)}`
      + '/databases/(default)/documents/recommendationFeedbackSources');
    url.searchParams.set('pageSize', '300');
    if (pageToken) url.searchParams.set('pageToken', pageToken);
    const response = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
    if (!response.ok) {
      throw new Error(`Firestore export failed (${response.status}): ${await response.text()}`);
    }
    const page = await response.json();
    documents.push(...(page.documents ?? []));
    pageToken = page.nextPageToken ?? '';
  } while (pageToken);
  return documents;
}

function projectArgument(arguments_) {
  const index = arguments_.indexOf('--project');
  const projectId = index >= 0 ? arguments_[index + 1] : '';
  if (!projectId || !/^[a-z][a-z0-9-]{4,28}[a-z0-9]$/.test(projectId)) {
    throw new Error('Usage: node tools/feedback-export.mjs --project FIREBASE_PROJECT_ID');
  }
  return projectId;
}

async function main() {
  const projectId = projectArgument(process.argv.slice(2));
  const snapshots = mergeSnapshotDocuments(await fetchDocuments(projectId, accessToken()));
  process.stdout.write(`${JSON.stringify({
    schemaVersion: 1,
    exportedAt: new Date().toISOString(),
    sourceCount: snapshots.length,
    snapshots,
  }, null, 2)}\n`);
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  main().catch(error => {
    process.stderr.write(`${error.message}\n`);
    process.exitCode = 1;
  });
}
