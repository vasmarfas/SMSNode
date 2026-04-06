package com.vasmarfas.smsnode.ui.navigation

import kotlinx.serialization.Serializable

const val ROUTE_LOGIN = "login"
const val ROUTE_REGISTER = "register"
const val ROUTE_DIALOGS = "dialogs"
const val ROUTE_CONTACTS = "contacts"
const val ROUTE_MY_SIMS = "my_sims"
const val ROUTE_PROFILE = "profile"
const val ROUTE_CHAT = "chat/{phone}"
const val ROUTE_ADMIN = "admin"
const val ROUTE_ADMIN_GATEWAYS = "admin/gateways"
const val ROUTE_ADMIN_USERS = "admin/users"
const val ROUTE_ADMIN_PENDING = "admin/pending"
const val ROUTE_ADMIN_REG_MODE = "admin/regmode"
const val ROUTE_ADMIN_MESSAGES = "admin/messages"

fun routeChat(phone: String) = "chat/$phone"

@Serializable
object Login

@Serializable
object Register

@Serializable
object Dialogs

@Serializable
data class Chat(val phone: String)

@Serializable
object Contacts

@Serializable
object MySims

@Serializable
object Profile

@Serializable
object AdminGateways

@Serializable
data class AdminGatewayDetail(val gatewayId: Int)

@Serializable
object AdminUsers

@Serializable
data class AdminUserDetail(val userId: Int)

@Serializable
object AdminPendingRegistrations

@Serializable
object AdminRegistrationMode

@Serializable
object AdminMessages

@Serializable
object Settings
