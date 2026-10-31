package com.privychat.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.privychat.app.ui.call.CallScreen
import com.privychat.app.ui.call.CallViewModel
import com.privychat.app.ui.chat.ConversationScreen
import com.privychat.app.ui.chat.ConversationViewModel
import com.privychat.app.ui.contacts.ContactsScreen
import com.privychat.app.ui.contacts.ContactsViewModel
import com.privychat.app.ui.dashboard.ChatsDashboardScreen
import com.privychat.app.ui.dashboard.ChatsDashboardViewModel
import com.privychat.app.ui.onboarding.*
import com.privychat.app.ui.profile.EditProfileScreen
import com.privychat.app.ui.profile.ProfileScreen
import com.privychat.app.ui.profile.ProfileViewModel
import com.privychat.app.ui.settings.SafetyNumberScreen
import com.privychat.app.ui.settings.SettingsScreen
import com.privychat.app.ui.settings.SettingsViewModel
import java.util.UUID

@Composable
fun PrivyNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            SplashScreen(
                viewModel = authViewModel,
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToWelcome = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onRegisterSuccess = { email, phone ->
                    navController.navigate(Screen.EmailVerify.createRoute(email))
                }
            )
        }

        composable(
            route = Screen.EmailVerify.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val authViewModel: AuthViewModel = hiltViewModel()
            EmailVerifyScreen(
                email = email,
                viewModel = authViewModel,
                onVerificationSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val dashboardViewModel: ChatsDashboardViewModel = hiltViewModel()
            ChatsDashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToConversation = { convId, name, userId ->
                    navController.navigate(Screen.Conversation.createRoute(convId, name, userId))
                },
                onNavigateToContacts = {
                    navController.navigate(Screen.Contacts.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Profile.route) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onLogout = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.EditProfile.route) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            EditProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Conversation.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("participantName") { type = NavType.StringType },
                navArgument("participantUserId") { type = NavType.StringType }
            )
        ) {
            val conversationViewModel: ConversationViewModel = hiltViewModel()
            ConversationScreen(
                viewModel = conversationViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onInitiateCall = { callId, name, type ->
                    navController.navigate(Screen.Call.createRoute(callId, name, type, false))
                },
                onNavigateToSafetyNumber = { name, userId ->
                    navController.navigate(Screen.SafetyNumber.createRoute(name, userId))
                }
            )
        }

        composable(
            route = Screen.Call.route,
            arguments = listOf(
                navArgument("callId") { type = NavType.StringType },
                navArgument("otherPartyName") { type = NavType.StringType },
                navArgument("callType") { type = NavType.StringType },
                navArgument("isIncoming") { type = NavType.StringType }
            )
        ) {
            val callViewModel: CallViewModel = hiltViewModel()
            CallScreen(
                viewModel = callViewModel,
                onCallEnded = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Contacts.route) {
            val contactsViewModel: ContactsViewModel = hiltViewModel()
            ContactsScreen(
                viewModel = contactsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartConversation = { convId, name, userId ->
                    navController.navigate(Screen.Conversation.createRoute(convId, name, userId)) {
                        popUpTo(Screen.Contacts.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onLoggedOut = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.SafetyNumber.route,
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: "Contact"
            SafetyNumberScreen(
                contactName = name,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

