package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.AppContainer
import com.example.R
import com.example.ui.components.FuturisticBackdrop
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ChatViewModel
import com.example.ui.screens.CodingAssistantScreen
import com.example.ui.screens.CodingAssistantViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageGeneratorScreen
import com.example.ui.screens.ImageGeneratorViewModel
import com.example.ui.screens.PdfReaderScreen
import com.example.ui.screens.PdfReaderViewModel
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SettingsViewModel
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.VoiceScreen
import com.example.ui.screens.VoiceViewModel
import com.example.ui.screens.WebSearchScreen
import com.example.ui.screens.WebSearchViewModel
import com.example.ui.theme.LocalAiThemeTokens
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data class ChatRoute(val conversationId: Long? = null, val nonce: Long = 0L)
@Serializable data object VoiceRoute
@Serializable data object ToolsRoute
@Serializable data object SettingsRoute
@Serializable data object ProfileRoute
@Serializable data object ImageGeneratorRoute
@Serializable data object CodingAssistantRoute
@Serializable data object PdfReaderRoute
@Serializable data object WebSearchRoute

private data class TopLevelDestination(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun AiAssistantApp(container: AppContainer) {
    var showSplash by rememberSaveable { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val tokens = LocalAiThemeTokens.current

    val settingsVm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(
            container.settingsRepository,
            container.chatRepository
        )
    )
    val settingsState by settingsVm.uiState.collectAsStateWithLifecycle()
    val recentConversations by container.chatRepository.observeConversations()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val topDestinations = listOf(
        TopLevelDestination(
            HomeRoute,
            R.string.nav_home,
            Icons.Rounded.Home,
            Icons.Outlined.Home,
            "nav_home"
        ),
        TopLevelDestination(
            ChatRoute(),
            R.string.nav_chat,
            Icons.AutoMirrored.Rounded.Chat,
            Icons.AutoMirrored.Outlined.Chat,
            "nav_chat"
        ),
        TopLevelDestination(
            VoiceRoute,
            R.string.nav_voice,
            Icons.Rounded.GraphicEq,
            Icons.Outlined.GraphicEq,
            "nav_voice"
        ),
        TopLevelDestination(
            ToolsRoute,
            R.string.nav_tools,
            Icons.Rounded.Build,
            Icons.Outlined.Build,
            "nav_tools"
        ),
        TopLevelDestination(
            SettingsRoute,
            R.string.nav_settings,
            Icons.Rounded.Settings,
            Icons.Outlined.Settings,
            "nav_settings"
        )
    )

    val showBottomOrRailNav = currentDestination?.let { dest ->
        dest.hasRoute<HomeRoute>() ||
            dest.hasRoute<ChatRoute>() ||
            dest.hasRoute<VoiceRoute>() ||
            dest.hasRoute<ToolsRoute>() ||
            dest.hasRoute<SettingsRoute>()
    } ?: true

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        FuturisticBackdrop {
            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail && showBottomOrRailNav) {
                    NavigationRail(
                        containerColor = tokens.glassSurface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            val label = stringResource(id = dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = tokens.glowSecondary,
                                    selectedTextColor = tokens.glowSecondary,
                                    indicatorColor = tokens.glowPrimary.copy(alpha = 0.24f)
                                )
                            )
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        if (!useRail && showBottomOrRailNav) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                tonalElevation = 4.dp
                            ) {
                                topDestinations.forEach { dest ->
                                    val selected = currentDestination?.hasRoute(dest.route::class) == true
                                    val label = stringResource(id = dest.labelRes)
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopLevel(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                maxLines = 1
                                            )
                                        },
                                        modifier = Modifier.testTag(dest.testTag),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = tokens.glowSecondary,
                                            selectedTextColor = tokens.glowSecondary,
                                            indicatorColor = tokens.glowPrimary.copy(alpha = 0.24f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = HomeRoute
                        ) {
                            composable<HomeRoute> {
                                HomeScreen(
                                    settings = settingsState.settings,
                                    recentConversations = recentConversations,
                                    onStartNewChat = {
                                        navController.navigate(
                                            ChatRoute(
                                                conversationId = null,
                                                nonce = System.currentTimeMillis()
                                            )
                                        )
                                    },
                                    onOpenConversation = { id ->
                                        navController.navigate(ChatRoute(conversationId = id))
                                    },
                                    onOpenVoice = { navigateTopLevel(VoiceRoute) },
                                    onOpenImageGen = { navController.navigate(ImageGeneratorRoute) },
                                    onOpenCoding = { navController.navigate(CodingAssistantRoute) },
                                    onOpenPdfReader = { navController.navigate(PdfReaderRoute) },
                                    onOpenWebSearch = { navController.navigate(WebSearchRoute) },
                                    onOpenProfile = { navController.navigate(ProfileRoute) }
                                )
                            }

                            composable<ChatRoute> { backStackEntry ->
                                val args = backStackEntry.toRoute<ChatRoute>()
                                val chatVm: ChatViewModel = viewModel(
                                    key = "chat_${args.conversationId ?: "main"}_${args.nonce}",
                                    factory = ChatViewModel.provideFactory(
                                        container.chatRepository,
                                        container.settingsRepository,
                                        container.aiService,
                                        args.conversationId
                                    )
                                )
                                val chatState by chatVm.uiState.collectAsStateWithLifecycle()
                                ChatScreen(
                                    uiState = chatState,
                                    onSendMessage = chatVm::sendMessage,
                                    onNewChat = chatVm::startNewChat,
                                    onSelectConversation = chatVm::selectConversation,
                                    onDeleteConversation = chatVm::deleteConversation,
                                    onClearAllHistory = chatVm::clearAllHistory,
                                    onUpdateSearchQuery = chatVm::updateSearchQuery,
                                    onSelectLanguage = chatVm::setLanguage,
                                    onRegenerate = chatVm::regenerateLastResponse,
                                    onRetryFailed = chatVm::retryLastFailed
                                )
                            }

                            composable<VoiceRoute> {
                                val context = LocalContext.current
                                val voiceVm: VoiceViewModel = viewModel(
                                    factory = VoiceViewModel.provideFactory(
                                        context,
                                        container.settingsRepository,
                                        container.aiService
                                    )
                                )
                                val voiceState by voiceVm.uiState.collectAsStateWithLifecycle()
                                VoiceScreen(
                                    uiState = voiceState,
                                    onStartListening = voiceVm::startListening,
                                    onStopListening = voiceVm::stopListening,
                                    onSelectLocale = voiceVm::selectLocale,
                                    onToggleTts = voiceVm::toggleTtsEnabled,
                                    onSpeakText = voiceVm::speakText,
                                    onStopSpeaking = voiceVm::stopSpeaking,
                                    onSubmitQuery = voiceVm::submitVoiceQuery
                                )
                            }

                            composable<ToolsRoute> {
                                ToolsScreen(
                                    onOpenImageGenerator = { navController.navigate(ImageGeneratorRoute) },
                                    onOpenCodingAssistant = { navController.navigate(CodingAssistantRoute) },
                                    onOpenPdfReader = { navController.navigate(PdfReaderRoute) },
                                    onOpenWebSearch = { navController.navigate(WebSearchRoute) }
                                )
                            }

                            composable<SettingsRoute> {
                                SettingsScreen(
                                    uiState = settingsState,
                                    onUpdateLanguage = settingsVm::updateLanguage,
                                    onUpdateTheme = settingsVm::updateTheme,
                                    onUpdateVoice = settingsVm::updateVoiceSettings,
                                    onSaveGeminiConfig = settingsVm::saveGeminiConfig,
                                    onClearGeminiKey = settingsVm::clearGeminiKey,
                                    onSaveImageConfig = settingsVm::saveImageGenConfig,
                                    onClearImageKey = settingsVm::clearImageGenKey,
                                    onSaveSearchConfig = settingsVm::saveWebSearchConfig,
                                    onClearSearchKey = settingsVm::clearWebSearchKey,
                                    onClearAllHistory = settingsVm::clearAllChatHistory,
                                    onOpenProfile = { navController.navigate(ProfileRoute) },
                                    onDismissToast = settingsVm::clearToast
                                )
                            }

                            composable<ProfileRoute> {
                                ProfileScreen(
                                    uiState = settingsState,
                                    onSaveProfile = settingsVm::updateProfile,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<ImageGeneratorRoute> {
                                val context = LocalContext.current
                                val imageVm: ImageGeneratorViewModel = viewModel(
                                    factory = ImageGeneratorViewModel.provideFactory(
                                        container.aiService,
                                        container.settingsRepository
                                    )
                                )
                                val imageState by imageVm.uiState.collectAsStateWithLifecycle()
                                ImageGeneratorScreen(
                                    uiState = imageState,
                                    onPromptChange = imageVm::updatePrompt,
                                    onSelectStyle = imageVm::selectStyle,
                                    onGenerate = imageVm::generateImage,
                                    onSaveImage = { bmp, uri -> imageVm.saveImage(context, bmp, uri) },
                                    onPrepareShare = { bmp -> imageVm.prepareShareIntent(context, bmp) },
                                    onOpenSettings = { navigateTopLevel(SettingsRoute) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<CodingAssistantRoute> {
                                val codingVm: CodingAssistantViewModel = viewModel(
                                    factory = CodingAssistantViewModel.provideFactory(container.aiService)
                                )
                                val codingState by codingVm.uiState.collectAsStateWithLifecycle()
                                CodingAssistantScreen(
                                    uiState = codingState,
                                    onSelectLanguage = codingVm::selectLanguage,
                                    onSelectTask = codingVm::selectTask,
                                    onPromptChange = codingVm::updatePrompt,
                                    onSourceCodeChange = codingVm::updateSourceCode,
                                    onRunTask = codingVm::runCodingTask,
                                    onFormatAction = codingVm::formatLocalInputIfPossible,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<PdfReaderRoute> {
                                val pdfVm: PdfReaderViewModel = viewModel(
                                    factory = PdfReaderViewModel.provideFactory(
                                        container.pdfProcessor,
                                        container.aiService
                                    )
                                )
                                val pdfState by pdfVm.uiState.collectAsStateWithLifecycle()
                                PdfReaderScreen(
                                    uiState = pdfState,
                                    onPickPdfUri = pdfVm::loadPdf,
                                    onGoToPage = pdfVm::goToPage,
                                    onToggleViewMode = pdfVm::toggleViewMode,
                                    onGrantConsent = pdfVm::grantAiConsent,
                                    onQuestionChange = pdfVm::updateQuestion,
                                    onRunAiAction = pdfVm::runPdfAiAction,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<WebSearchRoute> {
                                val searchVm: WebSearchViewModel = viewModel(
                                    factory = WebSearchViewModel.provideFactory(
                                        container.webSearchRepository,
                                        container.aiService,
                                        container.settingsRepository
                                    )
                                )
                                val searchState by searchVm.uiState.collectAsStateWithLifecycle()
                                WebSearchScreen(
                                    uiState = searchState,
                                    onQueryChange = searchVm::updateQuery,
                                    onExecuteSearch = searchVm::executeSearch,
                                    onOpenSettings = { navigateTopLevel(SettingsRoute) },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
