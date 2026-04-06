package com.vasmarfas.smsnode.data.api

import com.vasmarfas.smsnode.data.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class SmsNodeApi(
    private val client: HttpClient,
    private var baseUrl: String,
    private var token: String? = null
) {
    fun setBaseUrl(url: String) { baseUrl = url.trimEnd('/') }
    fun setToken(t: String?) { token = t }
    fun getToken(): String? = token

    private fun HttpRequestBuilder.auth() {
        token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }

    private suspend fun request(
        block: suspend HttpRequestBuilder.() -> Unit
    ): ApiResult<HttpResponse> {
        val response = runCatching { client.request { block() } }
        return response.fold(
            onSuccess = { resp ->
                when (resp.status.value) {
                    in 200..299 -> ApiResult.Success(resp)
                    401 -> {
                        com.vasmarfas.smsnode.data.events.SmsNodeEvents.emitUnauthorized()
                        ApiResult.Unauthorized
                    }
                    403 -> ApiResult.Forbidden(parseDetail(resp))
                    404 -> ApiResult.NotFound(parseDetail(resp))
                    409 -> ApiResult.Conflict(parseDetail(resp))
                    422 -> ApiResult.ValidationError(parseDetail(resp))
                    else -> ApiResult.Error(resp.status.value, parseDetail(resp))
                }
            },
            onFailure = { ApiResult.NetworkError(it.message ?: it.toString()) }
        )
    }

    private suspend fun parseDetail(response: HttpResponse): String {
        return runCatching {
            response.body<ApiErrorDetail>().detail
        }.getOrElse { response.bodyAsText().take(200) }
    }

    suspend fun health(): ApiResult<HealthResponse> = request {
        method = HttpMethod.Get
        url("$baseUrl/")
        auth()
    }.mapBody { it.body<HealthResponse>() }

    private fun formUrlEncode(value: String): String = buildString {
        for (c in value) {
            when (c) {
                ' ' -> append('+')
                in 'a'..'z', in 'A'..'Z', in '0'..'9', '-', '_', '.', '~' -> append(c)
                else -> append("%").append(c.code.toString(16).uppercase().padStart(2, '0'))
            }
        }
    }

    suspend fun loginByTelegram(initData: String): ApiResult<TokenResponse> = runCatching {
        client.post("$baseUrl/auth/telegram") {
            contentType(ContentType.Application.Json)
            setBody(TelegramLoginRequest(initData))
        }
    }.fold(
        onSuccess = { resp ->
            when (resp.status.value) {
                200 -> ApiResult.Success(resp).mapBody { it.body<TokenResponse>() }
                401 -> ApiResult.Unauthorized
                403 -> ApiResult.Forbidden(parseDetail(resp))
                503 -> ApiResult.Error(503, parseDetail(resp))
                else -> ApiResult.Error(resp.status.value, parseDetail(resp))
            }
        },
        onFailure = { ApiResult.NetworkError(it.message ?: it.toString()) }
    )

    suspend fun login(username: String, password: String): ApiResult<TokenResponse> = runCatching {
        val body = "username=${formUrlEncode(username)}&password=${formUrlEncode(password)}"
        client.post("$baseUrl/auth/token") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(body)
        }
    }.fold(
        onSuccess = { resp ->
            when (resp.status.value) {
                200 -> ApiResult.Success(resp).mapBody { it.body<TokenResponse>() }
                401 -> ApiResult.Unauthorized
                403 -> ApiResult.Forbidden(parseDetail(resp))
                else -> ApiResult.Error(resp.status.value, parseDetail(resp))
            }
        },
        onFailure = { ApiResult.NetworkError(it.message ?: it.toString()) }
    )

    suspend fun register(username: String, password: String): ApiResult<RegisterResult> = runCatching {
        client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username, password))
        }
    }.fold(
        onSuccess = { resp ->
            when (resp.status.value) {
                201 -> ApiResult.Success(resp).mapBody { RegisterResult.Created(resp.body<UserResponse>()) }
                202 -> ApiResult.Success(resp).mapBody { RegisterResult.Pending(resp.body<ApiErrorDetail>().detail) }
                403 -> ApiResult.Forbidden(parseDetail(resp))
                409 -> ApiResult.Conflict(parseDetail(resp))
                else -> ApiResult.Error(resp.status.value, parseDetail(resp))
            }
        },
        onFailure = { ApiResult.NetworkError(it.message ?: it.toString()) }
    )

    suspend fun me(): ApiResult<UserResponse> = request {
        method = HttpMethod.Get
        url("$baseUrl/auth/me")
        auth()
    }.mapBody { it.body<UserResponse>() }

    suspend fun getDialogs(): ApiResult<List<DialogSummary>> = request {
        method = HttpMethod.Get
        url("$baseUrl/api/v1/user/dialogs")
        auth()
    }.mapBody { it.body<List<DialogSummary>>() }

    suspend fun getDialogMessages(
        phone: String,
        limit: Int = 50,
        offset: Int = 0
    ): ApiResult<List<MessageResponse>> = runCatching {
        val pathPhone = formUrlEncode(phone)
        client.get("$baseUrl/api/v1/user/dialogs/$pathPhone/messages") {
            parameter("limit", limit)
            parameter("offset", offset)
            auth()
        }
    }.fold(
        onSuccess = { resp ->
            when (resp.status.value) {
                in 200..299 -> ApiResult.Success(resp).mapBody { it.body<List<MessageResponse>>() }
                401 -> ApiResult.Unauthorized
                403 -> ApiResult.Forbidden(parseDetail(resp))
                404 -> ApiResult.NotFound(parseDetail(resp))
                409 -> ApiResult.Conflict(parseDetail(resp))
                422 -> ApiResult.ValidationError(parseDetail(resp))
                else -> ApiResult.Error(resp.status.value, parseDetail(resp))
            }
        },
        onFailure = { ApiResult.NetworkError(it.message ?: it.toString()) }
    )

    suspend fun getRecentMessages(since: String? = null): ApiResult<RecentMessagesResponse> = runCatching {
        val u = "$baseUrl/api/v1/user/messages/recent"
        client.get(u) {
            since?.let { parameter("since", it) }
            auth()
        }
    }.fold(
        onSuccess = { resp ->
            when (resp.status.value) {
                in 200..299 -> ApiResult.Success(resp).mapBody { it.body<RecentMessagesResponse>() }
                401 -> ApiResult.Unauthorized
                403 -> ApiResult.Forbidden(parseDetail(resp))
                404 -> ApiResult.NotFound(parseDetail(resp))
                409 -> ApiResult.Conflict(parseDetail(resp))
                422 -> ApiResult.ValidationError(parseDetail(resp))
                else -> ApiResult.Error(resp.status.value, parseDetail(resp))
            }
        },
        onFailure = { ApiResult.NetworkError(it.message ?: it.toString()) }
    )

    suspend fun sendMessage(phone: String, text: String, simCardId: Int? = null): ApiResult<SendMessageResponse> = request {
        method = HttpMethod.Post
        url("$baseUrl/api/v1/user/messages/send")
        setBody(SendMessageRequest(phone, text, simCardId))
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<SendMessageResponse>() }

    suspend fun sendGroupMessage(groupId: Int, text: String, simCardId: Int? = null): ApiResult<SendGroupResponse> = request {
        method = HttpMethod.Post
        url("$baseUrl/api/v1/user/messages/send_group")
        setBody(SendGroupRequest(groupId = groupId, text = text, simCardId = simCardId))
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<SendGroupResponse>() }

    suspend fun getContacts(): ApiResult<List<ContactResponse>> = request {
        method = HttpMethod.Get
        url("$baseUrl/api/v1/user/contacts")
        auth()
    }.mapBody { it.body<List<ContactResponse>>() }

    suspend fun createContact(name: String, phoneNumber: String): ApiResult<ContactResponse> = request {
        method = HttpMethod.Post
        url("$baseUrl/api/v1/user/contacts")
        setBody(ContactCreateRequest(name, phoneNumber))
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<ContactResponse>() }

    suspend fun getContact(contactId: Int): ApiResult<ContactResponse> = request {
        method = HttpMethod.Get
        url("$baseUrl/api/v1/user/contacts/$contactId")
        auth()
    }.mapBody { it.body<ContactResponse>() }

    suspend fun updateContact(contactId: Int, name: String?, phoneNumber: String?): ApiResult<ContactResponse> = request {
        method = HttpMethod.Patch
        url("$baseUrl/api/v1/user/contacts/$contactId")
        setBody(ContactUpdateRequest(name, phoneNumber))
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<ContactResponse>() }

    suspend fun deleteContact(contactId: Int): ApiResult<Unit> = request {
        method = HttpMethod.Delete
        url("$baseUrl/api/v1/user/contacts/$contactId")
        auth()
    }.mapBody { }

    suspend fun getContactGroups(): ApiResult<List<ContactGroupResponse>> = request {
        method = HttpMethod.Get
        url("$baseUrl/api/v1/user/contact-groups")
        auth()
    }.mapBody { it.body<List<ContactGroupResponse>>() }

    suspend fun createContactGroup(name: String): ApiResult<ContactGroupResponse> = request {
        method = HttpMethod.Post
        url("$baseUrl/api/v1/user/contact-groups")
        setBody(ContactGroupCreateRequest(name))
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<ContactGroupResponse>() }

    suspend fun updateContactGroup(groupId: Int, name: String?): ApiResult<ContactGroupResponse> = request {
        method = HttpMethod.Patch
        url("$baseUrl/api/v1/user/contact-groups/$groupId")
        setBody(ContactGroupUpdateRequest(name = name))
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<ContactGroupResponse>() }

    suspend fun setContactGroupMembers(groupId: Int, contactIds: List<Int>): ApiResult<ContactGroupResponse> = request {
        method = HttpMethod.Put
        url("$baseUrl/api/v1/user/contact-groups/$groupId/members")
        setBody(ContactGroupMembersRequest(contactIds = contactIds))
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<ContactGroupResponse>() }

    suspend fun deleteContactGroup(groupId: Int): ApiResult<Unit> = request {
        method = HttpMethod.Delete
        url("$baseUrl/api/v1/user/contact-groups/$groupId")
        auth()
    }.mapBody { }

    suspend fun getMySims(): ApiResult<List<SimCardResponse>> = request {
        method = HttpMethod.Get
        url("$baseUrl/api/v1/user/me/sims")
        auth()
    }.mapBody { it.body<List<SimCardResponse>>() }

    suspend fun updateMySim(simId: Int, label: String?): ApiResult<SimCardResponse> = request {
        method = HttpMethod.Patch
        url("$baseUrl/api/v1/user/me/sims/$simId")
        setBody(SimCardUpdateRequest(label = label))
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<SimCardResponse>() }

    suspend fun getTemplates(): ApiResult<List<TemplateResponse>> = request {
        method = HttpMethod.Get
        url("$baseUrl/api/v1/user/templates")
        auth()
    }.mapBody { it.body<List<TemplateResponse>>() }

    suspend fun createTemplate(name: String, content: String, isGlobal: Boolean = false): ApiResult<TemplateResponse> = request {
        method = HttpMethod.Post
        url("$baseUrl/api/v1/user/templates")
        setBody(TemplateCreateRequest(name, content, isGlobal = isGlobal))
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<TemplateResponse>() }

    suspend fun updateTemplate(id: Int, name: String?, content: String?, isGlobal: Boolean? = null): ApiResult<TemplateResponse> = request {
        method = HttpMethod.Patch
        url("$baseUrl/api/v1/user/templates/$id")
        setBody(TemplateUpdateRequest(name, content, isGlobal))
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<TemplateResponse>() }

    suspend fun deleteTemplate(templateId: Int): ApiResult<Unit> = request {
        method = HttpMethod.Delete
        url("$baseUrl/api/v1/user/templates/$templateId")
        auth()
    }.mapBody { }

    suspend fun getRegistrationMode(): ApiResult<RegistrationModeResponse> = request {
        method = HttpMethod.Get
        url("$baseUrl/api/v1/admin/settings/registration-mode")
        auth()
    }.mapBody { it.body<RegistrationModeResponse>() }

    suspend fun setRegistrationMode(mode: String): ApiResult<RegistrationModeResponse> = request {
        method = HttpMethod.Patch
        url("$baseUrl/api/v1/admin/settings/registration-mode")
        setBody(RegistrationModeSetRequest(mode))
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<RegistrationModeResponse>() }

    suspend fun getPendingRegistrations(): ApiResult<List<PendingRegistrationResponse>> = request {
        method = HttpMethod.Get
        url("$baseUrl/api/v1/admin/pending-registrations")
        auth()
    }.mapBody { it.body<List<PendingRegistrationResponse>>() }

    suspend fun approvePendingRegistration(pendingId: Int): ApiResult<UserResponse> = request {
        method = HttpMethod.Post
        url("$baseUrl/api/v1/admin/pending-registrations/$pendingId/approve")
        auth()
    }.mapBody { it.body<UserResponse>() }

    suspend fun rejectPendingRegistration(pendingId: Int): ApiResult<Unit> = request {
        method = HttpMethod.Delete
        url("$baseUrl/api/v1/admin/pending-registrations/$pendingId")
        auth()
    }.mapBody { }

    suspend fun getGateways(): ApiResult<List<GatewayResponse>> = request {
        method = HttpMethod.Get
        url("$baseUrl/api/v1/admin/gateways")
        auth()
    }.mapBody { it.body<List<GatewayResponse>>() }

    suspend fun createGateway(body: GatewayCreateRequest): ApiResult<GatewayResponse> = request {
        method = HttpMethod.Post
        url("$baseUrl/api/v1/admin/gateways")
        setBody(body)
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<GatewayResponse>() }

    suspend fun getGateway(gatewayId: Int): ApiResult<GatewayResponse> = request {
        method = HttpMethod.Get
        url("$baseUrl/api/v1/admin/gateways/$gatewayId")
        auth()
    }.mapBody { it.body<GatewayResponse>() }

    suspend fun updateGateway(gatewayId: Int, body: GatewayUpdateRequest): ApiResult<GatewayResponse> = request {
        method = HttpMethod.Patch
        url("$baseUrl/api/v1/admin/gateways/$gatewayId")
        setBody(body)
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<GatewayResponse>() }

    suspend fun deleteGateway(gatewayId: Int, force: Boolean = false): ApiResult<Unit> = request {
        method = HttpMethod.Delete
        url("$baseUrl/api/v1/admin/gateways/$gatewayId?force=$force")
        auth()
    }.mapBody { }

    suspend fun testGateway(gatewayId: Int): ApiResult<GatewayTestResult> = request {
        method = HttpMethod.Post
        url("$baseUrl/api/v1/admin/gateways/$gatewayId/test")
        auth()
    }.mapBody { it.body<GatewayTestResult>() }

    suspend fun getGatewaySims(gatewayId: Int): ApiResult<List<SimCardResponse>> = request {
        method = HttpMethod.Get
        url("$baseUrl/api/v1/admin/gateways/$gatewayId/sims")
        auth()
    }.mapBody { it.body<List<SimCardResponse>>() }

    suspend fun addGatewaySim(gatewayId: Int, portNumber: Int, phoneNumber: String? = null): ApiResult<SimCardResponse> = request {
        method = HttpMethod.Post
        url("$baseUrl/api/v1/admin/gateways/$gatewayId/sims")
        setBody(SimCardCreateRequest(portNumber, phoneNumber))
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<SimCardResponse>() }

    suspend fun getDiscoveredSims(minutes: Int = 10): ApiResult<List<DiscoveredChannel>> = request {
        method = HttpMethod.Get
        url("$baseUrl/api/v1/admin/gateways/discovered-sims?minutes=$minutes")
        auth()
    }.mapBody { it.body<List<DiscoveredChannel>>() }

    suspend fun addDiscoveredSim(body: DiscoveredSimAddRequest): ApiResult<SimCardResponse> = request {
        method = HttpMethod.Post
        url("$baseUrl/api/v1/admin/gateways/discovered-sims")
        setBody(body)
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<SimCardResponse>() }

    suspend fun getUsers(): ApiResult<List<UserResponse>> = request {
        method = HttpMethod.Get
        url("$baseUrl/api/v1/admin/users")
        auth()
    }.mapBody { it.body<List<UserResponse>>() }

    suspend fun createUser(body: UserCreateRequest): ApiResult<UserResponse> = request {
        method = HttpMethod.Post
        url("$baseUrl/api/v1/admin/users")
        setBody(body)
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<UserResponse>() }

    suspend fun getUser(userId: Int): ApiResult<UserResponse> = request {
        method = HttpMethod.Get
        url("$baseUrl/api/v1/admin/users/$userId")
        auth()
    }.mapBody { it.body<UserResponse>() }

    suspend fun updateUser(userId: Int, body: UserUpdateRequest): ApiResult<UserResponse> = request {
        method = HttpMethod.Patch
        url("$baseUrl/api/v1/admin/users/$userId")
        setBody(body)
        contentType(ContentType.Application.Json)
        auth()
    }.mapBody { it.body<UserResponse>() }

    suspend fun deactivateUser(userId: Int): ApiResult<Unit> = request {
        method = HttpMethod.Delete
        url("$baseUrl/api/v1/admin/users/$userId")
        auth()
    }.mapBody { }

    suspend fun getUserSims(userId: Int): ApiResult<List<SimCardResponse>> = request {
        method = HttpMethod.Get
        url("$baseUrl/api/v1/admin/users/$userId/sims")
        auth()
    }.mapBody { it.body<List<SimCardResponse>>() }

    suspend fun assignSimToUser(userId: Int, simId: Int): ApiResult<Unit> = request {
        method = HttpMethod.Post
        url("$baseUrl/api/v1/admin/users/$userId/sims/$simId")
        auth()
    }.mapBody { }

    suspend fun revokeSimFromUser(userId: Int, simId: Int): ApiResult<Unit> = request {
        method = HttpMethod.Delete
        url("$baseUrl/api/v1/admin/users/$userId/sims/$simId")
        auth()
    }.mapBody { }

    suspend fun getAdminMessages(limit: Int = 50, offset: Int = 0, direction: String? = null, externalPhone: String? = null): ApiResult<List<AdminMessageResponse>> = request {
        method = HttpMethod.Get
        val q = buildString {
            append("limit=$limit&offset=$offset")
            direction?.let { append("&direction=${formUrlEncode(it)}") }
            externalPhone?.let { append("&external_phone=${formUrlEncode(it)}") }
        }
        url("$baseUrl/api/v1/admin/messages?$q")
        auth()
    }.mapBody { it.body<List<AdminMessageResponse>>() }
}

sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    object Unauthorized : ApiResult<Nothing>()
    data class Forbidden(val message: String) : ApiResult<Nothing>()
    data class NotFound(val message: String) : ApiResult<Nothing>()
    data class Conflict(val message: String) : ApiResult<Nothing>()
    data class ValidationError(val message: String) : ApiResult<Nothing>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val message: String) : ApiResult<Nothing>()

    @Suppress("UNCHECKED_CAST")
    inline fun <R> mapBody(block: (HttpResponse) -> R): ApiResult<R> = when (this) {
        is Success -> ApiResult.Success(block(value as HttpResponse))
        is Unauthorized -> this as ApiResult<R>
        is Forbidden -> this as ApiResult<R>
        is NotFound -> this as ApiResult<R>
        is Conflict -> this as ApiResult<R>
        is ValidationError -> this as ApiResult<R>
        is Error -> this as ApiResult<R>
        is NetworkError -> this as ApiResult<R>
    }
}

sealed class RegisterResult {
    data class Created(val user: UserResponse) : RegisterResult()
    data class Pending(val message: String) : RegisterResult()
}
