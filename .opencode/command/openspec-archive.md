---
agent: build
description: Archive a deployed OpenSpec change, update specs, and mark GitHub resources.
---
<!-- OPENSPEC:START -->
**Recommandations**
- Privilégiez les implémentations simples et minimales et n'ajoutez de la complexité que lorsque cela est demandé ou clairement nécessaire.
- Limitez strictement les modifications au résultat attendu.
- Consultez le fichier `openspec/AGENTS.md` (situé dans le répertoire `openspec/` ; exécutez `ls openspec` ou `openspec update` si vous ne le voyez pas) pour obtenir des conventions OpenSpec supplémentaires ou des clarifications.

**Workflow GitHub**
L'archivage finalise le changement après le déploiement et met à jour toutes les ressources GitHub associées.

**Étapes**

1. **Pré-requis**: Vérifiez que:
    - Le changement est déployé en production
    - L'issue GitHub correspondante est fermée
    - Tous les PRs d'implémentation sont mergés
    - Les tests passent en production

2. **Identification**: Identifiez l'ID de la modification demandée:
    - Via l'invite de commande, ou
    - Via `openspec list` pour voir les changements actifs, ou
    - Via le numéro de l'issue GitHub fermée

3. **Archivage**: Exécutez `openspec archive <id> --yes` pour:
    - Déplacer `changes/<id>/` vers `changes/archive/YYYY-MM-DD-<id>/`
    - Appliquer automatiquement les deltas aux spécifications dans `specs/`
    - Générer un rapport des modifications appliquées
    - Utiliser `--skip-specs` uniquement pour les changements d'outillage sans impact sur les specs

4. **Vérification CLI**: Vérifiez la sortie de la commande pour confirmer:
    - Les spécifications cibles ont été mises à jour correctement
    - La modification a été enregistrée dans `changes/archive/`
    - Aucune erreur de validation n'est remontée

5. **Mise à jour GitHub Wiki**: Après l'archivage:
    - Accédez aux pages wiki `changes/<id>/design` et `changes/<id>/specs`
    - Mettez à jour le statut: `Status: Archived`
    - Ajoutez la date d'archivage: `Archived: YYYY-MM-DD`
    - Optionnellement, déplacez les pages vers `changes/archive/<id>/` dans le wiki
    - Ajoutez un lien vers les specs finales dans `openspec/specs/`

6. **Mise à jour GitHub Issue**:
    - Ajoutez un commentaire final sur l'issue fermée:
      ```markdown
      ✅ Change archived on YYYY-MM-DD
      - Specs updated: [list of affected specs]
      - Archive location: `changes/archive/YYYY-MM-DD-<id>/`
      - Wiki: [Updated status](link to wiki)
      ```
    - Ajoutez le label `archived` à l'issue

7. **Validation finale**:
    - Exécutez `openspec validate --strict` pour confirmer que tout est cohérent
    - Vérifiez avec `openspec show <id>` si quelque chose semble anormal
    - Confirmez que `openspec list --specs` affiche les specs mises à jour

8. **Commit et PR**: Créez un PR pour l'archivage:
   ```bash
   git checkout -b archive/<id>
   git add openspec/changes/archive/ openspec/specs/
   git commit -m "[#<issue>] Archive <id> after deployment"
   git push -u origin archive/<id>
   ```
    - Créez un PR de type "chore" ou "docs"
    - Liez l'issue originale dans la description
    - Mergez après revue

**Référence**
- Vérifiez les spécifications mises à jour avec `openspec list --specs`
- Corrigez les problèmes de validation avant de committer
- Consultez le wiki pour confirmer que toutes les exigences sont reflétées dans les specs finales
- Utilisez `openspec show <spec> --type spec` pour voir l'état final d'une spec

**Structure après archivage**
```
openspec/
├── specs/
│   └── [capability]/
│       └── spec.md              # ✅ Mis à jour avec les deltas
├── changes/
│   └── archive/
│       └── YYYY-MM-DD-<id>/
│           └── proposal.md       # ✅ Archivé

GitHub Wiki:
└── changes/
    └── <id>/
        ├── design.md             # ✅ Status: Archived
        └── specs.md              # ✅ Status: Archived

GitHub Issue #N:                  # ✅ Closed + Label: archived
```

**Aide-mémoire**
```
Deploy → Close Issue → Archive CLI → Update Wiki → 
Validate → Commit Archive → PR Archive → Done
```

**Commandes utiles**
```bash
# Archiver avec confirmation automatique
openspec archive <id> --yes

# Archiver sans mettre à jour les specs (outillage seulement)
openspec archive <id> --skip-specs --yes

# Valider après archivage
openspec validate --strict

# Voir l'état final d'une spec
openspec show <spec> --type spec

# Lister toutes les specs
openspec list --specs
```
<!-- OPENSPEC:END -->