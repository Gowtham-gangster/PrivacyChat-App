package com.privychat.app.ui.navigation

import java.util.UUID

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object EmailVerify : Screen("email_verify/{email}") {
        fun createRoute(email: String) = "email_verify/$email"
    }
    data object PhoneVerify : Screen("phone_verify/{phone}") {
        fun createRoute(phone: String) = "phone_verify/$phone"
    }
    data object Dashboard : Screen("dashboard")
    data object Conversation : Screen("conversation/{conversationId}/{participantName}/{participantUserId}") {
        fun createRoute(convId: UUID, name: String, userId: UUID) = "conversation/$convId/$name/$userId"
    }
    data object Call : Screen("call/{callId}/{otherPartyName}/{callType}/{isIncoming}") {
        fun createRoute(callId: UUID, name: String, type: String, incoming: Boolean) = "call/$callId/$name/$type/$incoming"
    }
    data object Contacts : Screen("contacts")
    data object Settings : Screen("settings")
    data object SafetyNumber : Screen("safety_number/{name}/{userId}") {
        fun createRoute(name: String, userId: UUID) = "safety_number/$name/$userId"
    }
}

