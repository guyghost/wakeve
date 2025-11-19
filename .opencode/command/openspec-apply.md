---
agent: build
description: Implement an approved OpenSpec change and keep GitHub issue tasks in sync.
---
<!-- OPENSPEC:START -->
**Recommandations**
- Privilégiez les implémentations simples et minimales et n'ajoutez de la complexité que lorsque cela est demandé ou clairement nécessaire.
- Veillez à ce que les modifications restent strictement limitées au résultat attendu.
- Consultez le fichier `openspec/AGENTS.md` (situé dans le répertoire `openspec/`; exécutez `ls openspec` ou `openspec update` si vous ne le voyez pas) pour obtenir des informations supplémentaires sur les conventions OpenSpec ou des clarifications.

**Workflow GitHub**
L'implémentation suit l'approbation du PR de proposition et maintient la synchronisation avec l'issue GitHub.

**Étapes**
Suivez ces étapes comme des tâches à accomplir et effectuez-les une par une.

1. **Gate d'approbation**: Vérifiez que le PR de proposition a été approuvé et mergé. Ne démarrez PAS l'implémentation avant cette approbation.

2. **Lecture de contexte**: Lisez les fichiers suivants pour confirmer la portée et les critères d'acceptation:
    - `openspec/changes/<id>/proposal.md` - Vue d'ensemble et raison d'être
    - GitHub Project Item - Décisions techniques, architecture et exigences détaillées avec scénarios
    - GitHub Issue #N - Liste de contrôle des tâches

3. **Implémentation séquentielle**: Traitez les tâches de l'issue GitHub une par une, en minimisant les modifications et en les concentrant sur le changement demandé:
    - Créez des PRs d'implémentation avec le format: `[#<issue>] <description>`
    - Liez chaque PR à l'issue originale
    - Suivez les décisions architecturales documentées dans l'item du projet GitHub
    - Respectez les exigences et scénarios du projet GitHub

4. **Mise à jour de l'issue**: Au fur et à mesure de la progression:
    - Cochez les tâches complétées dans l'issue GitHub: `- [x]`
    - Ajoutez des commentaires pour signaler les jalons importants
    - Liez les PRs d'implémentation dans les commentaires de l'issue

5. **Confirmation de réalisation**: Avant de fermer l'issue:
    - Assurez-vous que chaque élément de la liste de contrôle est terminé
    - Vérifiez que tous les PRs d'implémentation sont mergés
    - Confirmez que les tests passent
    - Validez que le comportement correspond aux scénarios du projet GitHub

6. **Fermeture**:
    - Fermez l'issue GitHub avec un commentaire de synthèse
    - Mettez à jour le statut du projet GitHub: `Status: Implemented`
    - Préparez pour l'archivage (étape suivante: `openspec-archive`)

**Référence**
- Utilisez `openspec show <id> --json --deltas-only` si vous avez besoin d'informations supplémentaires issues de la proposition lors de la mise en œuvre.
- Consultez `openspec list` ou `openspec show <item>` lorsque des informations supplémentaires sont nécessaires.
- Vérifiez le projet GitHub pour les décisions de design et les compromis dans la description de l'item
- Référencez l'issue GitHub dans tous les commits: `[#<issue-number>] <description>`

**Format des commits**
```bash
git commit -m "[#42] Implement user authentication endpoint"
git commit -m "[#42] Add tests for OAuth2 flow"
git commit -m "[#42] Update documentation"
```

**Aide-mémoire**
```
Approval → Read (proposal + wiki) → Implement → Update Issue → 
Test → Close Issue → Archive
```
<!-- OPENSPEC:END -->