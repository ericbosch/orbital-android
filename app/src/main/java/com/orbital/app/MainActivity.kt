package com.orbital.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.orbital.app.core.network.DiscoveredServer
import com.orbital.app.domain.AppearanceSettings
import com.orbital.app.ui.components.BottomNav
import com.orbital.app.ui.screens.app.AgentDetailScreen
import com.orbital.app.ui.screens.app.AgentsScreen
import com.orbital.app.ui.screens.app.ChatScreen
import com.orbital.app.ui.screens.app.ExploreScreen
import com.orbital.app.ui.screens.app.HomeScreen
import com.orbital.app.ui.screens.app.SessionsScreen
import com.orbital.app.ui.screens.app.SettingsScreen
import com.orbital.app.ui.screens.app.TroubleshootScreen
import com.orbital.app.ui.screens.onboarding.DetectAgents
import com.orbital.app.ui.screens.onboarding.ReadyScreen
import com.orbital.app.ui.screens.onboarding.ScanScreen
import com.orbital.app.ui.screens.onboarding.Splash
import com.orbital.app.ui.theme.OrbitalTheme
import com.orbital.app.ui.viewmodel.ChatViewModel
import com.orbital.app.ui.viewmodel.ConnectionState
import com.orbital.app.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

private val TAB_ROUTES = setOf("home", "agents", "sessions", "explore", "settings")

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OrbitalRoot() }
    }
}

@Composable
fun OrbitalRoot() {
    val vm: MainViewModel = hiltViewModel()
    val appearance by vm.appearance.collectAsState()

    OrbitalTheme(
        themeName  = appearance.themeName,
        accentName = appearance.accentName,
        fontName   = appearance.fontName
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = OrbitalTheme.colors.void
        ) {
            OrbitalNavigation(vm = vm, appearance = appearance)
        }
    }
}

@Composable
fun OrbitalNavigation(vm: MainViewModel, appearance: AppearanceSettings) {
    val navController = rememberNavController()
    val backstackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backstackEntry?.destination?.route?.substringBefore("/")
    val showBottomNav = currentRoute in TAB_ROUTES

    Scaffold(
        containerColor = OrbitalTheme.colors.void,
        bottomBar = {
            if (showBottomNav) {
                BottomNav(
                    active = currentRoute ?: "home",
                    onChange = { tab ->
                        if (tab != currentRoute) {
                            navController.navigate(tab) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = "splash",
            modifier         = Modifier.padding(innerPadding)
        ) {
            // ── Onboarding ──────────────────────────────────────────
            composable("splash") {
                val connectionState by vm.connectionState.collectAsState()
                Splash(onNext = {
                    if (connectionState is ConnectionState.Connected) {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("scan")
                    }
                })
            }
            composable("scan") {
                val discoveredServers by vm.discoveredServers.collectAsState()
                val connectionState   by vm.connectionState.collectAsState()
                LaunchedEffect(connectionState) {
                    if (connectionState is ConnectionState.Connected) {
                        navController.navigate("detect") {
                            popUpTo("scan") { inclusive = true }
                        }
                    }
                }
                ScanScreen(
                    discoveredServers = discoveredServers,
                    connectionState   = connectionState,
                    onStartScan       = vm::startScan,
                    onConnect         = { server, token -> vm.connect(server, token) },
                    onManual          = { host, port, token -> vm.connectManual(host, port, token) }
                )
            }
            composable("detect") {
                DetectAgents(onNext = { navController.navigate("ready") })
            }
            composable("ready") {
                ReadyScreen(onDone = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                })
            }

            // ── App tabs ────────────────────────────────────────────
            composable("home") {
                HomeScreen(
                    serverName    = vm.serverName,
                    serverLatency = vm.latencyMs,
                    agents        = vm.agents,
                    sessions      = vm.sessions,
                    onNavAgents   = {
                        navController.navigate("agents") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    onNavSessions = {
                        navController.navigate("sessions") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    onSession = { s -> navController.navigate("chat/${s.id}") }
                )
            }
            composable("agents") {
                AgentsScreen(
                    agents  = vm.agents,
                    onAgent = { a -> navController.navigate("agent/${a.id}") }
                )
            }
            composable("sessions") {
                SessionsScreen(
                    sessions  = vm.sessions,
                    onSession = { s -> navController.navigate("chat/${s.id}") }
                )
            }
            composable("explore") {
                ExploreScreen(
                    skills = vm.skills,
                    searchResults = vm.searchResults,
                    isSearching = vm.isSearching,
                    onSearch = vm::search
                )
            }
            composable("settings") {
                val connectionState by vm.connectionState.collectAsState()
                SettingsScreen(
                    serverHost         = vm.serverHost,
                    authToken          = vm.authToken,
                    connectionError    = (connectionState as? ConnectionState.Error)?.message,
                    appearance         = appearance,
                    onAppearanceChange = vm::updateAppearance,
                    onSaveToken        = vm::updateServerToken,
                    onRefreshData      = vm::refreshFromServer,
                    onDisconnect       = vm::disconnect,
                    onTroubleshoot     = { navController.navigate("troubleshoot") }
                )
            }

            // ── Sub-screens ─────────────────────────────────────────
            composable(
                route     = "chat/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { back ->
                val id      = back.arguments?.getString("sessionId") ?: return@composable
                val session = vm.sessions.find { it.id == id }       ?: return@composable
                val chatVm: ChatViewModel = hiltViewModel()
                LaunchedEffect(id) { chatVm.bindSession(session) }
                ChatScreen(
                    session = session,
                    messages = chatVm.messages,
                    isStreaming = chatVm.isStreaming,
                    errorMessage = chatVm.errorMessage,
                    onSendMessage = chatVm::sendMessage,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route     = "agent/{agentId}",
                arguments = listOf(navArgument("agentId") { type = NavType.StringType })
            ) { back ->
                val id    = back.arguments?.getString("agentId") ?: return@composable
                val agent = vm.agents.find { it.id == id }       ?: return@composable
                AgentDetailScreen(agent = agent, onBack = { navController.popBackStack() })
            }
            composable("troubleshoot") {
                TroubleshootScreen(
                    serverName = vm.serverName,
                    checks = vm.diagnostics,
                    isRunning = vm.diagnosticsRunning,
                    onRun = vm::runDiagnostics,
                    onBack     = { navController.popBackStack() }
                )
            }
        }
    }
}
