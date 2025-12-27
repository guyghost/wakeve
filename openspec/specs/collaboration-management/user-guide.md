# Guide d'Utilisation - Système de Commentaires Wakeve

## A. Introduction

### Qu'est-ce que le système de commentaires Wakeve?
Le système de commentaires Wakeve permet aux participants d'un événement de discuter, poser des questions et organiser les décisions directement dans le contexte de l'événement. Il prend en charge les sections (ex : Général, Scénarios, Budget, Logistique), les threads (réponses), les mentions, les réactions et les notifications.

### Comment accéder aux commentaires
- Sur Android (Compose) : bouton **Commentaires** (icône bulle) dans la barre d'action ou en bas des écrans de section.
- Sur iOS (SwiftUI) : icône **Commentaires** dans la barre de navigation ou bouton **Commentaires** dans la vue de détail.
- Sur Web : bouton **Commentaires** dans la barre supérieure ou section collante sur la droite.

> **Note**: Seuls les participants d'un événement peuvent accéder aux commentaires.

---

## B. Ajouter des commentaires

### Comment poster un commentaire
1. Ouvrez l'événement concerné
2. Appuyez sur le bouton **Commentaires**
3. Sélectionnez la section pertinente (Général, Scénarios, Budget...)
4. Appuyez sur **Nouveau commentaire** ou sur le champ de texte en bas
5. Rédigez votre message (format Markdown simple supporté)
6. Appuyez sur **Poster**

**Limites**:
- Maximum recommandé : 2000 caractères
- Pièces jointes : supportées via le champ `metadata.attachments` (voir administrateur)

### Où trouver le bouton de commentaires
- En-tête d'écran: icône bulle de dialogue
- Sections listées: bouton flottant **Commentaires** en bas
- Détails d'un item (ex: scénario): bouton commentaire contextualisé

### Sections disponibles
- Général
- Scénarios
- Budget
- Transport
- Hébergement
- Repas
- Équipements
- Activités
- Autres

---

## C. Répondre aux commentaires

1. Dans la liste, appuyez sur **Répondre** sur le commentaire ciblé
2. Le champ de saisie pré-remplit `@Auteur` pour référence
3. Saisissez votre réponse et appuyez sur **Poster**

### Notation des réponses
- Utilisez `@nom` pour mentionner un participant (autocomplete disponible)
- Les mentions déclenchent une notification pour l'utilisateur mentionné

### Indentation visuelle
- Les réponses sont indentées une fois (niveau 1) et s'affichent en cascade. Pour éviter des threads trop profonds (>5), l'application proposera de créer une nouvelle discussion.

---

## D. Modifier et supprimer

### Modifier ses propres commentaires
1. Ouvrez le menu (•••) sur votre commentaire
2. Choisissez **Modifier**
3. Mettez à jour le texte et appuyez sur **Enregistrer**

> **Note**: Le champ `isEdited` sera mis à jour et un badge "Édité" apparaîtra

### Supprimer un commentaire
1. Ouvrez le menu (•••)
2. Choisissez **Supprimer**
3. Confirmez la suppression

**Comportement**:
- Suppression soft (l'auteur ou un admin peut restaurer)
- Le contenu supprimé peut être remplacé par "Commentaire supprimé"

### Permissions
- Seul l'auteur, l'organisateur ou un admin peuvent modifier/supprimer

---

## E. Filtrer les commentaires

### Par section
- Dans la vue commentaires, utilisez le sélecteur de section pour n'afficher que les commentaires d'une section donnée

### Par type de contenu
- Filtrez par `hasAttachment`, `mentions`, `unread` (optionnel)

### Pagination et chargement
- Chargement initial : `limit=20`
- Scroll infini pour charger `offset += limit`
- Possibilité d'utiliser un tri : `Nouveaux`, `Plus répondus`, `Anciens`

---

## F. Notifications

### Comment activer les notifications
- Android : Paramètres de l'application > Notifications > Commentaires
- iOS : Réglages iOS > Notifications > Wakeve, et dans l'app > Notifications
- Web : Autoriser les notifications du navigateur

### Types de notifications reçues
- Nouvelle réponse à votre commentaire
- Nouvelle mention
- Nouvelle activité dans une section que vous suivez
- Réaction à votre commentaire

### Gestion des préférences
- Par événement : activer/désactiver notifications pour cet événement
- Par section : activer/désactiver notifications par section
- Digests : activer résumé quotidien (option future)

---

## G. FAQ

Q: Puis-je éditer le commentaire d'un autre participant ?
A: Non, seulement l'auteur ou un admin/organisateur peut le faire.

Q: Que se passe-t-il si je supprime un commentaire avec des réponses ?
A: Le commentaire est soft-deleted; les réponses restent visibles et le parent est marqué comme supprimé.

Q: Comment retrouver un commentaire que j'ai posté ?
A: Utilisez le filtre "Mes commentaires" ou recherchez par mot-clé.

Q: Les mentions génèrent-elles toujours une notification ?
A: Oui sauf si l'utilisateur a désactivé les notifications.

---

## H. Dépannage

- Problème: Mes commentaires n'apparaissent pas après publication
  - Vérifiez la connexion et l'état offline. Les commentaires créés hors-ligne sont synchronisés à la reconnexion.
  - Assurez-vous d'être membre de l'événement.

- Problème: Je ne reçois pas de notifications
  - Vérifiez les permissions de notifications système et les préférences dans l'app.

- Problème: Je ne peux pas modifier un commentaire
  - Vérifiez que vous êtes l'auteur et que le commentaire n'est pas verrouillé par l'organisateur.

---

*Fin du guide utilisateur*
