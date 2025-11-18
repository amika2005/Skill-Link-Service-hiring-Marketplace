# Firebase Database Setup Guide

## Prerequisites

- Firebase project created
- Android Studio project set up

## Step 1: Create Firestore Database

1. **Go to Firebase Console**: https://console.firebase.google.com/
2. **Select your project**: `your-project-id`
3. **Go to Firestore Database** (left sidebar)
4. **Click "Create database"**
5. **Choose "Start in test mode"** (for development)
6. **Select a location** (choose closest to your users)
7. **Click "Enable"**

## Step 2: Enable Authentication

1. **Go to Authentication** (left sidebar)
2. **Click "Get started"**
3. **Enable "Email/Password"** sign-in method
4. **Save settings**

## Step 3: Deploy Security Rules

1. **In Firestore Database**, go to "Rules" tab
2. **Replace existing rules** with appropriate security rules for your app
3. **Click "Publish"**

## Step 4: Configure Android App

1. **Download google-services.json** from Firebase Console
2. **Place it in your app/ directory**
3. **Add Firebase dependencies** to build.gradle

## Testing

1. **Build and install app**
2. **Test registration and login**
3. **Verify data appears in Firestore Console**

## Security Notes

- Never commit google-services.json to version control
- Use different projects for development and production
- Implement proper security rules before production deployment
