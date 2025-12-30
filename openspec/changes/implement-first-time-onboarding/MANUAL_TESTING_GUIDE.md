# Guide de Tests Manuels - Onboarding

Ce document fournit les instructions pour exécuter les tests manuels restants (A1.4-A1.6 et I1.9-I1.11).

## Prérequis

### Android
- Android Studio installé avec SDK Android 34+
- Émulateur Android configuré (API 34+ recommandé)
- OU device physique Android avec USB debugging activé

### iOS
- Xcode installé (version 15.0+)
- Simulateur iOS (iPhone 15 ou supérieur recommandé)
- OU device physique iOS avec provisioning profile

## Tests Android (A1.4-A1.6)

### A1.4 : Tester le flow de premier lancement sur Android

**Objectif** : Vérifier que l'onboarding s'affiche correctement lors du premier lancement

**Étapes** :

1. **Préparer l'émulateur/device**
   ```bash
   # Lancer l'émulateur depuis Android Studio
   # OU connecter un device physique via USB
   
   # Vérifier que le device est détecté
   adb devices
   ```

2. **Désinstaller l'application (si déjà installée)**
   ```bash
   adb uninstall com.guyghost.wakeve
   ```

3. **Build et installer l'application**
   ```bash
   cd /Users/guy/Developer/dev/wakeve
   ./gradlew composeApp:assembleDebug
   ./gradlew composeApp:installDebug
   ```

4. **Lancer l'application**
   - Ouvrir l'app depuis le launcher Android
   - **ATTENDRE** que le splash screen s'affiche
   - **VÉRIFIER** que l'onboarding s'affiche automatiquement

5. **Valider l'affichage**
   - [ ] Le splash screen s'affiche pendant ~2 secondes
   - [ ] L'onboarding s'affiche avec 4 pages
   - [ ] La première page affiche "Créer des Événements"
   - [ ] Les animations de transition sont fluides
   - [ ] Le bouton "Ignorer" est visible en haut à droite
   - [ ] Le bouton "Suivant" est visible en bas

6. **Naviguer entre les pages**
   - [ ] Swiper vers la gauche pour passer à la page 2 ("Collaborer")
   - [ ] Swiper vers la gauche pour passer à la page 3 ("Organiser")
   - [ ] Swiper vers la gauche pour passer à la page 4 ("Profiter")
   - [ ] Le bouton "Suivant" devient "Commencer" sur la page 4
   - [ ] Les indicateurs de page (dots) se mettent à jour

7. **Compléter l'onboarding**
   - [ ] Cliquer sur "Commencer" sur la dernière page
   - [ ] L'application navigue vers l'écran d'accueil
   - [ ] L'écran d'accueil affiche la liste d'événements vide

**Résultat attendu** : ✅ Onboarding s'affiche et fonctionne correctement

---

### A1.5 : Tester que l'onboarding ne s'affiche pas aux lancements suivants

**Objectif** : Vérifier que l'onboarding ne s'affiche plus après le premier lancement

**Étapes** :

1. **Fermer complètement l'application**
   - Appuyer sur le bouton Home
   - Swiper vers le haut pour afficher les apps récentes
   - Swiper vers le haut sur Wakeve pour la fermer

2. **Relancer l'application**
   - Ouvrir l'app depuis le launcher Android
   - **ATTENDRE** que le splash screen s'affiche

3. **Valider le comportement**
   - [ ] Le splash screen s'affiche pendant ~2 secondes
   - [ ] L'application navigue **directement** vers l'écran d'accueil
   - [ ] L'onboarding **n'apparaît PAS**

4. **Tester plusieurs lancements**
   - Fermer et relancer l'app 3 fois
   - [ ] Vérifier que l'onboarding ne s'affiche jamais

**Résultat attendu** : ✅ Onboarding ne s'affiche plus après le premier lancement

---

### A1.6 : Tester le skip de l'onboarding

**Objectif** : Vérifier que le bouton "Ignorer" fonctionne correctement

**Étapes** :

1. **Réinitialiser l'application**
   ```bash
   # Désinstaller et réinstaller l'app
   adb uninstall com.guyghost.wakeve
   ./gradlew composeApp:installDebug
   ```

2. **Lancer l'application**
   - Ouvrir l'app depuis le launcher Android
   - **ATTENDRE** que l'onboarding s'affiche

3. **Tester le skip depuis la page 1**
   - [ ] Cliquer sur "Ignorer" en haut à droite
   - [ ] L'application navigue directement vers l'écran d'accueil
   - [ ] L'onboarding ne s'affiche plus aux lancements suivants

4. **Réinitialiser et tester le skip depuis la page 2**
   ```bash
   adb uninstall com.guyghost.wakeve
   ./gradlew composeApp:installDebug
   ```
   - Ouvrir l'app
   - Swiper vers la page 2
   - [ ] Cliquer sur "Ignorer"
   - [ ] L'application navigue directement vers l'écran d'accueil

5. **Réinitialiser et tester le skip depuis la page 3**
   - Répéter le processus depuis la page 3
   - [ ] Vérifier que le skip fonctionne correctement

**Résultat attendu** : ✅ Le bouton "Ignorer" fonctionne depuis n'importe quelle page

---

## Tests iOS (I1.9-I1.11)

### I1.9 : Tester le flow de premier lancement sur iOS (simulator)

**Objectif** : Vérifier que l'onboarding s'affiche correctement lors du premier lancement

**Étapes** :

1. **Ouvrir le projet dans Xcode**
   ```bash
   cd /Users/guy/Developer/dev/wakeve
   open iosApp/iosApp.xcodeproj
   ```

2. **Configurer le simulateur**
   - Sélectionner "iPhone 15" dans la barre d'outils Xcode
   - OU n'importe quel simulateur iOS 16+

3. **Réinitialiser le simulateur (si nécessaire)**
   - Menu : Device > Erase All Content and Settings...
   - Attendre que le simulateur redémarre

4. **Build et lancer l'application**
   - Appuyer sur Cmd+R (ou cliquer sur le bouton Play)
   - **ATTENDRE** que l'app se build et s'installe

5. **Valider l'affichage**
   - [ ] Le splash screen s'affiche brièvement
   - [ ] L'onboarding s'affiche avec 4 pages
   - [ ] La première page affiche "Créer des Événements"
   - [ ] Le design Liquid Glass est appliqué (matériaux vitrés)
   - [ ] Les corners sont continuous (arrondis iOS)
   - [ ] Le bouton "Ignorer" est visible en haut à droite
   - [ ] Le bouton "Suivant" est visible en bas

6. **Naviguer entre les pages**
   - [ ] Swiper vers la gauche pour passer à la page 2 ("Collaborer")
   - [ ] Swiper vers la gauche pour passer à la page 3 ("Organiser")
   - [ ] Swiper vers la gauche pour passer à la page 4 ("Profiter")
   - [ ] Le bouton "Suivant" devient "Commencer" sur la page 4
   - [ ] Les indicateurs de page (dots) se mettent à jour

7. **Compléter l'onboarding**
   - [ ] Tapper sur "Commencer" sur la dernière page
   - [ ] L'application navigue vers le ContentView (tab bar)
   - [ ] Le tab bar affiche 4 tabs (Events, Explore, Profile, +)

**Résultat attendu** : ✅ Onboarding s'affiche et fonctionne correctement

---

### I1.10 : Tester que l'onboarding ne s'affiche pas aux lancements suivants

**Objectif** : Vérifier que l'onboarding ne s'affiche plus après le premier lancement

**Étapes** :

1. **Fermer complètement l'application**
   - Appuyer sur Cmd+Shift+H (ou swiper vers le haut depuis le bas de l'écran)
   - Swiper vers le haut sur Wakeve pour la fermer complètement

2. **Relancer l'application**
   - Tapper sur l'icône Wakeve dans le simulateur
   - **ATTENDRE** que l'app se lance

3. **Valider le comportement**
   - [ ] Le splash screen s'affiche brièvement
   - [ ] L'application navigue **directement** vers le ContentView (tab bar)
   - [ ] L'onboarding **n'apparaît PAS**

4. **Tester plusieurs lancements**
   - Fermer et relancer l'app 3 fois
   - [ ] Vérifier que l'onboarding ne s'affiche jamais

**Résultat attendu** : ✅ Onboarding ne s'affiche plus après le premier lancement

---

### I1.11 : Tester le skip de l'onboarding

**Objectif** : Vérifier que le bouton "Ignorer" fonctionne correctement

**Étapes** :

1. **Réinitialiser l'application**
   - Dans le simulateur : Settings > General > iPhone Storage
   - Trouver Wakeve et cliquer sur "Delete App"
   - OU réinitialiser complètement le simulateur (Device > Erase All Content and Settings...)

2. **Relancer l'application depuis Xcode**
   - Appuyer sur Cmd+R dans Xcode
   - **ATTENDRE** que l'onboarding s'affiche

3. **Tester le skip depuis la page 1**
   - [ ] Tapper sur "Ignorer" en haut à droite
   - [ ] L'application navigue directement vers le ContentView (tab bar)
   - [ ] L'onboarding ne s'affiche plus aux lancements suivants

4. **Réinitialiser et tester le skip depuis la page 2**
   - Supprimer l'app et relancer depuis Xcode
   - Swiper vers la page 2
   - [ ] Tapper sur "Ignorer"
   - [ ] L'application navigue directement vers le ContentView

5. **Réinitialiser et tester le skip depuis la page 3**
   - Répéter le processus depuis la page 3
   - [ ] Vérifier que le skip fonctionne correctement

**Résultat attendu** : ✅ Le bouton "Ignorer" fonctionne depuis n'importe quelle page

---

## Validation Visuelle Finale

### Android

**Points de vérification** :
- [ ] Palette de couleurs Material You correcte (Blue 600 primary)
- [ ] Typographie Material Design 3 (Roboto)
- [ ] Animations fluides (fade + slide)
- [ ] Icônes Material Symbols correctement affichées
- [ ] Boutons Material3 (FilledTonalButton, TextButton)
- [ ] Aucun caractère corrompu dans le texte

### iOS

**Points de vérification** :
- [ ] Palette de couleurs Liquid Glass correcte (Blue #2563EB)
- [ ] Typographie SF Pro (système iOS)
- [ ] Matériaux vitrés visibles (background blur)
- [ ] Continuous corners appliqués
- [ ] Animations fluides (scale + opacity)
- [ ] Icônes SF Symbols correctement affichées
- [ ] Aucune couleur hardcodée (adaptable dark mode)

---

## Rapport de Tests

Une fois tous les tests complétés, remplir le rapport suivant :

### Android Tests
- [ ] A1.4 : Flow de premier lancement ✅ / ❌
- [ ] A1.5 : Pas d'onboarding aux lancements suivants ✅ / ❌
- [ ] A1.6 : Skip de l'onboarding ✅ / ❌

### iOS Tests
- [ ] I1.9 : Flow de premier lancement ✅ / ❌
- [ ] I1.10 : Pas d'onboarding aux lancements suivants ✅ / ❌
- [ ] I1.11 : Skip de l'onboarding ✅ / ❌

### Issues Identifiés
```
Décrire tout problème identifié ici :
- [ ] Issue 1 : ...
- [ ] Issue 2 : ...
```

### Recommandations
```
Décrire les recommandations pour améliorer l'expérience :
- [ ] Recommandation 1 : ...
- [ ] Recommandation 2 : ...
```

---

## Prochaines Étapes

Une fois tous les tests manuels validés :

1. Mettre à jour `tasks.md` avec les résultats
2. Créer un commit avec les tests manuels validés
3. Si tous les tests passent, archiver le changement OpenSpec :
   ```bash
   openspec archive implement-first-time-onboarding --yes
   ```

---

**Note** : Ce guide de tests manuels complète les 35 tests automatisés (25 Android + 10 iOS) déjà en place. Les tests automatisés couvrent la logique de persistance et les flows programmatiques, tandis que ces tests manuels valident l'expérience utilisateur et l'affichage visuel.
