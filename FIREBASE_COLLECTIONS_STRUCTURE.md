# Firestore Collections Auto-Created During Registration

## Short Answer

**Yes, when you register as a new user or worker, the system automatically creates several collections and documents in Firestore!**

The system creates a main user document and sets up the infrastructure for additional collections that will be populated as the user uses the app.

## What Gets Created Immediately During Registration

### 1. Main User Document
**Collection**: `users`
**Document**: `{userId}` (the user's unique Firebase Auth ID)

**Document Structure**:
```javascript
users/{userId}/
{
  uid: "abc123xyz789",                    // Firebase Auth user ID
  email: "user@example.com",              // User's email
  displayName: "John Doe",                // User's name
  phone: "+94771234567",                  // Phone number (if provided)
  role: "USER" or "WORKER",               // User role
  emailVerified: false,                   // Email verification status
  createdAt: 1640995200000,               // Registration timestamp
  avatarUri: null,                        // Profile picture URL (initially null)
  location: null,                         // User location (initially null)
  bio: null                              // User biography (initially null)
}
```

## Collections That Are Set Up for Future Use

### 2. Payment Cards Subcollection
**Collection**: `users/{userId}/paymentCards`
- Initially empty
- Created when user adds payment methods
- Structure: `paymentCards/{cardId}`

### 3. User Preferences Subcollections
**Collection**: `users/{userId}/preferences/`
- `payment/` - Payment preferences
- `payhere/` - PayHere merchant settings
- `locations/` - Recent service areas

### 4. Real-time Listeners Setup
The system immediately sets up real-time listeners for these collections:
- `services` - Worker services (for workers)
- `bookings` - User bookings (for users)
- `jobRequests` - Job requests (for workers)
- `conversations` - Chat conversations

## Complete Firestore Database Structure

```
Firestore Database
├── users/                              # Main user profiles
│   ├── {userId}/                       # Individual user document
│   │   ├── uid: "user123"
│   │   ├── email: "user@example.com"
│   │   ├── displayName: "John Doe"
│   │   ├── phone: "+94771234567"
│   │   ├── role: "USER" or "WORKER"
│   │   ├── emailVerified: false
│   │   ├── createdAt: 1640995200000
│   │   ├── avatarUri: null
│   │   ├── location: null
│   │   └── bio: null
│   │
│   ├── paymentCards/                   # User's saved payment methods
│   │   ├── {cardId}/
│   │   │   ├── id: "card123"
│   │   │   ├── holderName: "John Doe"
│   │   │   ├── brand: "Visa"
│   │   │   ├── last4: "1234"
│   │   │   ├── expiry: "12/25"
│   │   │   └── savedAt: 1640995200000
│   │
│   └── preferences/                    # User preferences
│       ├── payment/
│       │   ├── paymentCashEnabled: true
│       │   └── updatedAt: 1640995200000
│       ├── payhere/
│       │   ├── payHereMerchantId: "merchant123"
│       │   ├── payHereMerchantSecret: "secret123"
│       │   └── updatedAt: 1640995200000
│       └── locations/
│           ├── recentAreas: ["Colombo", "Kandy"]
│           └── updatedAt: 1640995200000
│
├── services/                           # Worker services (created when workers add services)
│   ├── {serviceId}/
│   │   ├── workerId: "worker123"
│   │   ├── category: "electrician"
│   │   ├── title: "Emergency Electrical Repairs"
│   │   ├── description: "Certified electrician..."
│   │   ├── priceType: "HOURLY"
│   │   ├── price: "4800"
│   │   ├── location: "Colombo"
│   │   ├── latitude: 6.927079
│   │   ├── longitude: 79.861244
│   │   ├── radius: 18.0
│   │   ├── available: true
│   │   ├── rating: 4.9
│   │   ├── reviewCount: 182
│   │   ├── createdAt: 1640995200000
│   │   └── updatedAt: 1640995200000
│
├── bookings/                           # User bookings (created when users book services)
│   ├── {bookingId}/
│   │   ├── userId: "user123"
│   │   ├── workerId: "worker123"
│   │   ├── serviceId: "service123"
│   │   ├── status: "PENDING"
│   │   ├── scheduledDate: "2024-01-15"
│   │   ├── scheduledTime: "14:30"
│   │   ├── duration: 2
│   │   ├── totalPrice: "9600"
│   │   ├── location: "Colombo"
│   │   ├── address: "123 Main Street"
│   │   ├── notes: "Please bring extra tools"
│   │   ├── createdAt: 1640995200000
│   │   └── updatedAt: 1640995200000
│
├── jobRequests/                        # Worker job requests (created when users request services)
│   ├── {requestId}/
│   │   ├── workerId: "worker123"
│   │   ├── userId: "user123"
│   │   ├── serviceId: "service123"
│   │   ├── status: "PENDING"
│   │   ├── requestedDate: "2024-01-15"
│   │   ├── requestedTime: "14:30"
│   │   ├── location: "Colombo"
│   │   ├── address: "123 Main Street"
│   │   ├── description: "Need emergency electrical repair"
│   │   ├── budget: "10000"
│   │   ├── urgent: true
│   │   ├── createdAt: 1640995200000
│   │   └── updatedAt: 1640995200000
│
├── conversations/                       # Chat conversations (created when users message workers)
│   ├── {conversationId}/
│   │   ├── userId: "user123"
│   │   ├── workerId: "worker123"
│   │   ├── serviceId: "service123"
│   │   ├── bookingId: "booking123"
│   │   ├── lastMessage: "When can you start?"
│   │   ├── lastMessageTimestamp: 1640995200000
│   │   ├── lastMessageSender: "user"
│   │   ├── userUnread: true
│   │   ├── workerUnread: false
│   │   ├── createdAt: 1640995200000
│   │   └── updatedAt: 1640995200000
│   │
│   └── messages/                        # Subcollection for chat messages
│       ├── {messageId}/
│       │   ├── senderId: "user123"
│       │   ├── senderType: "user"
│       │   ├── content: "When can you start?"
│       │   ├── timestamp: 1640995200000
│       │   ├── read: false
│       │   └── type: "text"
│
└── recommendedWorkers/                  # Global recommended workers (pre-populated)
    ├── {workerId}/
    │   ├── workerId: "worker123"
    │   ├── displayName: "Amal Perera"
    │   ├── profession: "Master Electrician"
    │   ├── rating: 4.9
    │   ├── reviewCount: 182
    │   ├── availability: "Available today"
    │   ├── price: "LKR 4,800 / hr"
    │   ├── distance: "2.1 km away"
    │   ├── avatarUri: "https://..."
    │   ├── category: "Electrical"
    │   ├── skills: ["Panel upgrades", "Safety inspections", "Emergency repairs"]
    │   ├── location: "Colombo"
    │   ├── latitude: 6.927079
    │   ├── longitude: 79.861244
    │   └── serviceRadius: 18.0
```

## Registration Process Step-by-Step

### User Registration
1. **Firebase Auth Account Created** → User gets email/password authentication
2. **User Document Created** → `users/{userId}` document with profile data
3. **Real-time Listeners Activated** → System starts listening to user-specific collections
4. **Email Verification Sent** → Verification email sent to user
5. **Session Established** → User logged in and session created

### Worker Registration
1. **Same as User Registration** → All steps above
2. **Role Set to "WORKER"** → User role set to worker in profile
3. **Services Listener Ready** → System ready to handle worker services
4. **Job Requests Listener Ready** → System ready to handle job requests

## Auto-Creation vs On-Demand Creation

### Created Immediately During Registration:
- ✅ `users/{userId}` document with profile data
- ✅ Real-time listeners setup
- ✅ Firebase Authentication account

### Created On-Demand (When Features Used):
- 🔄 `paymentCards` subcollection (when user adds payment method)
- 🔄 `preferences` subcollections (when user sets preferences)
- 🔄 `services` documents (when worker adds services)
- 🔄 `bookings` documents (when user books service)
- 🔄 `jobRequests` documents (when user requests service)
- 🔄 `conversations` documents (when users chat)
- 🔄 `messages` subcollection (when messages are sent)

## Data Security and Rules

The Firestore rules ensure:
- Users can only read/write their own data
- Workers can only manage their own services
- Bookings and requests are properly secured
- Role-based access control

## Real-Time Synchronization

All collections support real-time updates:
- Profile changes sync immediately
- New bookings appear instantly
- Messages sync in real-time
- Service availability updates instantly

## Benefits of This Structure

1. **Scalable**: Can handle millions of users
2. **Organized**: Clear separation of data types
3. **Secure**: Role-based access control
4. **Real-time**: Instant data synchronization
5. **Flexible**: Easy to add new features
6. **Efficient**: Optimized queries with proper indexing

**Summary**: When a user or worker registers, the system immediately creates their profile document in the `users` collection and sets up the infrastructure for all other collections. Additional collections are created on-demand as users interact with different features of the app.
