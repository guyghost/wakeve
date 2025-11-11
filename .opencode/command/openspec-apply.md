---
agent: build
description: Implement an approved OpenSpec change and keep tasks in sync.
---
<!-- OPENSPEC:START -->
**Recommandations**
- Privilégiez les implémentations simples et minimales et n'ajoutez de la complexité que lorsque cela est demandé ou clairement nécessaire.
- Veillez à ce que les modifications restent strictement limitées au résultat attendu.
- Consultez le fichier `openspec/AGENTS.md` (situé dans le répertoire `openspec/`; exécutez `ls openspec` ou `openspec update` si vous ne le voyez pas) pour obtenir des informations supplémentaires sur les conventions OpenSpec ou des clarifications.

**Étapes**
Suivez ces étapes comme des tâches à accomplir et effectuez-les une par une.
1. Lisez les fichiers `changes/<id>/proposal.md`, `design.md` (le cas échéant) et `tasks.md` pour confirmer la portée et les critères d'acceptation.
2. Traitez les tâches séquentiellement, en minimisant les modifications et en les concentrant sur le changement demandé.
3. Confirmez la réalisation des tâches avant de mettre à jour les statuts ; assurez-vous que chaque élément du fichier `tasks.md` est terminé. 4. Mettez à jour la liste de contrôle une fois le travail terminé afin que chaque tâche soit marquée `- [x]` et reflète la réalité.
5. Consultez `openspec list` ou `openspec show <item>` lorsque des informations supplémentaires sont nécessaires.

**Référence**
- Utilisez `openspec show <id> --json --deltas-only` si vous avez besoin d'informations supplémentaires issues de la proposition lors de la mise en œuvre.
<!-- OPENSPEC:END -->
