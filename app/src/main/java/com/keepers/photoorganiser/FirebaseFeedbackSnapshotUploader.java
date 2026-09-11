package com.keepers.photoorganiser;

import android.content.Context;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class FirebaseFeedbackSnapshotUploader {
    private static final long TIMEOUT_SECONDS = 45;

    FirebaseApp configuredApp(Context context) {
        return FirebaseApp.initializeApp(context);
    }

    boolean upload(FirebaseApp app, RecommendationFeedbackSnapshot snapshot) throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance(app);
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            user = Tasks.await(auth.signInAnonymously(), TIMEOUT_SECONDS, TimeUnit.SECONDS).getUser();
        }
        if (user == null) throw new IllegalStateException("Anonymous Firebase sign-in failed");
        String ownerUid = user.getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance(app);
        DocumentReference document = firestore.collection("recommendationFeedbackSources")
                .document(snapshot.sourceId());
        return Tasks.await(firestore.runTransaction(transaction -> {
            Long existingSequence = transaction.get(document).getLong("snapshotSequence");
            if (!FeedbackSyncDocument.shouldReplace(
                    existingSequence, snapshot.snapshotSequence())) return false;
            Map<String, Object> fields = new HashMap<>(FeedbackSyncDocument.fields(
                    snapshot, ownerUid, System.currentTimeMillis()));
            fields.put("serverUpdatedAt", FieldValue.serverTimestamp());
            transaction.set(document, fields);
            return true;
        }), TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
