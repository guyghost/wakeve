# Context: Phase 6 Performance - Image Optimization with Coil (Ralph Mode)

## Ralph Mode Configuration
- **Enabled**: true
- **Max Iterations**: 10
- **Current Iteration**: 3
- **Mode**: Standard (not TDD for UI components)

## Objective
Implémenter l'optimisation du chargement des images avec Coil:
- Memory cache (25% de la mémoire disponible)
- Disk cache (50MB)
- Placeholders et error states
- Crossfade animation (200ms)
- Transformations (Circle, Rounded corners)

## Current State
- ✅ Database indexes completed
- ✅ Pagination implemented
- ✅ Security fixes committed
- 🔄 Starting P0.4: Image Optimization

## Technical Stack
- **Android**: Coil 2.x avec Jetpack Compose
- **iOS**: Kingfisher ou native (à définir)
- **Backend**: Déjà supporte upload d'images

## Acceptance Criteria (from Phase 6 specs)
- [ ] Coil intégré avec Memory cache, Disk cache
- [ ] Placeholders gris/borders pour images en chargement
- [ ] Error placeholders pour images échouées
- [ ] Crossfade animation (200ms)
- [ ] Transformation: Circle, Rounded corners
- [ ] Tests d'intégration

## Files to Create/Modify
1. `wakeveApp/build.gradle.kts` - Add Coil dependency
2. `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/components/ImageLoader.kt` - Coil configuration
3. `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/components/AsyncImage.kt` - Reusable image component
4. Update existing screens to use new image component

## Implementation Details

### Coil Configuration
```kotlin
object ImageLoader {
    val imageLoader = Coil.imageLoader {
        memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }
        diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(50 * 1024 * 1024)
                .build()
        }
        crossfade(true)
        placeholder(R.drawable.placeholder)
        error(R.drawable.error_placeholder)
    }
}
```

### Reusable Component
```kotlin
@Composable
fun WakeveAsyncImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    transformation: ImageTransformation = ImageTransformation.None
)

enum class ImageTransformation {
    None,
    Circle,
    RoundedCorners
}
```

## Next Tasks Priority
1. P0.4 Image Optimization (this task)
2. P0.5 Memory Profiling
3. P1.1 Analytics Provider Interface
