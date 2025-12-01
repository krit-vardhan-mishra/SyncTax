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
│   │   │   └── 📁 [Package directories - see below]
│   │   ├── 📁 python/                   # Chaquopy Python scripts
│   │   └── 📁 res/                      # Android resources
│   ├── 📁 androidTest/                  # Instrumentation tests
│   └── 📁 test/                         # Unit tests
└── 📁 release/                          # Release artifacts
```

## 🏗️ Architecture Layers

### 1. **Presentation Layer (UI)**
```
ui/
├── 📁 screens/                  # Main app screens (Compose)
│   ├── HomeScreen.kt           # Main dashboard
│   ├── SearchScreen.kt         # Music search
│   ├── LibraryScreen.kt        # Song library
│   ├── QuickPicksScreen.kt     # ML recommendations
│   └── [Other screens]
├── 📁 components/               # Reusable UI components
│   ├── 📁 app/                 # App-specific components
│   ├── 📁 card/                # Card components
│   ├── 📁 player/              # Player controls
│   └── 📁 section/             # Section components
├── 📁 viewmodels/              # ViewModels (MVVM pattern)
│   ├── PlayerViewModel.kt      # Playback logic
│   ├── HomeViewModel.kt        # Home screen logic
│   └── DynamicBgViewModel.kt   # Background management
├── 📁 theme/                   # App theming
├── 📁 background/              # Background components
├── 📁 dynamic/                 # Dynamic UI elements
├── 📁 guide/                   # Onboarding guides
├── 📁 model/                   # UI models
├── 📁 adapter/                 # Legacy adapters (consider migrating)
└── 📁 utils/                   # UI utilities
```

### 2. **Domain/Business Logic Layer**
```
core/
├── 📁 ml/                      # Machine Learning components
│   ├── MusicRecommendationManager.kt
│   ├── 📁 agents/             # ML agents (Statistical, Collaborative, etc.)
│   └── 📁 models/             # ML model definitions
├── 📁 player/                 # Audio playback system
│   ├── MusicPlayer.kt         # Core player
│   ├── QueueManager.kt        # Playback queue
│   ├── PlaybackCollector.kt   # Analytics collection
│   └── PreloadManager.kt      # Song preloading
├── 📁 data/                   # Data processing
│   ├── 📁 cache/              # Caching managers
│   ├── 📁 local/              # Local data models
│   ├── 📁 model/              # Data models
│   ├── 📁 pagination/         # Pagination logic
│   ├── 📁 preferences/        # User preferences
│   └── 📁 repository/         # Data repositories
├── 📁 network/                # Network operations
│   ├── OnlineSearchManager.kt
│   └── YouTubeInnerTubeClient.kt
├── 📁 utils/                  # Core utilities
├── 📁 chaquopy/               # Python integration
└── 📁 download/               # Download management
```

### 3. **Data Layer**
```
data/
├── 📁 local/                  # Room database entities & DAOs
│   ├── MusicDatabase.kt       # Database setup
│   ├── 📁 dao/               # Data Access Objects
│   └── 📁 entities/          # Database entities
├── 📁 preferences/            # SharedPreferences wrappers
└── 📁 repository/             # Repository implementations
```

### 4. **Infrastructure Layer**
```
├── 📁 service/                 # Android services
│   └── MusicService.kt        # Background music service
├── 📁 di/                     # Dependency injection
│   └── AppModule.kt           # DI configuration
├── 📁 util/                   # Application utilities
├── 📁 widget/                 # App widgets
└── 📁 constraints/            # Design constraints (consider moving to theme/)
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
- **Compose screens**: Keep in `ui/screens/`, use ViewModels for logic
- **Reusable components**: Place in `ui/components/`
- **Business logic**: Isolate in appropriate core packages
- **Data models**: Keep in `data/` or `core/data/`

### 4. **Testing**
- **Unit tests**: `test/` directory for ViewModels, utilities
- **Integration tests**: `androidTest/` for UI and database tests
- **Test coverage**: Aim for critical business logic coverage

## 🔄 Migration & Improvements

### Immediate Improvements
1. **Move `constraints/` to `ui/theme/`** - Design tokens belong with theming
2. **Migrate `ui/adapter/` to `ui/components/`** - Legacy RecyclerView adapters should be Compose components
3. **Consolidate repositories** - Merge `data/repository/` and `core/data/repository/`
4. **Add domain layer** - Extract business logic from ViewModels into use cases

### Future Enhancements
1. **Add `domain/` package** for use cases and business rules
2. **Implement Hilt/Dagger** for better dependency injection
3. **Add `common/` package** for shared utilities across modules
4. **Create feature modules** for better modularity (if app grows)

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

### Current Structure Assessment
**Strengths:**
- ✅ Clear separation of UI, core, and data layers
- ✅ MVVM pattern implementation
- ✅ Repository pattern for data access
- ✅ Comprehensive documentation
- ✅ Modern tech stack (Kotlin, Compose, Room)

**Areas for Improvement:**
- ⚠️ Some legacy packages (`adapter/`, `constraints/`)
- ⚠️ Scattered repository implementations
- ⚠️ Missing domain layer for complex business logic
- ⚠️ Could benefit from feature-based modules

### Comparison with Industry Leaders

Based on analysis of popular Android music apps (Phonograph, Vinyl Music Player, Retro Music Player):

**Similarities:**
- Feature-based package organization
- UI/data/service separation
- Standard Android project structure
- Comprehensive testing setup

**Differences:**
- **Retro Music Player**: Uses more modern Kotlin features, better layered architecture with `repository/` and `network/` packages
- **Industry Trend**: Moving toward feature modules and domain-driven design
- **Best Practice**: Retro's approach with `activities/`, `fragments/`, `db/`, `network/` subpackages

### Recommended Structure Evolution

For better alignment with industry standards:

```
app/src/main/java/com/just_for_fun/synctax/
├── 📁 domain/                 # Business logic layer (NEW)
│   ├── 📁 usecase/           # Use cases
│   └── 📁 model/             # Domain models
├── 📁 data/                  # Data layer
│   ├── 📁 local/            # Room implementation
│   ├── 📁 remote/           # API clients
│   └── 📁 repository/       # Repository implementations
├── 📁 presentation/          # UI layer (renamed from ui/)
│   ├── 📁 screens/          # Compose screens
│   ├── 📁 components/       # Reusable components
│   └── 📁 viewmodels/       # ViewModels
├── 📁 core/                  # Infrastructure
│   ├── 📁 di/               # Dependency injection
│   ├── 📁 service/          # Android services
│   └── 📁 utils/            # Shared utilities
└── 📁 feature/               # Feature modules (future)
```

This structure provides better scalability and follows clean architecture principles while maintaining Android-specific patterns.

## 📞 Support

For questions about the architecture or contribution guidelines, refer to:
- `Docs/README.md` for documentation index
- `CONTRIBUTING.md` (if created) for contribution guidelines
- GitHub Issues for architecture discussions</content>
<parameter name="filePath">e:\Git-Hub\SnycTax\ARCHITECTURE.md