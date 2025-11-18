# Skill Link App Build Instructions

## Prerequisites

1. **Java Development Kit (JDK)**: 
   - Install JDK 11 or higher (JDK 17 recommended)
   - Set JAVA_HOME environment variable to point to your JDK installation

2. **Android Studio** (recommended):
   - Download and install Android Studio from https://developer.android.com/studio
   - This will automatically install the required Android SDK and tools

## Setup Instructions

### Option 1: Using Android Studio (Recommended)

1. Open Android Studio
2. Select "Open an existing Android Studio project"
3. Navigate to the `skill_link` directory and select it
4. Wait for Gradle sync to complete
5. Build the project using `Build > Make Project`

### Option 2: Using Command Line

1. **Install Gradle**:
   - Download Gradle from https://gradle.org/releases/
   - Extract to a directory (e.g., C:\gradle)
   - Add C:\gradle\bin to your system PATH

2. **Generate Gradle Wrapper**:
   ```bash
   cd c:\Users\user\Documents\skill_link
   gradle wrapper --gradle-version 7.5
   ```

3. **Build the Project**:
   ```bash
   cd c:\Users\user\Documents\skill_link
   .\gradlew build
   ```

## Troubleshooting

### Common Issues

1. **"Unsupported class file major version" Error**:
   - This occurs when using a newer Java version than supported by Gradle
   - Solution: Use Java 11 or update Gradle to a newer version

2. **"Could not find or load main class" Error**:
   - This occurs when gradle-wrapper.jar is missing
   - Solution: Generate the wrapper files using `gradle wrapper`

3. **"JAVA_HOME is not set" Error**:
   - Set the JAVA_HOME environment variable to point to your JDK installation
   - On Windows: 
     - Open System Properties > Advanced > Environment Variables
     - Add a new system variable JAVA_HOME with value pointing to your JDK (e.g., C:\Program Files\Java\jdk-11)

## Project Structure

The project is organized as follows:
```
skill_link/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/           # Java source files
│   │   │   ├── res/            # Resources (layouts, drawables, etc.)
│   │   │   └── AndroidManifest.xml
│   │   └── test/               # Unit tests
│   └── build.gradle            # App module build configuration
├── gradle/                     # Gradle wrapper files
│   └── wrapper/
├── build.gradle                # Top-level build configuration
├── gradle.properties           # Gradle properties
├── gradlew                     # Gradle wrapper script (Unix)
├── gradlew.bat                 # Gradle wrapper script (Windows)
└── settings.gradle             # Project settings
```

## Configuration Details

### Java Compatibility
- Source Compatibility: Java 11
- Target Compatibility: Java 11

### Android Configuration
- compileSdk: 34
- minSdk: 24
- targetSdk: 34

### Dependencies
- AndroidX AppCompat: 1.6.1
- Material Design Components: 1.9.0
- Navigation Components: 2.6.0
- RecyclerView: 1.3.0
- CardView: 1.0.0
- CircleImageView: 3.1.0

## Building for Different Environments

### Debug Build
```bash
.\gradlew assembleDebug
```

### Release Build
```bash
.\gradlew assembleRelease
```

### Running Tests
```bash
.\gradlew test
```

## Additional Resources

- Android Developer Guide: https://developer.android.com/guide
- Gradle Documentation: https://docs.gradle.org/
- Material Design Guidelines: https://material.io/