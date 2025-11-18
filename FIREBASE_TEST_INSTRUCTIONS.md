# Firebase Registration and Login Test Instructions

## Quick Test Steps

### 1. Build and Install the App
```bash
cd "c:/Users/user/Documents/skill_link"
.\gradlew.bat assembleDebug
```
Install the generated APK from `app/build/outputs/apk/debug/app-debug.apk`

### 2. Test Registration

#### Customer Registration
1. Open the app
2. Select "Customer" role
3. Enter registration details:
   - Email: `testcustomer@example.com`
   - Password: `test123456`
   - Name: `Test Customer`
   - Phone: `0771234567`
4. Accept terms and conditions
5. Click "Create customer account"

#### Worker Registration
1. Open the app
2. Select "Professional" role
3. Enter registration details:
   - Email: `testworker@example.com`
   - Password: `test123456`
   - First Name: `Test`
   - Last Name: `Worker`
   - Phone: `0777654321`
   - Skills: `Plumbing, Electrical`
   - Experience: `5 years`
   - Location: `Colombo`
4. Accept terms and conditions
5. Click "Join as professional"

### 3. Test Login

#### Customer Login
1. Select "Customer" role
2. Enter credentials:
   - Email: `testcustomer@example.com`
   - Password: `test123456`
3. Click "Sign in"
4. Should redirect to Customer Dashboard

#### Worker Login
1. Select "Professional" role
2. Enter credentials:
   - Email: `testworker@example.com`
   - Password: `test123456`
3. Click "Sign in"
4. Should redirect to Worker Dashboard

### 4. Debug Information

#### Check Android Logcat
- Filter by tag: `FirebaseAuthManager`
- Look for these messages:
  - "Starting registration for email: ..."
  - "User created successfully with UID: ..."
  - "User profile saved successfully to Firestore"
  - "Display name updated successfully"

#### Common Log Messages
- **Success**: "Registration successful" or "Login successful"
- **Network Error**: "Network error. Please check your connection"
- **Email in Use**: "Email already registered"
- **Invalid Credentials**: "Invalid email address" or "Incorrect password"
- **Role Mismatch**: "Account exists but with different role"

### 5. Verify Firestore Data

#### Check Firebase Console
1. Go to Firebase Console
2. Select your project
3. Navigate to Firestore Database
4. Check `users` collection
5. Verify user documents contain:
   - `uid`: User ID
   - `email`: User email
   - `displayName`: User name
   - `phone`: Phone number
   - `role`: "USER" or "WORKER"
   - `createdAt`: Timestamp

### 6. Troubleshooting

#### Registration Fails
1. Check internet connection
2. Verify Firebase project settings
3. Ensure Authentication is enabled in Firebase Console
4. Check Firestore rules are deployed
5. Look at Logcat for detailed error messages

#### Login Fails
1. Verify correct email/password
2. Check role selection matches registered role
3. Ensure user exists in Firestore
4. Check if email is verified (if required)
5. Look at Logcat for authentication errors

#### Build Issues
1. Clean project: `.\gradlew.bat clean`
2. Rebuild: `.\gradlew.bat assembleDebug`
3. Check google-services.json is in app/ folder
4. Verify Firebase project configuration

### 7. Expected Behavior

#### Registration Flow
1. Show loading state
2. Create Firebase Auth user
3. Save profile to Firestore
4. Update display name
5. Send verification email
6. Show success message
7. Redirect to login screen

#### Login Flow
1. Show loading state
2. Authenticate with Firebase Auth
3. Load profile from Firestore
4. Verify role matches
5. Show success message
6. Redirect to appropriate dashboard

### 8. Test Edge Cases

#### Invalid Email
- Try `invalid-email` (should show error)
- Try `test@` (should show error)

#### Weak Password
- Try `123` (should show error)
- Try `password` (should work - 6+ characters)

#### Email Already in Use
- Try registering same email twice (should show error)

#### Wrong Password
- Try incorrect password (should show error)

#### Role Mismatch
- Register as Customer, try login as Worker (should show error)

### 9. Performance Checks

#### Network Speed
- Test on slow network
- Test with no network (should show appropriate error)

#### Firebase Response Time
- Registration should complete within 5-10 seconds
- Login should complete within 3-5 seconds

### 10. Success Criteria

✅ Registration works for both Customer and Worker roles
✅ Login works with correct credentials
✅ Role validation prevents cross-role login
✅ Error messages are clear and helpful
✅ User data is correctly saved to Firestore
✅ App redirects to correct dashboard after login
✅ Network errors are handled gracefully
✅ Form validation works properly

If all tests pass, the registration and login functionality is working correctly!
