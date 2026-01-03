package com.guyghost.wakeve.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyghost.wakeve.chat.ChatMessage
import com.guyghost.wakeve.di.getImagePickerService
import com.guyghost.wakeve.image.ImagePickerService
import com.guyghost.wakeve.image.ImagePickerService as ImagePicker
import com.guyghost.wakeve.ui.components.ActionBubble
import com.guyghost.wakeve.ui.components.ConnectionStatusBanner
import com.guyghost.wakeve.ui.components.MessageBubble
import com.guyghost.wakeve.ui.components.MessageInputBar
import com.guyghost.wakeve.ui.components.TypingIndicatorRow
import com.guyghost.wakeve.viewmodel.ChatViewModel
import com.guyghost.wakeve.viewmodel.ConnectionStatus
import kotlinx.coroutines.launch

/**
 * ChatScreen - Main screen for real-time messaging in an event.
 * 
 * Features:
 * - Real-time message display with bubbles
 * - Typing indicators
 * - Emoji reactions (add/remove)
 * - Threaded conversations with indentation
 * - Section-based message organization
 * - Offline support with connection status
 * - Action bubbles for creating scenarios
 * - Image sending from gallery
 * 
 * ## Usage
 * 
 * ```kotlin
 * @Composable
 * fun EventChatRoute(
 *     eventId: String,
 *     viewModel: ChatViewModel = viewModel()
 * ) {
 *     LaunchedEffect(eventId) {
 *         viewModel.connectToChat(eventId, "wss://api.wakeve.com/ws")
 *         viewModel.setCurrentUser("user-123", "Jean Dupont")
 *     }
 *     
 *     ChatScreen(
 *         eventId = eventId,
 *         eventTitle = "Week-end ski",
 *         viewModel = view
 *     )
 * }
 * ```
 * 
 * @param eventId The event ID this chat is associated with
 * @param eventTitle The title of the event (shown in header)
 * @param viewModel The ChatViewModel instance
 * @param modifier Modifier for the component
 * @param onNavigateBack Callback when back navigation is triggered
 * @param onOpenSettings Callback when settings is tapped
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    eventId: String,
    eventTitle: String,
    viewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    // Collect state from ViewModel
    val messages by viewModel.messages.collectAsState()
    val typingUsers by viewModel.typingUsers.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedSection by viewModel.selectedSection.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Image picker service
    val imagePicker = remember { getImagePickerService() }
    
    // Show connection status changes in snackbar
    LaunchedEffect(connectionStatus) {
        when (val status = connectionStatus) {
            is ConnectionStatus.Disconnected -> {
                snackbarHostState.showSnackbar("Vous êtes hors ligne")
            }
            is ConnectionStatus.Queued -> {
                snackbarHostState.showSnackbar("${status.count} message(s) en attente d'envoi")
            }
            is ConnectionStatus.Syncing -> {
                // Don't show for syncing
            }
            else -> {}
        }
    }
    
    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            ChatTopBar(
                eventTitle = eventTitle,
                participantCount = 0, // TODO: Get from participants flow
                onNavigateBack = onNavigateBack,
                onOpenSettings = onOpenSettings
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Connection status banner
            ConnectionStatusBanner(isConnected = isConnected)
            
            // Typing indicators
            TypingIndicatorRow(typingUsers = typingUsers)
            
            // Messages list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty() && !isConnected) {
                    // Empty state when offline and no messages
                    EmptyChatState(
                        isOffline = !isConnected,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    MessagesList(
                        messages = messages,
                        currentUserId = currentUser?.userId ?: "",
                        listState = listState,
                        onReactionClick = { messageId, emoji ->
                            viewModel.toggleReaction(messageId, emoji)
                        },
                        onRemoveReaction = { messageId, emoji ->
                            viewModel.removeReaction(messageId, emoji)
                        },
                        onReplyClick = { messageId ->
                            // TODO: Open reply flow
                        },
                        onMessageVisible = { messageId ->
                            viewModel.markAsRead(messageId)
                        }
                    )
                }
            }
            
            // Action bubbles for creating scenarios (when chat is empty)
            AnimatedVisibility(
                visible = messages.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ActionBubblesRow(
                    onCreateScenario = { /* TODO: Navigate to scenario creation */ },
                    onQuickPoll = { /* TODO: Open quick poll */ },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Message input bar
            MessageInputBar(
                onSendMessage = { content ->
                    viewModel.sendMessage(
                        content = content,
                        section = selectedSection
                    )
                },
                onTypingStart = { viewModel.startTyping() },
                onTypingStop = { viewModel.stopTyping() },
                selectedSection = selectedSection,
                onSectionChange = { viewModel.setSectionFilter(it) },
                onImageSelected = {
                    scope.launch {
                        val result = imagePicker.pickImage()
                        result.fold(
                            onSuccess = { image ->
                                viewModel.sendImageMessage(
                                    image = image,
                                    section = selectedSection
                                )
                                snackbarHostState.showSnackbar("Image ajouté à la discussion")
                            },
                            onFailure = { error ->
                                val message = when (error) {
                                    is ImagePickerService.ImagePickerCancelledException -> "Sélection d'image annulée"
                                    is ImagePickerService.ImagePickerPermissionDeniedException -> "Permission refusée"
                                    else -> "Erreur: ${error.message}"
                                }
                                snackbarHostState.showSnackbar(message)
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Top app bar for the chat screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    eventTitle: String,
    participantCount: Int,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column {
                    Text(
                        text = eventTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (participantCount > 0) {
                        Text(
                            text = "$participantCount participant${if (participantCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour"
                )
            }
        },
        actions = {
            IconButton(onClick = { /* TODO: Voice call */ }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Comment,
                    contentDescription = "Appel vocal"
                )
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Plus d'options"
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Participants") },
                        onClick = {
                            showMenu = false
                            // TODO: Navigate to participants
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Médias partagés") },
                        onClick = {
                            showMenu = false
                            // TODO: Navigate to media
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Paramètres") },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onOpenSettings()
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * Scrollable list of messages.
 */
@Composable
private fun MessagesList(
    messages: List<ChatMessage>,
    currentUserId: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onReactionClick: (String, String) -> Unit,
    onRemoveReaction: (String, String) -> Unit,
    onReplyClick: (String) -> Unit,
    onMessageVisible: (String) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            items = messages,
            key = { it.id }
        ) { message ->
            MessageBubble(
                message = message,
                isCurrentUser = message.senderId == currentUserId,
                onReactionClick = { emoji -> onReactionClick(message.id, emoji) },
                onRemoveReaction = { userId, emoji -> onRemoveReaction(message.id, emoji) },
                onReplyClick = { onReplyClick(message.id) }
            )
            
            // Mark message as visible when it appears
            LaunchedEffect(message.id) {
                onMessageVisible(message.id)
            }
        }
    }
}

/**
 * Row of action bubbles for quick actions.
 */
@Composable
private fun ActionBubblesRow(
    onCreateScenario: () -> Unit,
    onQuickPoll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionBubble(
            label = "Créer un scénario",
            emoji = "📋",
            onClick = onCreateScenario,
            modifier = Modifier.weight(1f)
        )
        ActionBubble(
            label = "Sondage rapide",
            emoji = "📊",
            onClick = onQuickPoll,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Empty state for the chat.
 */
@Composable
private fun EmptyChatState(
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isOffline) "💬" else "👋",
            style = MaterialTheme.typography.displayLarge
        )
        Text(
            text = if (isOffline) {
                "Vous êtes hors ligne.\nLes messages seront synchronisés automatiquement."
            } else {
                "Aucun message pour le moment.\nLancez la discussion !"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Loading state for the chat.
 */
@Composable
fun ChatLoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Chargement de la discussion...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Error state for the chat.
 */
@Composable
fun ChatErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "😕",
            style = MaterialTheme.typography.displayLarge
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            onClick = onRetry,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Text(
                text = "Réessayer",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}
