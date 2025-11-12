---
agent: build
description: Scaffold a new OpenSpec change with GitHub workflow and validate strictly.
---
L'utilisateur a soumis la proposition de modification suivante. Veuillez utiliser les instructions d'OpenSpec pour créer votre proposition de modification en suivant le workflow GitHub.
<UserRequest>
$ARGUMENTS
</UserRequest>
<!-- OPENSPEC:START -->
**Recommandations**
- Privilégiez les implémentations simples et minimales et n'ajoutez de la complexité que lorsque cela est demandé ou clairement nécessaire.
- Limitez strictement les modifications au résultat attendu.
- Consultez `openspec/AGENTS.md` (situé dans le répertoire `openspec/`; exécutez `ls openspec` ou `openspec update` si vous ne le voyez pas) si vous avez besoin de conventions OpenSpec supplémentaires ou de clarifications.
- Identifiez tout détail vague ou ambigu et posez les questions nécessaires avant de modifier les fichiers.

**Workflow GitHub**
Ce processus intègre GitHub Issues, Projects et Pull Requests pour une gestion complète des changements.

**Étapes**
1. **Contexte**: Consultez `openspec/project.md`, exécutez `openspec list` et `openspec list --specs`, et examinez le code ou la documentation associés (par exemple, via `rg`/`ls`) afin de fonder la proposition sur le comportement actuel; notez les lacunes nécessitant des éclaircissements.

2. **Change ID**: Choisissez un identifiant unique `change-id` (identifiant de changement) basé sur un verbe (kebab-case: `add-`, `update-`, `remove-`, `refactor-`).

3. **GitHub Issue**: Créez une issue GitHub avec:
    - **Titre**: `[Change] <Description avec verbe>`
    - **Change ID**: `<change-id>` dans le corps
    - **Tasks**: Liste de contrôle des tâches d'implémentation
    - **Labels**: `openspec-change`, `in-progress`
    - Notez le numéro d'issue (ex: #42)

4. **GitHub Project - Item**: Créez un item GitHub Project `<change-id>: <Description avec verbe>` avec dans la **Description**:
    - **Design section**:
      - Contexte et contraintes
      - Objectifs (Goals) et Non-Objectifs (Non-Goals)
      - Architecture et composants
      - Décisions techniques avec alternatives considérées
      - Plan d'implémentation par phases
      - Risques et mitigation
      - Plan de migration et rollback
      - Questions ouvertes
    - **Specs section**:
      - Liste des spécifications affectées
      - `## ADDED Requirements` - Nouvelles capacités avec au moins un `#### Scenario:` (format Given-When-Then) par exigence
      - `## MODIFIED Requirements` - Comportements modifiés (copier l'exigence complète existante et la modifier)
      - `## REMOVED Requirements` - Fonctionnalités dépréciées avec raison et impact
      - Résultats de validation avec `openspec validate <id> --strict`
      - Cartographie des cross-références

5. **Mise à jour Issue**: Ajoutez le lien vers l'item du projet GitHub dans l'issue:
   ```markdown
   ## Documentation Links
   - **GitHub Project Item**: [URL du project item]
   ```

6. **Validation locale**: Exécutez `openspec validate <id> --strict` et corrigez tous les problèmes avant de continuer.

7. **Feature Branch**: Créez une branche:
   ```bash
   git checkout -b change/<change-id>
   ```

8. **Proposal local**: Créez `openspec/changes/<change-id>/proposal.md` avec:
    - **Change ID**: `<change-id>`
    - **Related Links**: Liens vers l'issue (#N) et l'item du projet GitHub
    - **Why**: 1-2 phrases sur le problème/opportunité
    - **What Changes**: Liste à puces des modifications (marquer les breaking changes avec **BREAKING**)
    - **Impact**: Specs affectées, code affecté, issues liées
    - **Next Steps**: Ce qui se passe après l'approbation

9. **Commit et Push**:
    ```bash
    git add openspec/changes/<change-id>/proposal.md
    git commit -m "[#<issue-number>] Add proposal for <change-id>"
    git push -u origin change/<change-id>
    ```

10. **Pull Request**: Créez un PR avec:
    - **Titre**: `[Proposal] <Description avec verbe>`
    - **Body**: Lien vers l'issue (`Relates to #<issue-number>`) et l'item du projet GitHub
    - **Labels**: `proposal`, `needs-review`
    - Utilisez le template PR si disponible

11. **Attendre approbation**: Ne pas démarrer l'implémentation avant que le PR de proposition soit approuvé et mergé.

**Format critique des scénarios**
Chaque exigence DOIT avoir au moins un scénario au format:
```markdown
#### Scenario: <Nom descriptif>
**Given**: Conditions initiales
**When**: Action ou déclencheur
**Then**: Résultat attendu
```

**Référence**
- Utilisez `openspec show <id> --json --deltas-only` ou `openspec show <spec> --type spec` pour examiner les détails en cas d'échec de la validation.
- Recherchez les exigences existantes avec `rg -n "Requirement:|Scenario:" openspec/specs` avant d'en rédiger de nouvelles.
- Explorez le code source avec `rg <mot-clé>`, `ls` ou en lisant directement les fichiers afin que les propositions correspondent aux réalités de l'implémentation actuelle.
- Vérifiez les issues et PRs existants sur GitHub pour éviter les doublons.
- Assurez-vous que GitHub Projects est activé et accessible.

**Aide-mémoire**
```
Issue (#) → Project (Design + Specs) → Validate → Branch →
Proposal (PR) → Review → Approve → Implement
```
<!-- OPENSPEC:END -->