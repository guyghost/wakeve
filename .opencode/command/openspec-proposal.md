---
agent: build
description: Scaffold a new OpenSpec change and validate strictly.
---
L'utilisateur a soumis la proposition de modification suivante. Veuillez utiliser les instructions d'OpenSpec pour créer votre proposition de modification.
<UserRequest>
  $ARGUMENTS
</UserRequest>
<!-- OPENSPEC:START -->
**Recommandations**
- Privilégiez les implémentations simples et minimales et n'ajoutez de la complexité que lorsque cela est demandé ou clairement nécessaire.
- Limitez strictement les modifications au résultat attendu.
- Consultez `openspec/AGENTS.md` (situé dans le répertoire `openspec/`; exécutez `ls openspec` ou `openspec update` si vous ne le voyez pas) si vous avez besoin de conventions OpenSpec supplémentaires ou de clarifications.
- Identifiez tout détail vague ou ambigu et posez les questions nécessaires avant de modifier les fichiers.

**Étapes**
1. Consultez `openspec/project.md`, exécutez `openspec list` et `openspec list --specs`, et examinez le code ou la documentation associés (par exemple, via `rg`/`ls`) afin de fonder la proposition sur le comportement actuel; notez les lacunes nécessitant des éclaircissements.
2. Choisissez un identifiant unique `change-id` (identifiant de changement) basé sur un verbe et créez les fichiers `proposal.md`, `tasks.md` et `design.md` (le cas échéant) sous `openspec/changes/<id>/`.
3. Traduisez le changement en capacités ou exigences concrètes, en décomposant les efforts multi-périphériques en deltas de spécification distincts, avec des relations et une séquence claires.
4. Consignez le raisonnement architectural dans `design.md` lorsque la solution s'étend sur plusieurs systèmes, introduit de nouveaux modèles ou nécessite une discussion sur les compromis avant de valider les spécifications.
5. Rédigez les deltas de spécification dans `changes/<id>/specs/<capability>/spec.md` (un dossier par capacité) en utilisant `## ADDED|MODIFIED|REMOVED Requirements` avec au moins un `#### Scenario:` par exigence et en faisant référence aux capacités connexes le cas échéant. 6. Rédigez le fichier `tasks.md` sous forme de liste ordonnée de petites tâches vérifiables, permettant de visualiser la progression pour l'utilisateur, incluant la validation (tests, outils) et mettant en évidence les dépendances et les tâches parallélisables.
7. Validez avec `openspec validate <id> --strict` et corrigez chaque problème avant de soumettre la proposition.

**Référence**
- Utilisez `openspec show <id> --json --deltas-only` ou `openspec show <spec> --type spec` pour examiner les détails en cas d'échec de la validation.
- Recherchez les exigences existantes avec `rg -n "Requirement:|Scenario:" openspec/specs` avant d'en rédiger de nouvelles.
- Explorez le code source avec `rg <mot-clé>`, `ls` ou en lisant directement les fichiers afin que les propositions correspondent aux réalités de l'implémentation actuelle.
<!-- OPENSPEC:END -->
