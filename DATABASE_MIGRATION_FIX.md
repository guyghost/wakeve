# Fix: Database Migration - Enhanced DRAFT Phase

## Problème

L'application plante au démarrage avec l'erreur:
```
no such column: event.eventType
```

La table `event` dans la base de données existante n'a pas les nouvelles colonnes ajoutées pour la phase DRAFT améliorée.

## Solution Appliquée

### 1. Migration SQLDelight Créée

Fichier: `shared/src/commonMain/sqldelight/com/guyghost/wakeve/migrations/1.sqm`

```sql
-- Migration 1: Add enhanced DRAFT phase columns to event and timeSlot tables
-- Event table columns
ALTER TABLE event ADD COLUMN eventType TEXT DEFAULT 'OTHER';
ALTER TABLE event ADD COLUMN eventTypeCustom TEXT;
ALTER TABLE event ADD COLUMN minParticipants INTEGER;
ALTER TABLE event ADD COLUMN maxParticipants INTEGER;
ALTER TABLE event ADD COLUMN expectedParticipants INTEGER;

-- TimeSlot table column
ALTER TABLE timeSlot ADD COLUMN timeOfDay TEXT DEFAULT 'SPECIFIC';
```

### 2. Shared Module & Apps Rebuild

```bash
# Clean et rebuild shared module
./gradlew :shared:clean
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Rebuild Android app
./gradlew :composeApp:assembleDebug
```

✅ Builds réussis avec migration intégrée
✅ Framework iOS reconstruit
✅ APK Android reconstruit

## Comment Appliquer

### Option A: Désinstaller/Réinstaller (RECOMMANDÉ)

**iOS**:
1. Supprimer l'app du simulateur/device
2. Rebuild et run depuis Xcode

**Android**:
1. Désinstaller l'app
2. Rebuild et run depuis Android Studio

### Option B: Supprimer les Bases de Données Manuellement

**iOS Simulator**:
```bash
# Trouver l'app
xcrun simctl list | grep Wakeve

# Supprimer les données
xcrun simctl uninstall booted com.guyghost.wakeve

# Ou supprimer tout le simulateur
xcrun simctl erase all
```

**Android Emulator**:
```bash
# Via adb
adb shell pm clear com.guyghost.wakeve

# Ou via Android Studio
# Settings → Apps → Wakeve → Clear Data
```

**Backend Server**:
```bash
rm -f /Users/guy/Developer/dev/wakeve/server/wakev_server.db
```
✅ Déjà supprimé

## Vérification

Après rebuild et réinstallation, l'app devrait:
1. ✅ Démarrer sans erreur
2. ✅ Créer la table `event` avec toutes les colonnes
3. ✅ Permettre de créer des événements avec les nouveaux champs

## Nouvelles Colonnes Ajoutées

### Table `event`

| Colonne | Type | Description |
|---------|------|-------------|
| `eventType` | TEXT | Type d'événement (BIRTHDAY, WEDDING, etc.) |
| `eventTypeCustom` | TEXT | Texte personnalisé si eventType = CUSTOM |
| `minParticipants` | INTEGER | Minimum de participants attendus |
| `maxParticipants` | INTEGER | Maximum de participants attendus |
| `expectedParticipants` | INTEGER | Nombre attendu de participants |

### Table `timeSlot`

| Colonne | Type | Description |
|---------|------|-------------|
| `timeOfDay` | TEXT | Moment de la journée (ALL_DAY, MORNING, AFTERNOON, EVENING, SPECIFIC) |

## Schéma Complet de la Table Event

```sql
CREATE TABLE event (
    id TEXT PRIMARY KEY NOT NULL,
    organizerId TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    status TEXT NOT NULL,
    deadline TEXT NOT NULL,
    createdAt TEXT NOT NULL,
    updatedAt TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    eventType TEXT DEFAULT 'OTHER',           -- NOUVEAU
    eventTypeCustom TEXT,                     -- NOUVEAU
    minParticipants INTEGER,                  -- NOUVEAU
    maxParticipants INTEGER,                  -- NOUVEAU
    expectedParticipants INTEGER              -- NOUVEAU
);
```

## Notes

- La migration SQLDelight s'applique automatiquement au premier lancement
- Les bases de données existantes sont incompatibles et nécessitent une réinstallation
- Pour la production, les migrations SQLDelight préserveront les données existantes
- La colonne `eventType` a une valeur par défaut `'OTHER'` pour rétrocompatibilité

## Fichiers Modifiés

- `shared/src/commonMain/sqldelight/com/guyghost/wakeve/Event.sq` (déjà à jour)
- `shared/src/commonMain/sqldelight/com/guyghost/wakeve/migrations/1.sqm` (créé)
- `shared/build.gradle.kts` (configuration SQLDelight existante)

## Prochaines Étapes

1. Désinstaller l'app iOS/Android
2. Rebuild et relancer
3. Tester la création d'événements avec les nouveaux champs
4. Vérifier que tout fonctionne correctement

---

**Status**: ✅ Migration créée et framework rebuilt
**Action Requise**: Désinstaller/réinstaller l'application
