# Firebase Database Setup - CRITICAL FIX

## Issue Identified

The error message is clear:
```
The database (default) does not exist for project skilllink-e376a
Please visit https://console.cloud.google.com/datastore/setup?project=skilllink-e376a to add a Cloud Datastore or Cloud Firestore database.
```

## Immediate Action Required

### Step 1: Create Firestore Database

1. **Go to Firebase Console**: https://console.firebase.google.com/
2. **Select your project**: `skilllink-e376a`
3. **Go to Firestore Database** (left sidebar)
4. **Click "Create database"**
5. **Choose "Start in test mode"** (for now)
6. **Select a location** (choose closest to your users, e.g., `asia-southeast1`)
7. **Click "Enable"**

### Step 2: Enable Authentication

1. **Go to Authentication** (left sidebar)
2. **Click "Get started"**
3. **Enable "Email/Password"** sign-in method
4. **Save settings**

### Step 3: Deploy Security Rules

1. **In Firestore Database**, go to "Rules" tab
2. **Replace existing rules** with:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can read and write their own profile
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Authenticated users can read service data
    match /worker_services/{serviceId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
        request.auth.uid == resource.data.workerId;
    }
    
    // Authenticated users can manage their bookings
    match /user_bookings/{bookingId} {
      allow read, write: if request.auth != null && 
        (request.auth.uid == resource.data.userId || 
         request.auth.uid == resource.data.workerId);
    }
    
    // Authenticated users can manage job requests
    match /worker_job_requests/{requestId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.workerId;
    }
    
    // Authenticated users can participate in conversations
    match /user_conversations/{conversationId} {
      allow read, write: if request.auth != null && 
        (request.auth.uid == resource.data.participants.userId || 
         request.auth.uid == resource.data.participants.workerId);
    }
    
    // Authenticated users can read/write messages in their conversations
    match /user_chat_messages/{messageId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.senderId;
    }
  }
}
```

3. **Click "Publish"**

## Why Registration is Hanging

The registration process is:
1. ✅ Create user in Firebase Auth (working)
2. ❌ Save user profile to Firestore (failing - no database exists)
3. ❌ Complete registration (stuck at step 2)

## Temporary Fix (Optional)

If you want to test registration immediately while setting up the database, I can modify the code to skip Firestore saving temporarily:

```java
// In FirebaseAuthManager.java - temporary bypass
private void saveUserProfileToFirestore(UserProfile profile, OnCompleteListener<Void> callback) {
    // Temporary bypass until database is created
    Log.d(TAG, "Firestore database not available - skipping profile save");
    callback.onComplete(Tasks.forResult(null)); // Pretend it succeeded
}
```

## Testing After Setup

1. **Complete the database setup above**
2. **Build and install the app**
3. **Test registration** - should complete successfully
4. **Verify user data in Firestore Console**

## Expected Behavior After Fix

- Registration completes in 3-5 seconds
- User data appears in Firestore Database
- Login works with saved user profiles
- No more "database does not exist" errors

## Verification

Check these logs after setup:
```
D/FirebaseAuthManager: User profile saved successfully to Firestore
D/FirebaseAuthManager: Registration successful
```

The registration hanging issue will be completely resolved once you create the Firestore database in your Firebase project.
