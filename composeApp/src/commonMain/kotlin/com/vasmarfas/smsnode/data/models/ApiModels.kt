package com.vasmarfas.smsnode.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer"
)

@Serializable
data class UserResponse(
    val id: Int,
    val username: String,
    val role: String,
    @SerialName("telegram_id") val telegramId: Long? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String
)

@Serializable
data class TelegramLoginRequest(
    @SerialName("init_data") val initData: String
)

@Serializable
data class DialogSummary(
    @SerialName("external_phone") val externalPhone: String,
    @SerialName("last_text") val lastText: String,
    @SerialName("last_at") val lastAt: String? = null,
    @SerialName("last_direction") val lastDirection: String
)

@Serializable
data class MessageResponse(
    val id: Int,
    @SerialName("sim_card_id") val simCardId: Int? = null,
    @SerialName("external_phone") val externalPhone: String,
    val direction: String,
    val text: String,
    val status: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AdminMessageResponse(
    val id: Int,
    @SerialName("sim_card_id") val simCardId: Int? = null,
    @SerialName("sim_card_label") val simCardLabel: String? = null,
    val username: String? = null,
    @SerialName("external_phone") val externalPhone: String,
    val direction: String,
    val text: String,
    val status: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SendMessageRequest(
    val phone: String,
    val text: String,
    @SerialName("sim_card_id") val simCardId: Int? = null
)

@Serializable
data class SendMessageResponse(
    @SerialName("job_id") val jobId: String,
    val status: String = "queued"
)

@Serializable
data class RecentMessagesResponse(
    val messages: List<MessageResponse>
)

@Serializable
data class ContactResponse(
    val id: Int,
    val name: String,
    @SerialName("phone_number") val phoneNumber: String
)

@Serializable
data class ContactCreateRequest(
    val name: String,
    @SerialName("phone_number") val phoneNumber: String
)

@Serializable
data class ContactUpdateRequest(
    val name: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null
)

@Serializable
data class ContactImportResponse(
    val created: Int,
    val updated: Int,
    val skipped: Int
)

@Serializable
data class ContactGroupResponse(
    val id: Int,
    val name: String,
    @SerialName("contact_ids") val contactIds: List<Int> = emptyList(),
    @SerialName("contacts_count") val contactsCount: Int = 0
)

@Serializable
data class ContactGroupCreateRequest(
    val name: String
)

@Serializable
data class ContactGroupUpdateRequest(
    val name: String? = null
)

@Serializable
data class ContactGroupMembersRequest(
    @SerialName("contact_ids") val contactIds: List<Int> = emptyList()
)

@Serializable
data class SendGroupRequest(
    @SerialName("group_id") val groupId: Int,
    val text: String,
    @SerialName("sim_card_id") val simCardId: Int? = null
)

@Serializable
data class SendGroupResponse(
    val total: Int,
    @SerialName("job_ids") val jobIds: List<String>
)

@Serializable
data class SimCardResponse(
    val id: Int,
    @SerialName("port_number") val portNumber: Int,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val label: String? = null,
    val status: String,
    @SerialName("gateway_id") val gatewayId: Int? = null,
    @SerialName("assigned_user_id") val assignedUserId: Int? = null
)

@Serializable
data class SimCardUpdateRequest(
    val label: String? = null
)

@Serializable
data class GatewayResponse(
    val id: Int,
    val name: String,
    val type: String,
    val host: String,
    val port: Int,
    val username: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("last_seen") val lastSeen: String? = null,
    @SerialName("last_status") val lastStatus: String? = null
)

@Serializable
data class GatewayCreateRequest(
    val name: String,
    val type: String,
    val host: String,
    val port: Int = 9991,
    val username: String = "admin",
    val password: String
)

@Serializable
data class GatewayUpdateRequest(
    val name: String? = null,
    val host: String? = null,
    val port: Int? = null,
    val username: String? = null,
    val password: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)

@Serializable
data class GatewayTestResult(
    @SerialName("gateway_id") val gatewayId: Int,
    val online: Boolean,
    val detail: String,
    @SerialName("latency_ms") val latencyMs: Double? = null
)

@Serializable
data class SimCardCreateRequest(
    @SerialName("port_number") val portNumber: Int,
    @SerialName("phone_number") val phoneNumber: String? = null
)

@Serializable
data class DiscoveredChannel(
    val host: String,
    @SerialName("goip_id") val goipId: String,
    @SerialName("port_number") val portNumber: Int,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val signal: String? = null,
    @SerialName("gsm_status") val gsmStatus: String? = null,
    @SerialName("gateway_id") val gatewayId: Int? = null,
    @SerialName("can_add") val canAdd: Boolean = true
)

@Serializable
data class DiscoveredSimAddRequest(
    @SerialName("gateway_id") val gatewayId: Int,
    @SerialName("port_number") val portNumber: Int,
    @SerialName("phone_number") val phoneNumber: String? = null
)

@Serializable
data class UserCreateRequest(
    val username: String,
    val password: String,
    val role: String = "user",
    @SerialName("telegram_id") val telegramId: Long? = null
)

@Serializable
data class UserUpdateRequest(
    val password: String? = null,
    val role: String? = null,
    @SerialName("telegram_id") val telegramId: Long? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)

@Serializable
data class PendingRegistrationResponse(
    val id: Int,
    @SerialName("telegram_id") val telegramId: Long? = null,
    val username: String,
    val source: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class RegistrationModeResponse(
    val mode: String
)

@Serializable
data class RegistrationModeSetRequest(
    val mode: String
)

@Serializable
data class TemplateResponse(
    val id: Int,
    val name: String,
    val content: String,
    val category: String,
    @SerialName("is_global") val isGlobal: Boolean
)

@Serializable
data class TemplateCreateRequest(
    val name: String,
    val content: String,
    val category: String = "general",
    @SerialName("is_global") val isGlobal: Boolean = false
)

@Serializable
data class TemplateUpdateRequest(
    val name: String? = null,
    val content: String? = null,
    @SerialName("is_global") val isGlobal: Boolean? = null
)

@Serializable
data class HealthResponse(
    val status: String,
    val version: String,
    @SerialName("gateways_loaded") val gatewaysLoaded: Int
)

@Serializable
data class ApiErrorDetail(
    val detail: String
)
