# Règles d'Intake des Propositions Swarm DAO

> **Proposition** : DAO #22 — `governance-change`
> **Statut** : Codifiée — remplace les pratiques antérieures d'intake.
> **Références** : `AGENTS.md` (Workflow Swarm DAO, Gate Product Excellence)

Ce document codifie les règles obligatoires d'intake, de délibération et de
contrôle qualité des propositions Swarm DAO du projet Wakeve. Toute proposition
créée après l'adoption de DAO #22 doit s'y conformer.

---

## 1. Champs obligatoires avant délibération

Aucune proposition ne peut être soumise à `dao_deliberate` tant que les champs
structurés suivants ne sont pas renseignés et complets :

| Champ | Exigence |
|-------|----------|
| `problemStatement` | Description du problème réel à résoudre : contexte, objectif, impact sur les groupes privés qui préparent un événement. Un titre seul ne suffit pas. |
| `acceptanceCriteria` | **Au minimum 3 critères d'acceptation mesurables** (vérifiables par un test, une commande, une métrique ou une inspection). Un critère non vérifiable (« être plus simple », « améliorer l'UX ») est invalide. |
| `successMetrics` | Métriques de succès post-exécution, avec valeur cible ou direction mesurable. |
| `rollbackConditions` | Conditions explicites de rollback : situations dans lesquelles la proposition doit être annulée via `dao_rollback`. |

**Règle** : une proposition incomplète est renvoyée à son auteur pour
complément **avant** délibération. Elle ne doit pas consommer un cycle de
délibération du swarm.

---

## 2. Roundtable : de l'idée brute au brouillon structuré

Le `dao_roundtable` ne crée **plus de propositions brutes**. Les suggestions
émises en roundtable sont des **idées**, pas des propositions.

Flux obligatoire :

```
idée (roundtable ou humaine)
   → brouillon à champs structurés
      (problemStatement, ≥ 3 acceptanceCriteria mesurables,
       successMetrics, rollbackConditions)
   → dao_propose
   → cycle de vie standard (deliberate → dry-run → check → plan → execute → ship)
```

**Interdits** :
- Créer une proposition via `dao_propose` sans les quatre champs obligatoires
  (§1) — c'est le défaut qui a produit les doublons #1–#4, #6 et #7.
- Créer plusieurs propositions sur le même sujet à partir de suggestions de
  roundtable distinctes — elles doivent être **consolidées en une seule
  proposition** (voir registre §5).

---

## 3. Ordre impératif des gates

L'ordre des gates de contrôle est **impératif et non négociable** :

```
dao_deliberate  →  dao_dry_run  →  dao_check
```

### 3.1 Dry-run obligatoire en zone rouge

Pour toute proposition en **zone rouge** (risque élevé : risque ≥ 7/10 selon
la config DAO, paths critiques, breaking changes, sécurité), le **dry-run est
OBLIGATOIRE avant `dao_check`**.

**Pourquoi cet ordre** : un `dao_check` en échec verrouille la proposition en
statut **`failed` de façon terminale** — il n'existe pas de chemin de sortie.
Un échec détecté en dry-run, lui, est réversible : la proposition peut être
corrigée puis re-testée sans consommer son unique tentative de check.

**Leçons des propositions #5 et #11** : deux propositions verrouillées
`failed` de façon terminale après un `dao_check` passé trop tôt, sans dry-run
préalable. #5 (doublon roundtable en zone rouge) n'a pu être ni corrigée ni
consolidée ; elle a dû être remplacée par DAO #22.

**Règle pratique** : en cas de doute sur la zone de risque, exécuter le
dry-run. Le coût d'un dry-run est nul ; le coût d'un échec terminal est la
perte de la proposition.

---

## 4. Gate Product Excellence — critère de complétude des product-features

Pour toute proposition de type `product-feature`, le **Gate Product
Excellence** défini dans `AGENTS.md` fait partie intégrante des critères de
complétude. La proposition doit expliquer en quoi le changement :

- aide directement un groupe privé à préparer, décider, coordonner ou
  finaliser un événement ;
- réduit la charge mentale ou les allers-retours hors de Wakeve ;
- rend clair ce qui est confirmé, en attente, qui doit agir et la prochaine
  action utile ;
- reste rapide, compréhensible et utilisable sur mobile ;
- évite la dérive vers un réseau social, un chat générique, un gestionnaire
  de tâches, un calendrier ou un workspace générique.

Une product-feature qui ne satisfait pas ce gate doit être **rejetée,
différée ou rescopée avant implémentation** — le check de complétude ne peut
pas passer.

---

## 5. Registre de consolidation

Le premier passage de roundtable avait produit 7 propositions brutes
redondantes sur le même thème (resserrer les gates qualité). Leur sort :

| Proposition | Type | Origine | Résolution |
|-------------|------|---------|------------|
| #1 | product-feature | Doublon roundtable (Product Strategist) | Consolidée dans #22, puis **closes (shipped)** |
| #2 | product-feature | Doublon roundtable (Research Agent) | Consolidée dans #22, puis **closes (shipped)** |
| #3 | technical-change | Doublon roundtable (Solution Architect) | Consolidée dans #22, puis **closes (shipped)** |
| #4 | security-change | Doublon roundtable (Critic / Risk Agent) | Consolidée dans #22, puis **closes (shipped)** |
| #5 | governance-change | Doublon roundtable (Prioritization Agent) | Verrouillée **failed** (check terminal, cf. §3.1) — **remplacée par #22** |
| #6 | product-feature | Doublon roundtable (Spec Writer) | Consolidée dans #22, puis **closes (shipped)** |
| #7 | release-change | Doublon roundtable (Delivery Agent) | Consolidée dans #22, puis **closes (shipped)** |
| **#22** | governance-change | **Proposition de consolidation** | Remplace #5, absorbe le contenu de #1–#4, #6 et #7 |

**Règle** : en cas de doublon, la consolidation se fait dans une proposition
unique ; les doublons sont clos après consolidation. Une proposition
verrouillée `failed` ne peut pas être « ressuscitée » — elle est remplacée
par une nouvelle proposition qui reprend son contenu corrigé.

---

## 6. Escalade humaine

Si une proposition est bloquée par une gate de façon **injustifiée** —
l'échec ne reflète pas un problème réel de la proposition (faux positif de
check, verdict de délibération incohérent avec les critères, gate bloquant un
contenu conforme à ce document) :

1. **Ne pas forcer** : ne pas contourner la gate ni multiplier les
   propositions clones pour « re-tenter la chance ».
2. **Documenter** : consigner le blocage (lien audit via `dao_audit`,
   verdict de la gate, argumentaire expliquant pourquoi le blocage est
   injustifié).
3. **Escalader** : soumettre le dossier à une décision humaine (décision
   hors cycle automatique). L'humain peut arbitrer : déblocage, amendement
   de la proposition, ou confirmation du blocage.
4. **Tracer** : la décision humaine et ses motifs sont archivés avec la
   proposition (artefacts et audit DAO).

L'escalade humaine est le seul chemin de résolution d'un blocage injustifié
qui ne nécessite pas de recréer la proposition — et il doit rester
exceptionnel : sa fréquence est un signal de défaut dans les gates
eux-mêmes, à traiter via une nouvelle proposition `governance-change`.

---

*Document créé dans le cadre de la proposition Swarm DAO #22
(`docs(governance): codify proposal intake rules (DAO #22)`).*
