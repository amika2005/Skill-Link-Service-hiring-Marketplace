# Firebase Setup Instructions for SkillLink

## Current Status
The Firebase migration foundation has been created, but **no data is currently appearing in Firestore** because:

1. The Firebase project needs to be configured
2. The `google-services.json` file needs to be added
3. The Firebase integration code needs to be implemented (currently just placeholders)

## Steps to See Data in Firestore

### 1. Firebase Project Setup
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing one
3. Add an Android app with package name: `com.skilllink`
4. Download `google-services.json` and place it in `app/` directory
5. Enable Authentication (Email/Password) in Firebase Console
6. Enable Firestore Database in Firebase Console

### 2. Implement Firebase Integration
The current `FirebaseAuthManager` and `FirebaseSessionManager` are placeholder classes. To see data in Firestore, you need to:

#### Update FirebaseAuthManager.java:
```java
// Replace placeholder methods with actual Firebase implementation
// Example for registerUser method:
public void registerUser(String email, String password, String role, 
                       String displayName, String phone, RegistrationCallback callback) {
    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
        .addOnSuccessListener(authResult -> {
            // Create user document in Firestore
            Map<String, Object> user = new HashMap<>();
            user.put("email", email);
            user.put("role", role);
            user.put("displayName", displayName);
            user.put("phone", phone);
            user.put("createdAt", System.currentTimeMillis());
            
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(authResult.getUser().getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    UserProfile profile = new UserProfile(
                        authResult.getUser().getUid(),
                        email, displayName, phone, role,
                        authResult.getUser().isEmailVerified(),
                        System.currentTimeMillis()
                    );
                    callback.onSuccess(profile);
                });
        })
        .addOnFailureListener(e -> {
            callback.onError(mapFirebaseAuthError(e), e.getMessage());
        });
}
```

### 3. Update Activities to Use Firebase
Replace `AuthManager` usage with `FirebaseAuthManager` in:
- `LoginActivity.java`
- `UserSignupActivity.java` 
- `WorkerSignupActivity.java`

### 4. Test the Integration
1. Run the app
2. Try to register a new user
3. Check Firebase Console → Firestore Database
4. You should see data in the `users` collection

## Firestore Database Structure

Once implemented, you'll see these collections in Firestore:

### users Collection
```
users/{userId}
├── email: "user@example.com"
├── role: "USER" or "WORKER"
├── displayName: "John Doe"
├── phone: "+1234567890"
├── emailVerified: true
├── createdAt: 1234567890
├── updatedAt: 1234567890
```

### services Collection
```
services/{serviceId}
├── workerId: "worker123"
├── serviceName: "Electrical Repair"
├── category: "Electrician"
├── price: "5000"
├── location: "Colombo"
├── createdAt: 1234567890
```

### bookings Collection
```
bookings/{bookingId}
├── userId: "user123"
├── workerId: "worker123"
├── serviceId: "service123"
├── status: "PENDING"
├── createdAt: 1234567890
```

### conversations Collection
```
conversations/{conversationId}
├── userId: "user123"
├── workerId: "worker123"
├── lastMessage: "Hello, I need help"
├── lastMessageTimestamp: 1234567890
├── userUnread: true
├── workerUnread: false
```

### messages Subcollection
```
conversations/{conversationId}/messages/{messageId}
├── content: "Hello, I need help"
├── senderType: "USER"
├── timestamp: 1234567890
```

## What's Currently Working vs. What's Not

### ✅ Currently Working:
- Firebase dependencies are added to the project
- Basic structure for Firebase managers is created
- Migration plan and documentation are complete

### ❌ Currently Not Working:
- No actual Firebase authentication (placeholders only)
- No data being written to Firestore
- No real-time functionality
- Existing app still uses local SharedPreferences

## Next Steps to See Data in Firestore

1. **Immediate**: Set up Firebase project and add `google-services.json`
2. **Code Implementation**: Replace placeholder methods with actual Firebase code
3. **Integration**: Update activities to use Firebase managers
4. **Testing**: Register users and create bookings to see data appear

## Verification Steps

Once implemented, verify data appears in Firestore by:

1. Go to Firebase Console → Firestore Database
2. Look for collections: `users`, `services`, `bookings`, `conversations`
3. Click on any collection to see documents
4. Each document should contain the fields listed above

The migration foundation is ready, but actual Firebase integration needs to be implemented to see data in the Firestore database.
