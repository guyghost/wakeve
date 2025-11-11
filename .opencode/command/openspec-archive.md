---
agent: build
description: Archive a deployed OpenSpec change and update specs.
---
<!-- OPENSPEC:START -->
**Recommandations**
- Privilégiez les implémentations simples et minimales et n'ajoutez de la complexité que lorsque cela est demandé ou clairement nécessaire.
- Limitez strictement les modifications au résultat attendu.
- Consultez le fichier `openspec/AGENTS.md` (situé dans le répertoire `openspec/` ; exécutez `ls openspec` ou `openspec update` si vous ne le voyez pas) pour obtenir des conventions OpenSpec supplémentaires ou des clarifications.

**Étapes**
1. Identifiez l'ID de la modification demandée (via l'invite de commande ou `openspec list`).
2. Exécutez `openspec archive <id> --yes` pour que l'interface de ligne de commande déplace la modification et applique les mises à jour des spécifications sans invite (utilisez `--skip-specs` uniquement pour les opérations d'outillage).
3. Vérifiez la sortie de la commande pour confirmer que les spécifications cibles ont été mises à jour et que la modification a été enregistrée dans `changes/archive/`. 4. Validez avec `openspec validate --strict` et vérifiez avec `openspec show <id>` si quelque chose semble anormal.

**Référence**
- Vérifiez les spécifications mises à jour avec `openspec list --specs` et corrigez les problèmes de validation avant de les transmettre.
<!-- OPENSPEC:END -->
