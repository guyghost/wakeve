package com.guyghost.wakeve.i18n

/**
 * Server-side localization system for notification messages and other server-generated text.
 *
 * Supports 5 languages: EN, FR, ES, IT, PT.
 * Uses a key-based translation map with parameterized string formatting.
 *
 * Usage:
 * ```
 * ServerLocalizer.t("notification.vote.title_single", "fr") // "Nouveau vote"
 * ServerLocalizer.t("notification.vote.body_single", "en", "Alice", "Birthday Party") // "Alice voted on \"Birthday Party\""
 * ```
 */
object ServerLocalizer {

    private val translations: Map<String, Map<String, String>> = buildMap {
        // ==================== NOTIFICATION: VOTE ====================

        put("notification.vote.title_single", mapOf(
            "en" to "New vote",
            "fr" to "Nouveau vote",
            "es" to "Nuevo voto",
            "it" to "Nuovo voto",
            "pt" to "Novo voto"
        ))

        put("notification.vote.title_multiple", mapOf(
            "en" to "New votes",
            "fr" to "Nouveaux votes",
            "es" to "Nuevos votos",
            "it" to "Nuovi voti",
            "pt" to "Novos votos"
        ))

        put("notification.vote.body_single", mapOf(
            "en" to "%s voted on \"%s\"",
            "fr" to "%s a voté pour \"%s\"",
            "es" to "%s ha votado en \"%s\"",
            "it" to "%s ha votato per \"%s\"",
            "pt" to "%s votou em \"%s\""
        ))

        put("notification.vote.body_multiple", mapOf(
            "en" to "%s people voted on \"%s\"",
            "fr" to "%s personnes ont voté pour \"%s\"",
            "es" to "%s personas han votado en \"%s\"",
            "it" to "%s persone hanno votato per \"%s\"",
            "pt" to "%s pessoas votaram em \"%s\""
        ))

        put("notification.vote.default_voter", mapOf(
            "en" to "A participant",
            "fr" to "Un participant",
            "es" to "Un participante",
            "it" to "Un partecipante",
            "pt" to "Um participante"
        ))

        // ==================== NOTIFICATION: STATUS CHANGES ====================

        put("notification.status.confirmed_title", mapOf(
            "en" to "Date confirmed!",
            "fr" to "Date confirmée !",
            "es" to "¡Fecha confirmada!",
            "it" to "Data confermata!",
            "pt" to "Data confirmada!"
        ))

        put("notification.status.confirmed_body", mapOf(
            "en" to "The date for \"%s\" is confirmed",
            "fr" to "La date de \"%s\" est confirmée",
            "es" to "La fecha de \"%s\" está confirmada",
            "it" to "La data di \"%s\" è confermata",
            "pt" to "A data de \"%s\" está confirmada"
        ))

        put("notification.status.confirmed_body_with_date", mapOf(
            "en" to "The date for \"%s\" is confirmed: %s",
            "fr" to "La date de \"%s\" est confirmée : %s",
            "es" to "La fecha de \"%s\" está confirmada: %s",
            "it" to "La data di \"%s\" è confermata: %s",
            "pt" to "A data de \"%s\" está confirmada: %s"
        ))

        put("notification.status.polling_title", mapOf(
            "en" to "Vote now!",
            "fr" to "Votez maintenant !",
            "es" to "¡Vota ahora!",
            "it" to "Vota adesso!",
            "pt" to "Vote agora!"
        ))

        put("notification.status.polling_body", mapOf(
            "en" to "The poll for \"%s\" is open. Vote for your preferred dates.",
            "fr" to "Le sondage pour \"%s\" est ouvert. Votez pour vos dates préférées.",
            "es" to "La encuesta de \"%s\" está abierta. Vota por tus fechas preferidas.",
            "it" to "Il sondaggio per \"%s\" è aperto. Vota le tue date preferite.",
            "pt" to "A votação de \"%s\" está aberta. Vote nas suas datas preferidas."
        ))

        put("notification.status.cancelled_title", mapOf(
            "en" to "Event cancelled",
            "fr" to "Événement annulé",
            "es" to "Evento cancelado",
            "it" to "Evento annullato",
            "pt" to "Evento cancelado"
        ))

        put("notification.status.cancelled_body", mapOf(
            "en" to "\"%s\" has been cancelled by the organizer.",
            "fr" to "\"%s\" a été annulé par l'organisateur.",
            "es" to "\"%s\" ha sido cancelado por el organizador.",
            "it" to "\"%s\" è stato annullato dall'organizzatore.",
            "pt" to "\"%s\" foi cancelado pelo organizador."
        ))

        put("notification.status.updated_title", mapOf(
            "en" to "Update",
            "fr" to "Mise à jour",
            "es" to "Actualización",
            "it" to "Aggiornamento",
            "pt" to "Atualização"
        ))

        put("notification.status.updated_body", mapOf(
            "en" to "\"%s\" has been updated.",
            "fr" to "\"%s\" a été mis à jour.",
            "es" to "\"%s\" ha sido actualizado.",
            "it" to "\"%s\" è stato aggiornato.",
            "pt" to "\"%s\" foi atualizado."
        ))

        // ==================== NOTIFICATION: COMMENTS ====================

        put("notification.comment.title", mapOf(
            "en" to "%s commented",
            "fr" to "%s a commenté",
            "es" to "%s ha comentado",
            "it" to "%s ha commentato",
            "pt" to "%s comentou"
        ))

        put("notification.comment.body", mapOf(
            "en" to "\"%s\": %s",
            "fr" to "\"%s\" : %s",
            "es" to "\"%s\": %s",
            "it" to "\"%s\": %s",
            "pt" to "\"%s\": %s"
        ))

        // ==================== NOTIFICATION: DEADLINE ====================

        put("notification.deadline.title", mapOf(
            "en" to "Deadline approaching",
            "fr" to "Deadline approche",
            "es" to "Fecha límite cerca",
            "it" to "Scadenza in arrivo",
            "pt" to "Prazo se aproxima"
        ))

        put("notification.deadline.body", mapOf(
            "en" to "%s left to vote on \"%s\"",
            "fr" to "Il reste %s pour voter sur \"%s\"",
            "es" to "Quedan %s para votar en \"%s\"",
            "it" to "Restano %s per votare su \"%s\"",
            "pt" to "Restam %s para votar em \"%s\""
        ))

        put("notification.deadline.time_less_than_hour", mapOf(
            "en" to "less than an hour",
            "fr" to "moins d'une heure",
            "es" to "menos de una hora",
            "it" to "meno di un'ora",
            "pt" to "menos de uma hora"
        ))

        put("notification.deadline.time_hours", mapOf(
            "en" to "%s hours",
            "fr" to "%s heures",
            "es" to "%s horas",
            "it" to "%s ore",
            "pt" to "%s horas"
        ))

        put("notification.deadline.time_days", mapOf(
            "en" to "%s day(s)",
            "fr" to "%s jour(s)",
            "es" to "%s día(s)",
            "it" to "%s giorno/i",
            "pt" to "%s dia(s)"
        ))

        // ==================== NOTIFICATION: EVENT DAY ====================

        put("notification.event_day.title", mapOf(
            "en" to "It's today!",
            "fr" to "C'est aujourd'hui !",
            "es" to "¡Es hoy!",
            "it" to "È oggi!",
            "pt" to "É hoje!"
        ))

        put("notification.event_day.body", mapOf(
            "en" to "\"%s\" is happening today. Have a great day!",
            "fr" to "\"%s\" a lieu aujourd'hui. Bonne journée !",
            "es" to "\"%s\" es hoy. ¡Que tengas un buen día!",
            "it" to "\"%s\" è oggi. Buona giornata!",
            "pt" to "\"%s\" é hoje. Tenha um bom dia!"
        ))

        // ==================== NOTIFICATION: WEEKLY DIGEST ====================

        put("notification.digest.title", mapOf(
            "en" to "Weekly summary",
            "fr" to "Résumé de la semaine",
            "es" to "Resumen semanal",
            "it" to "Riepilogo settimanale",
            "pt" to "Resumo semanal"
        ))

        put("notification.digest.unread_single", mapOf(
            "en" to "You have 1 unread notification. ",
            "fr" to "Vous avez 1 notification non lue. ",
            "es" to "Tienes 1 notificación no leída. ",
            "it" to "Hai 1 notifica non letta. ",
            "pt" to "Tem 1 notificação não lida. "
        ))

        put("notification.digest.unread_multiple", mapOf(
            "en" to "You have %s unread notifications. ",
            "fr" to "Vous avez %s notifications non lues. ",
            "es" to "Tienes %s notificaciones no leídas. ",
            "it" to "Hai %s notifiche non lette. ",
            "pt" to "Tem %s notificações não lidas. "
        ))

        put("notification.digest.votes", mapOf(
            "en" to "%s vote(s)",
            "fr" to "%s vote(s)",
            "es" to "%s voto(s)",
            "it" to "%s voto/i",
            "pt" to "%s voto(s)"
        ))

        put("notification.digest.comments", mapOf(
            "en" to "%s comment(s)",
            "fr" to "%s commentaire(s)",
            "es" to "%s comentario(s)",
            "it" to "%s commento/i",
            "pt" to "%s comentário(s)"
        ))

        put("notification.digest.updates", mapOf(
            "en" to "%s update(s)",
            "fr" to "%s mise(s) à jour",
            "es" to "%s actualización(es)",
            "it" to "%s aggiornamento/i",
            "pt" to "%s atualização(ões)"
        ))

        put("notification.digest.reminders", mapOf(
            "en" to "%s reminder(s)",
            "fr" to "%s rappel(s)",
            "es" to "%s recordatorio(s)",
            "it" to "%s promemoria",
            "pt" to "%s lembrete(s)"
        ))

        put("notification.digest.notifications", mapOf(
            "en" to "%s notification(s)",
            "fr" to "%s notification(s)",
            "es" to "%s notificación(es)",
            "it" to "%s notifica/che",
            "pt" to "%s notificação(ões)"
        ))

        // ==================== LEVELS ====================

        put("level.beginner", mapOf(
            "en" to "Beginner",
            "fr" to "Débutant",
            "es" to "Principiante",
            "it" to "Principiante",
            "pt" to "Iniciante"
        ))

        put("level.explorer", mapOf(
            "en" to "Explorer",
            "fr" to "Explorateur",
            "es" to "Explorador",
            "it" to "Esploratore",
            "pt" to "Explorador"
        ))

        put("level.contributor", mapOf(
            "en" to "Contributor",
            "fr" to "Contributeur",
            "es" to "Contribuidor",
            "it" to "Collaboratore",
            "pt" to "Contribuidor"
        ))

        put("level.organizer", mapOf(
            "en" to "Organizer",
            "fr" to "Organisateur",
            "es" to "Organizador",
            "it" to "Organizzatore",
            "pt" to "Organizador"
        ))

        put("level.expert", mapOf(
            "en" to "Expert",
            "fr" to "Expert",
            "es" to "Experto",
            "it" to "Esperto",
            "pt" to "Especialista"
        ))

        put("level.master", mapOf(
            "en" to "Master",
            "fr" to "Maître",
            "es" to "Maestro",
            "it" to "Maestro",
            "pt" to "Mestre"
        ))

        put("level.champion", mapOf(
            "en" to "Champion",
            "fr" to "Champion",
            "es" to "Campeón",
            "it" to "Campione",
            "pt" to "Campeão"
        ))

        put("level.legend", mapOf(
            "en" to "Legend",
            "fr" to "Légende",
            "es" to "Leyenda",
            "it" to "Leggenda",
            "pt" to "Lenda"
        ))

        put("level.mythic", mapOf(
            "en" to "Mythic",
            "fr" to "Mythique",
            "es" to "Mítico",
            "it" to "Mitico",
            "pt" to "Mítico"
        ))

        put("level.transcendent", mapOf(
            "en" to "Transcendent",
            "fr" to "Transcendant",
            "es" to "Trascendente",
            "it" to "Trascendente",
            "pt" to "Transcendente"
        ))

        // ==================== CATEGORIES ====================

        put("category.all", mapOf(
            "en" to "All",
            "fr" to "Tout",
            "es" to "Todo",
            "it" to "Tutto",
            "pt" to "Tudo"
        ))

        put("category.social", mapOf(
            "en" to "Social",
            "fr" to "Social",
            "es" to "Social",
            "it" to "Sociale",
            "pt" to "Social"
        ))

        put("category.sport", mapOf(
            "en" to "Sport",
            "fr" to "Sport",
            "es" to "Deporte",
            "it" to "Sport",
            "pt" to "Esporte"
        ))

        put("category.culture", mapOf(
            "en" to "Culture",
            "fr" to "Culture",
            "es" to "Cultura",
            "it" to "Cultura",
            "pt" to "Cultura"
        ))

        put("category.professional", mapOf(
            "en" to "Pro",
            "fr" to "Pro",
            "es" to "Pro",
            "it" to "Pro",
            "pt" to "Pro"
        ))

        put("category.food", mapOf(
            "en" to "Food",
            "fr" to "Food",
            "es" to "Comida",
            "it" to "Cibo",
            "pt" to "Comida"
        ))

        put("category.wellness", mapOf(
            "en" to "Wellness",
            "fr" to "Bien-être",
            "es" to "Bienestar",
            "it" to "Benessere",
            "pt" to "Bem-estar"
        ))

        // ==================== BADGES ====================

        put("badge.first_event.name", mapOf(
            "en" to "First Event",
            "fr" to "Premier événement",
            "es" to "Primer evento",
            "it" to "Primo evento",
            "pt" to "Primeiro evento"
        ))
        put("badge.first_event.description", mapOf(
            "en" to "Created your first event",
            "fr" to "A créé votre premier événement",
            "es" to "Creó su primer evento",
            "it" to "Ha creato il primo evento",
            "pt" to "Criou o primeiro evento"
        ))

        put("badge.dedicated.name", mapOf(
            "en" to "Dedicated Organizer",
            "fr" to "Organisateur Dévoué",
            "es" to "Organizador Dedicado",
            "it" to "Organizzatore Dedicato",
            "pt" to "Organizador Dedicado"
        ))
        put("badge.dedicated.description", mapOf(
            "en" to "Created 5 events",
            "fr" to "A créé 5 événements",
            "es" to "Creó 5 eventos",
            "it" to "Ha creato 5 eventi",
            "pt" to "Criou 5 eventos"
        ))

        put("badge.super_organizer.name", mapOf(
            "en" to "Super Organizer",
            "fr" to "Super Organisateur",
            "es" to "Súper Organizador",
            "it" to "Super Organizzatore",
            "pt" to "Super Organizador"
        ))
        put("badge.super_organizer.description", mapOf(
            "en" to "Created 10 events",
            "fr" to "A créé 10 événements",
            "es" to "Creó 10 eventos",
            "it" to "Ha creato 10 eventi",
            "pt" to "Criou 10 eventos"
        ))

        put("badge.event_master.name", mapOf(
            "en" to "Event Master",
            "fr" to "Event Master",
            "es" to "Event Master",
            "it" to "Event Master",
            "pt" to "Event Master"
        ))
        put("badge.event_master.description", mapOf(
            "en" to "Created 5 events in one month",
            "fr" to "A créé 5 événements en un mois",
            "es" to "Creó 5 eventos en un mes",
            "it" to "Ha creato 5 eventi in un mese",
            "pt" to "Criou 5 eventos em um mês"
        ))

        put("badge.industry.name", mapOf(
            "en" to "Event Producer",
            "fr" to "Producteur d'Événements",
            "es" to "Productor de Eventos",
            "it" to "Produttore di Eventi",
            "pt" to "Produtor de Eventos"
        ))
        put("badge.industry.description", mapOf(
            "en" to "Created 25 events",
            "fr" to "A créé 25 événements",
            "es" to "Creó 25 eventos",
            "it" to "Ha creato 25 eventi",
            "pt" to "Criou 25 eventos"
        ))

        put("badge.first_vote.name", mapOf(
            "en" to "First Vote",
            "fr" to "Premier Vote",
            "es" to "Primer Voto",
            "it" to "Primo Voto",
            "pt" to "Primeiro Voto"
        ))
        put("badge.first_vote.description", mapOf(
            "en" to "Participated in your first vote",
            "fr" to "A participé à son premier vote",
            "es" to "Participó en su primer voto",
            "it" to "Ha partecipato al primo voto",
            "pt" to "Participou na primeira votação"
        ))

        put("badge.early_bird.name", mapOf(
            "en" to "Early Bird",
            "fr" to "Early Bird",
            "es" to "Early Bird",
            "it" to "Early Bird",
            "pt" to "Early Bird"
        ))
        put("badge.early_bird.description", mapOf(
            "en" to "Voted within 24h on 10 polls",
            "fr" to "A voté dans les 24h sur 10 sondages",
            "es" to "Votó en 24h en 10 encuestas",
            "it" to "Ha votato entro 24h su 10 sondaggi",
            "pt" to "Votou em 24h em 10 enquetes"
        ))

        put("badge.decision_maker.name", mapOf(
            "en" to "Decision Maker",
            "fr" to "Decision Maker",
            "es" to "Decision Maker",
            "it" to "Decision Maker",
            "pt" to "Decision Maker"
        ))
        put("badge.decision_maker.description", mapOf(
            "en" to "Voted on 50 polls",
            "fr" to "A voté sur 50 sondages",
            "es" to "Votó en 50 encuestas",
            "it" to "Ha votato su 50 sondaggi",
            "pt" to "Votou em 50 enquetes"
        ))

        put("badge.quick_responder.name", mapOf(
            "en" to "Quick Responder",
            "fr" to "Réponse Rapide",
            "es" to "Respuesta Rápida",
            "it" to "Risposta Rapida",
            "pt" to "Resposta Rápida"
        ))
        put("badge.quick_responder.description", mapOf(
            "en" to "Responded to 5 votes in less than 2 hours",
            "fr" to "A répondu à 5 votes en moins de 2 heures",
            "es" to "Respondió a 5 votos en menos de 2 horas",
            "it" to "Ha risposto a 5 voti in meno di 2 ore",
            "pt" to "Respondeu a 5 votos em menos de 2 horas"
        ))

        put("badge.voting_champion.name", mapOf(
            "en" to "Voting Champion",
            "fr" to "Champion du Vote",
            "es" to "Campeón de Votación",
            "it" to "Campione di Voto",
            "pt" to "Campeão de Votação"
        ))
        put("badge.voting_champion.description", mapOf(
            "en" to "Participated in 100 votes",
            "fr" to "A participé à 100 votes",
            "es" to "Participó en 100 votos",
            "it" to "Ha partecipato a 100 voti",
            "pt" to "Participou em 100 votos"
        ))

        put("badge.first_steps.name", mapOf(
            "en" to "First Steps",
            "fr" to "Premiers Pas",
            "es" to "Primeros Pasos",
            "it" to "Primi Passi",
            "pt" to "Primeiros Passos"
        ))
        put("badge.first_steps.description", mapOf(
            "en" to "Participated in your first event",
            "fr" to "A participé à son premier événement",
            "es" to "Participó en su primer evento",
            "it" to "Ha partecipato al primo evento",
            "pt" to "Participou no primeiro evento"
        ))

        put("badge.team_player.name", mapOf(
            "en" to "Team Player",
            "fr" to "Team Player",
            "es" to "Team Player",
            "it" to "Team Player",
            "pt" to "Team Player"
        ))
        put("badge.team_player.description", mapOf(
            "en" to "Participated in 20 events",
            "fr" to "A participé à 20 événements",
            "es" to "Participó en 20 eventos",
            "it" to "Ha partecipato a 20 eventi",
            "pt" to "Participou em 20 eventos"
        ))

        put("badge.social_butterfly.name", mapOf(
            "en" to "Social Butterfly",
            "fr" to "Social Butterfly",
            "es" to "Social Butterfly",
            "it" to "Social Butterfly",
            "pt" to "Social Butterfly"
        ))
        put("badge.social_butterfly.description", mapOf(
            "en" to "Participated in 10 events",
            "fr" to "A participé à 10 événements",
            "es" to "Participó en 10 eventos",
            "it" to "Ha partecipato a 10 eventi",
            "pt" to "Participou em 10 eventos"
        ))

        put("badge.party_animal.name", mapOf(
            "en" to "Party Animal",
            "fr" to "Fêteur",
            "es" to "Fiestero",
            "it" to "Festaiolo",
            "pt" to "Festeiro"
        ))
        put("badge.party_animal.description", mapOf(
            "en" to "Participated in 25 events",
            "fr" to "A participé à 25 événements",
            "es" to "Participó en 25 eventos",
            "it" to "Ha partecipato a 25 eventi",
            "pt" to "Participou em 25 eventos"
        ))

        put("badge.festival_goer.name", mapOf(
            "en" to "Festival Goer",
            "fr" to "Festival Goer",
            "es" to "Festival Goer",
            "it" to "Festival Goer",
            "pt" to "Festival Goer"
        ))
        put("badge.festival_goer.description", mapOf(
            "en" to "Attended 5 events on the same day",
            "fr" to "A assisté à 5 événements sur une même journée",
            "es" to "Asistió a 5 eventos en el mismo día",
            "it" to "Ha partecipato a 5 eventi nella stessa giornata",
            "pt" to "Participou de 5 eventos no mesmo dia"
        ))

        put("badge.chatty.name", mapOf(
            "en" to "Chatty",
            "fr" to "Bavard",
            "es" to "Parlanchín",
            "it" to "Chiacchierone",
            "pt" to "Tagarela"
        ))
        put("badge.chatty.description", mapOf(
            "en" to "Commented 10 times",
            "fr" to "A commenté 10 fois",
            "es" to "Comentó 10 veces",
            "it" to "Ha commentato 10 volte",
            "pt" to "Comentou 10 vezes"
        ))

        put("badge.conversationalist.name", mapOf(
            "en" to "Conversationalist",
            "fr" to "Conversationalist",
            "es" to "Conversationalist",
            "it" to "Conversationalist",
            "pt" to "Conversationalist"
        ))
        put("badge.conversationalist.description", mapOf(
            "en" to "Commented on 30 events",
            "fr" to "A commenté sur 30 événements",
            "es" to "Comentó en 30 eventos",
            "it" to "Ha commentato su 30 eventi",
            "pt" to "Comentou em 30 eventos"
        ))

        put("badge.opinionated.name", mapOf(
            "en" to "Opinionated",
            "fr" to "Avisé",
            "es" to "Experto",
            "it" to "Esperto",
            "pt" to "Opinador"
        ))
        put("badge.opinionated.description", mapOf(
            "en" to "Voted on 20 scenarios",
            "fr" to "A voté sur 20 scénarios",
            "es" to "Votó en 20 escenarios",
            "it" to "Ha votato su 20 scenari",
            "pt" to "Votou em 20 cenários"
        ))

        put("badge.scenario_creator.name", mapOf(
            "en" to "Scenario Creator",
            "fr" to "Créateur de Scénarios",
            "es" to "Creador de Escenarios",
            "it" to "Creatore di Scenari",
            "pt" to "Criador de Cenários"
        ))
        put("badge.scenario_creator.description", mapOf(
            "en" to "Created 5 scenarios",
            "fr" to "A créé 5 scénarios",
            "es" to "Creó 5 escenarios",
            "it" to "Ha creato 5 scenari",
            "pt" to "Criou 5 cenários"
        ))

        put("badge.legend.name", mapOf(
            "en" to "Event Legend",
            "fr" to "Event Legend",
            "es" to "Event Legend",
            "it" to "Event Legend",
            "pt" to "Event Legend"
        ))
        put("badge.legend.description", mapOf(
            "en" to "Reached 10,000 points",
            "fr" to "A atteint 10 000 points",
            "es" to "Alcanzó 10.000 puntos",
            "it" to "Ha raggiunto 10.000 punti",
            "pt" to "Alcançou 10.000 pontos"
        ))

        put("badge.century_club.name", mapOf(
            "en" to "Century Club",
            "fr" to "Club des Cent",
            "es" to "Club de los Cien",
            "it" to "Club dei Cento",
            "pt" to "Clube dos Cem"
        ))
        put("badge.century_club.description", mapOf(
            "en" to "Reached 100 total points",
            "fr" to "A atteint 100 points totaux",
            "es" to "Alcanzó 100 puntos totales",
            "it" to "Ha raggiunto 100 punti totali",
            "pt" to "Alcançou 100 pontos totais"
        ))

        put("badge.millenium_club.name", mapOf(
            "en" to "Millennium Club",
            "fr" to "Club des Mille",
            "es" to "Club de los Mil",
            "it" to "Club dei Mille",
            "pt" to "Clube dos Mil"
        ))
        put("badge.millenium_club.description", mapOf(
            "en" to "Reached 1,000 total points",
            "fr" to "A atteint 1 000 points totaux",
            "es" to "Alcanzó 1.000 puntos totales",
            "it" to "Ha raggiunto 1.000 punti totali",
            "pt" to "Alcançou 1.000 pontos totais"
        ))
    }

    /**
     * Supported locale codes.
     */
    val supportedLocales = setOf("en", "fr", "es", "it", "pt")

    /**
     * Translates a key to the given locale with optional parameter substitution.
     *
     * @param key The translation key (e.g., "notification.vote.title_single")
     * @param locale The locale code ("en", "fr", "es", "it", "pt"). Defaults to "fr".
     * @param params Optional parameters to substitute %s placeholders in order.
     * @return The translated string, or the key itself if not found.
     */
    fun t(key: String, locale: String = "fr", vararg params: Any): String {
        val normalizedLocale = normalizeLocale(locale)
        val localeMap = translations[key] ?: return key
        val template = localeMap[normalizedLocale] ?: localeMap["fr"] ?: return key

        if (params.isEmpty()) return template

        var result = template
        for (param in params) {
            result = result.replaceFirst("%s", param.toString())
        }
        return result
    }

    /**
     * Normalizes a locale string to a supported 2-letter code.
     * Handles formats like "fr-FR", "en_US", "pt-BR", etc.
     */
    fun normalizeLocale(locale: String): String {
        val code = locale.lowercase().split("-", "_").firstOrNull() ?: "fr"
        return if (code in supportedLocales) code else "fr"
    }
}
