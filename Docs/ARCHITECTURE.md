# SyncTax - Project Architecture Guide

## Overview

SyncTax is a modern Android music player and recommendation app built with Kotlin and Jetpack Compose. This document outlines the project's folder structure, architecture patterns, and best practices for maintaining and extending the codebase.

## 📁 Project Structure

### Root Level Structure
```
SyncTax/
├── 📄 build.gradle.kts          # Root build configuration
├── 📄 settings.gradle.kts       # Project settings and modules
├── 📄 gradle.properties         # Gradle properties
├── 📄 README.md                 # Project documentation
├── 📁 gradle/                   # Gradle wrapper
├── 📁 .github/                  # GitHub Actions and templates
├── 📁 Docs/                     # Comprehensive documentation
├── 📁 assets/                   # App assets and screenshots
├── 📁 app/                      # Main Android application module
└── 📄 Various config files      # .gitignore, etc.
```

### App Module Structure (`app/`)
```
app/
├── 📄 build.gradle.kts          # App-specific build configuration
├── 📄 proguard-rules.pro        # Obfuscation rules
├── 📁 src/
│   ├── 📁 main/
│   │   ├── 📄 AndroidManifest.xml
│   │   ├── 📁 java/com/just_for_fun/synctax/
│   │   │   ├── 📄 MainActivity.kt       # App entry point
│   │   │   ├── 📄 MusicApp.kt           # Navigation & app setup
│   │   │   ├── 📄 MusicApplication.kt   # Application class
│   │   │   ├── 📁 presentation/         # ✅ Presentation layer (UI)
│   │   │   ├── 📁 domain/               # ✅ Business logic layer
│   │   │   ├── 📁 data/                 # ✅ Data layer
│   │   │   └── 📁 core/                 # ✅ Infrastructure & shared utilities
│   │   ├── 📁 python/                   # Chaquopy Python scripts
│   │   └── 📁 res/                      # Android resources
│   ├── 📁 androidTest/                  # Instrumentation tests
│   └── 📁 test/                         # Unit tests
└── 📁 release/                          # Release artifacts
```

## 🏗️ Architecture Layers

### 1. **Presentation Layer**
```
presentation/
├── 📁 screens/                  # ✅ Main app screens (Compose)
│   ├── HomeScreen.kt           # Main dashboard
│   ├── SearchScreen.kt         # Music search
│   ├── LibraryScreen.kt        # Song library
│   ├── QuickPicksScreen.kt     # ML recommendations
│   └── [Other screens]
├── 📁 components/               # Reusable UI components
│   ├── 📁 player/              # Player controls
│   ├── 📁 section/             # Section components
│   ├── 📁 state/               # UI state models
│   ├── 📁 tabs/                # Tab components
│   ├── 📁 onboarding/          # Onboarding components
│   └── 📁 utils/               # Component utilities
├── 📁 viewmodels/              # ViewModels (MVVM pattern)
│   ├── PlayerViewModel.kt      # Playback logic
│   ├── HomeViewModel.kt        # Home screen logic
│   └── DynamicBgViewModel.kt   # Background management
├── 📁 background/              # ✅ Background components
├── 📁 dynamic/                 # ✅ Dynamic UI elements
├── 📁 guide/                   # ✅ Onboarding guides
├── 📁 model/                   # ✅ UI models
├── 📁 utils/                   # ✅ UI utilities
└── 📁 ui/                      # UI-specific packages
    ├── 📁 theme/               # App theming (Color, Type, Theme, Dimensions, ScalingProvider)
    ├── 📁 widget/              # App widgets (MusicWidgetProvider)
    └── 📁 adapter/             # ⚠️ Legacy RecyclerView adapter (FormatAdapter)
```

### 2. **Domain Layer**
```
domain/
├── 📁 usecase/                 # ✅ Business use cases
│   └── [Use case implementations]
└── 📁 model/                   # ✅ Domain models
    └── [Domain entities]
```

### 3. **Data Layer**
```
data/
├── 📁 local/                   # Room database
│   ├── MusicDatabase.kt       # Database setup
│   ├── 📁 dao/               # Data Access Objects
│   └── 📁 entities/          # Database entities
├── 📁 model/                  # Data models
├── 📁 repository/             # Repository implementations
├── 📁 preferences/            # User preferences
├── 📁 cache/                  # Caching layer
└── 📁 pagination/             # Pagination utilities
```

### 4. **Core/Infrastructure Layer**
```
core/
├── 📁 di/                      # ✅ Dependency injection
│   └── AppModule.kt           # DI configuration
├── 📁 service/                 # Android services
│   └── MusicService.kt        # Background music service
├── 📁 ml/                      # Machine Learning components
│   ├── MusicRecommendationManager.kt
│   ├── 📁 agents/             # ML agents (Statistical, Collaborative, etc.)
│   └── 📁 models/             # ML model definitions
├── 📁 player/                 # Audio playback system
│   ├── MusicPlayer.kt         # Core player
│   ├── QueueManager.kt        # Playback queue
│   ├── PlaybackCollector.kt   # Analytics collection
│   └── PreloadManager.kt      # Song preloading
├── 📁 network/                # Network operations
│   ├── OnlineSearchManager.kt
│   └── YouTubeInnerTubeClient.kt
├── 📁 download/               # Download management
├── 📁 chaquopy/               # Python integration
└── 📁 utils/                  # Core utilities
```

## 📋 Architecture Patterns

### MVVM (Model-View-ViewModel)
- **Views**: Compose screens in `ui/screens/`
- **ViewModels**: Business logic in `ui/viewmodels/`
- **Models**: Data classes in `core/data/model/` and `data/local/entities/`

### Repository Pattern
- **Repositories**: Data access abstraction in `data/repository/` and `core/data/repository/`
- **Data Sources**: Local (Room) and remote (APIs) data sources

### Service Layer
- **Services**: Background operations in `service/`
- **Managers**: Specialized managers in `core/` subdirectories

## 🎯 Best Practices & Guidelines

### 1. **Package Organization**
- **Feature-based**: Group related classes together
- **Layer separation**: Keep UI, business logic, and data separate
- **Single responsibility**: Each package/class has one clear purpose

### 2. **Naming Conventions**
- **Packages**: Lowercase, descriptive names (e.g., `musicplayer`, `recommendation`)
- **Classes**: PascalCase, descriptive (e.g., `MusicPlayer`, `RecommendationManager`)
- **Functions**: camelCase, action-oriented (e.g., `playSong()`, `loadRecommendations()`)

### 3. **Code Structure**
- **Compose screens**: Keep in `presentation/screens/`, use ViewModels for logic
- **Reusable components**: Place in `presentation/components/`
- **Business logic**: Extract to `domain/usecase/` when complex
- **Domain models**: Keep in `domain/model/`
- **Data models**: Keep in `data/model/` and `data/local/entities/`
- **UI models**: Keep in `presentation/model/`

### 4. **Testing**
- **Unit tests**: `test/` directory for ViewModels, utilities
- **Integration tests**: `androidTest/` for UI and database tests
- **Test coverage**: Aim for critical business logic coverage

## 🔄 Architecture Migration Status

### ✅ Completed Improvements (December 5, 2025)
1. ✅ **Moved `screens/` → `presentation/screens/`** - Screens now at presentation level, not nested under ui
2. ✅ **Moved `constraints/` → `presentation/ui/theme/`** - Design tokens (Dimensions.kt) now with theming
3. ✅ **Flattened `presentation/` structure** - Moved background, dynamic, guide, model, utils to presentation level
4. ✅ **Added `domain/` layer** - Business logic separation with usecase/ and model/ packages
5. ✅ **Clean architecture layers** - Clear separation: presentation/ → domain/ → data/ → core/

### 📝 Legacy Components
- ⚠️ **`presentation/ui/adapter/FormatAdapter.kt`** - Legacy RecyclerView adapter still in use by FormatSelectionBottomSheetDialog. Consider migrating to Compose when refactoring format selection UI.

### 🎯 Future Enhancements
1. **Implement Hilt/Dagger** for better dependency injection
2. **Migrate FormatAdapter** to Compose component when refactoring format selection
3. **Add feature modules** for better modularity (if app grows significantly)
4. **Enhance domain layer** with more use cases as business logic complexity grows

## 📚 Documentation Structure

```
Docs/
├── README.md                   # Documentation index
├── APP_OVERVIEW.md            # High-level architecture
├── PYTHON_MODEL.md            # ML model documentation
├── [Feature docs]             # Individual feature docs
└── [Implementation guides]    # Technical implementation details
```

## 🚀 Development Workflow

### Adding New Features
1. **Identify layer**: Determine if it's UI, business logic, or data
2. **Choose package**: Place in appropriate directory
3. **Follow patterns**: Use established architecture patterns
4. **Add tests**: Include unit/integration tests
5. **Update docs**: Document new components if significant

### Code Review Checklist
- [ ] Proper package placement
- [ ] MVVM pattern followed
- [ ] Dependency injection used
- [ ] Tests included
- [ ] Documentation updated
- [ ] Naming conventions followed

## 🔍 Analysis vs. Industry Standards

### Current Structure (Updated: December 5, 2025)

**✅ Industry-Standard Clean Architecture Achieved:**
- ✅ **Four-layer architecture**: presentation → domain → data → core
- ✅ **Clean presentation layer**: Screens, components, and viewmodels properly organized
- ✅ **Domain layer**: Business logic separation with usecases and domain models
- ✅ **Data layer**: Repository pattern with local/remote data sources
- ✅ **Core layer**: Infrastructure services (DI, player, ML, network)
- ✅ **MVVM pattern**: ViewModels mediate between UI and domain layer
- ✅ **Modern tech stack**: Kotlin, Jetpack Compose, Room, ExoPlayer
- ✅ **Comprehensive documentation**: Well-documented architecture

**Current Structure:**

```
app/src/main/java/com/just_for_fun/synctax/
├── 📁 presentation/           # ✅ Presentation layer
│   ├── 📁 screens/           # ✅ Compose screens (flat structure)
│   ├── 📁 components/        # UI components
│   ├── 📁 viewmodels/        # ViewModels
│   ├── 📁 background/        # ✅ Background components
│   ├── 📁 dynamic/           # ✅ Dynamic UI
│   ├── 📁 guide/             # ✅ Onboarding guides
│   ├── 📁 model/             # ✅ UI models
│   ├── 📁 utils/             # ✅ UI utilities
│   └── 📁 ui/                # UI-specific packages
│       ├── 📁 theme/         # ✅ Theming (incl. Dimensions)
│       ├── 📁 widget/        # App widgets
│       └── 📁 adapter/       # ⚠️ Legacy RecyclerView adapter
├── 📁 domain/                 # ✅ Business logic layer
│   ├── 📁 usecase/           # Business use cases
│   └── 📁 model/             # Domain models
├── 📁 data/                   # ✅ Data layer
│   ├── 📁 local/             # Room database
│   ├── 📁 model/             # Data models
│   ├── 📁 repository/        # Repository implementations
│   ├── 📁 preferences/       # User preferences
│   ├── 📁 cache/             # Caching
│   └── 📁 pagination/        # Pagination
└── 📁 core/                   # ✅ Infrastructure layer
    ├── 📁 di/                # ✅ Dependency injection
    ├── 📁 service/           # Android services
    ├── 📁 ml/                # Machine learning
    ├── 📁 player/            # Audio playback
    ├── 📁 network/           # Network operations
    ├── 📁 download/          # Download management
    ├── 📁 chaquopy/          # Python integration
    └── 📁 utils/             # Core utilities
```

### Comparison with Industry Standards

**Alignment with Industry Best Practices:**
- ✅ **Clean Architecture**: Follows Uncle Bob's clean architecture principles
- ✅ **Separation of Concerns**: Clear boundaries between layers
- ✅ **Dependency Rule**: Dependencies point inward (presentation → domain → data)
- ✅ **Modern Android**: Matches Google's recommended app architecture
- ✅ **Scalability**: Structure supports growth and feature modules

**Comparison with Popular Apps:**
- **Google's Now in Android**: Similar layered approach with domain/data/ui separation
- **Retro Music Player**: Comparable structure with modern Kotlin patterns
- **Industry Standard**: Meets and exceeds typical open-source music player architectures

## 📞 Support

For questions about the architecture or contribution guidelines, refer to:
- `Docs/README.md` for documentation index
- `CONTRIBUTING.md` (if created) for contribution guidelines
- GitHub Issues for architecture discussions</content>
<parameter name="filePath">e:\Git-Hub\SnycTax\ARCHITECTURE.md