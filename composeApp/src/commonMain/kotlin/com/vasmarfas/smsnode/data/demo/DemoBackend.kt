package com.vasmarfas.smsnode.data.demo

import com.vasmarfas.smsnode.data.models.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.resources.ExperimentalResourceApi
import smsnode.composeapp.generated.resources.Res

internal val demoJson = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

/** Сентинел-адрес офлайн-демо: реального сетевого запроса не происходит. */
const val OFFLINE_DEMO_URL = "https://offline.smsnode.local"

/**
 * Создаёт HTTP-клиент, который вместо сети обслуживает встроенный демо-набор данных.
 * Поведение повторяет серверный демо-режим: мутации проходят и видны в рамках сессии,
 * но живут только в памяти и сбрасываются при перезапуске приложения.
 */
fun createDemoHttpClient(): HttpClient {
    val backend = DemoBackend()
    return HttpClient(MockEngine) {
        install(ContentNegotiation) { json(demoJson) }
        engine {
            addHandler { request -> backend.handle(this, request) }
        }
    }
}

// --- DTO для разбора bundled-датасета (формат таблиц БД) ---

@Serializable
private data class Dataset(
    val anchor: String,
    val users: List<DUser>,
    val gateways: List<DGateway>,
    @SerialName("sim_cards") val simCards: List<DSim>,
    val contacts: List<DContact>,
    @SerialName("contact_groups") val groups: List<DGroup>,
    @SerialName("contact_group_members") val members: List<DMember>,
    val templates: List<DTemplate>,
    val messages: List<DMessage>,
)

@Serializable private data class DUser(val id: Int, val username: String, val password: String, val role: String, @SerialName("is_active") val isActive: Boolean = true)
@Serializable private data class DGateway(val id: Int, val name: String, val type: String, val host: String, val port: Int, val username: String, @SerialName("is_active") val isActive: Boolean = true, @SerialName("last_status") val lastStatus: String? = null)
@Serializable private data class DSim(val id: Int, @SerialName("gateway_id") val gatewayId: Int, @SerialName("port_number") val portNumber: Int, @SerialName("phone_number") val phoneNumber: String? = null, val operator: String? = null, val status: String = "UNKNOWN", val label: String? = null, @SerialName("assigned_user_id") val assignedUserId: Int? = null)
@Serializable private data class DContact(val id: Int, @SerialName("user_id") val userId: Int, @SerialName("phone_number") val phoneNumber: String, val name: String)
@Serializable private data class DGroup(val id: Int, @SerialName("user_id") val userId: Int, val name: String)
@Serializable private data class DMember(@SerialName("group_id") val groupId: Int, @SerialName("contact_id") val contactId: Int)
@Serializable private data class DTemplate(val id: Int, val name: String, val content: String, val category: String, @SerialName("is_global") val isGlobal: Boolean = false, @SerialName("user_id") val userId: Int? = null)
@Serializable private data class DMessage(
    val id: Int,
    @SerialName("sim_card_id") val simCardId: Int? = null,
    @SerialName("external_phone") val externalPhone: String,
    val direction: String,
    val text: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
)

private fun dirValue(name: String) = if (name == "OUTGOING") "out" else "in"
private fun enumValue(name: String) = name.lowercase()

private fun normInstant(s: String): String {
    var v = s.trim().replace(' ', 'T')
    v = Regex("([+-]\\d{2})$").replace(v) { it.groupValues[1] + ":00" }
    v = v.replace("+00:00", "Z")
    return v
}

@OptIn(ExperimentalTime::class)
private class DemoBackend {
    private val mutex = Mutex()
    private var loaded = false

    private lateinit var users: List<DUser>
    private lateinit var gateways: List<DGateway>
    private lateinit var sims: List<DSim>
    private val contacts = mutableListOf<DContact>()
    private val groups = mutableListOf<DGroup>()
    private val members = mutableListOf<DMember>()
    private val templates = mutableListOf<DTemplate>()
    private val messages = mutableListOf<DMessage>()
    private var nextId = 100000

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            val bytes = Res.readBytes("files/demo_seed_data.json")
            val data = demoJson.decodeFromString<Dataset>(bytes.decodeToString())

            // Сдвигаем даты так, чтобы самое свежее сообщение оказалось "сейчас".
            val anchor = Instant.parse(normInstant(data.anchor))
            val offset = Clock.System.now() - anchor

            users = data.users
            gateways = data.gateways
            sims = data.simCards
            contacts += data.contacts
            groups += data.groups
            members += data.members
            templates += data.templates
            messages += data.messages.map {
                it.copy(createdAt = (Instant.parse(normInstant(it.createdAt)) + offset).toString())
            }
            nextId = (messages.maxOfOrNull { it.id } ?: 0) + 1
            loaded = true
        }
    }

    private fun tokenFor(username: String) = "demo-offline-token:$username"
    private fun userOf(token: String?): DUser? {
        val name = token?.substringAfter("demo-offline-token:", "") ?: return null
        return users.firstOrNull { it.username == name }
    }

    private fun userSimIds(userId: Int) = sims.filter { it.assignedUserId == userId }.map { it.id }.toSet()

    suspend fun handle(scope: MockRequestHandleScope, request: HttpRequestData): HttpResponseData {
        ensureLoaded()
        val path = request.url.encodedPath
        val method = request.method
        val token = request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?.trim()
        val seg = path.trim('/').split('/')

        // --- Auth ---
        if (path == "/" && method == HttpMethod.Get) {
            return scope.json(HealthResponse(status = "ok", version = "demo", gatewaysLoaded = gateways.size))
        }
        if (path == "/auth/token" && method == HttpMethod.Post) {
            val form = bodyText(request).split("&").mapNotNull {
                val p = it.split("=", limit = 2); if (p.size == 2) p[0] to p[1] else null
            }.toMap()
            val username = form["username"].orEmpty()
            val password = form["password"].orEmpty()
            val u = users.firstOrNull { it.username == username && it.password == password }
                ?: return scope.error(HttpStatusCode.Unauthorized, "Неверный логин или пароль")
            return scope.json(TokenResponse(accessToken = tokenFor(u.username)))
        }
        if (path == "/auth/register" && method == HttpMethod.Post) {
            return scope.error(HttpStatusCode.Forbidden, "Регистрация недоступна в офлайн-демо")
        }
        if (path == "/auth/me" && method == HttpMethod.Get) {
            val u = userOf(token) ?: return scope.unauthorized()
            return scope.json(UserResponse(id = u.id, username = u.username, role = enumValue(u.role), isActive = u.isActive))
        }

        val me = userOf(token) ?: return scope.unauthorized()

        // --- User scope ---
        if (path == "/api/v1/user/dialogs" && method == HttpMethod.Get) {
            return scope.json(dialogsFor(me.id))
        }
        if (seg.size == 6 && seg[3] == "dialogs" && seg[5] == "messages" && method == HttpMethod.Get) {
            val phone = decode(seg[4])
            val mine = userSimIds(me.id)
            val list = messages.filter { it.externalPhone == phone && it.simCardId in mine }
                .sortedBy { it.createdAt }
                .map { it.toResponse() }
            return scope.json(list)
        }
        if (path == "/api/v1/user/messages/recent" && method == HttpMethod.Get) {
            return scope.json(RecentMessagesResponse(messages = emptyList()))
        }
        if (path == "/api/v1/user/messages/send" && method == HttpMethod.Post) {
            val req = decodeBody<SendMessageRequest>(request)
            val simId = req.simCardId ?: userSimIds(me.id).firstOrNull()
            val msg = DMessage(nextId++, simId, req.phone, "OUTGOING", req.text, "SENT_OK", Clock.System.now().toString())
            messages += msg
            return scope.json(SendMessageResponse(jobId = "demo-job-${msg.id}"))
        }
        if (path == "/api/v1/user/messages/send_group" && method == HttpMethod.Post) {
            val req = decodeBody<SendGroupRequest>(request)
            val contactIds = members.filter { it.groupId == req.groupId }.map { it.contactId }
            val targets = contacts.filter { it.id in contactIds }
            val simId = req.simCardId ?: userSimIds(me.id).firstOrNull()
            val jobIds = targets.map { c ->
                val msg = DMessage(nextId++, simId, c.phoneNumber, "OUTGOING", req.text, "SENT_OK", Clock.System.now().toString())
                messages += msg
                "demo-job-${msg.id}"
            }
            return scope.json(SendGroupResponse(total = jobIds.size, jobIds = jobIds))
        }
        if (path == "/api/v1/user/contacts" && method == HttpMethod.Get) {
            return scope.json(contacts.filter { it.userId == me.id }.map { it.toResponse() })
        }
        if (path == "/api/v1/user/contacts" && method == HttpMethod.Post) {
            val req = decodeBody<ContactCreateRequest>(request)
            val c = DContact(nextId++, me.id, req.phoneNumber, req.name)
            contacts += c
            return scope.json(c.toResponse())
        }
        if (seg.size == 5 && seg[3] == "contacts" && seg[4].toIntOrNull() != null) {
            val id = seg[4].toInt()
            val idx = contacts.indexOfFirst { it.id == id && it.userId == me.id }
            when (method) {
                HttpMethod.Get -> {
                    if (idx < 0) return scope.error(HttpStatusCode.NotFound, "Контакт не найден")
                    return scope.json(contacts[idx].toResponse())
                }
                HttpMethod.Patch -> {
                    if (idx < 0) return scope.error(HttpStatusCode.NotFound, "Контакт не найден")
                    val req = decodeBody<ContactUpdateRequest>(request)
                    val upd = contacts[idx].copy(name = req.name ?: contacts[idx].name, phoneNumber = req.phoneNumber ?: contacts[idx].phoneNumber)
                    contacts[idx] = upd
                    return scope.json(upd.toResponse())
                }
                HttpMethod.Delete -> {
                    if (idx >= 0) contacts.removeAt(idx)
                    return scope.empty()
                }
            }
        }
        if (path == "/api/v1/user/contact-groups" && method == HttpMethod.Get) {
            return scope.json(groups.filter { it.userId == me.id }.map { it.toResponse() })
        }
        if (path == "/api/v1/user/contact-groups" && method == HttpMethod.Post) {
            val req = decodeBody<ContactGroupCreateRequest>(request)
            val g = DGroup(nextId++, me.id, req.name)
            groups += g
            return scope.json(g.toResponse())
        }
        if (seg.size >= 5 && seg[3] == "contact-groups" && seg[4].toIntOrNull() != null) {
            val gid = seg[4].toInt()
            val idx = groups.indexOfFirst { it.id == gid && it.userId == me.id }
            if (seg.size == 6 && seg[5] == "members" && method == HttpMethod.Put) {
                if (idx < 0) return scope.error(HttpStatusCode.NotFound, "Группа не найдена")
                val req = decodeBody<ContactGroupMembersRequest>(request)
                members.removeAll { it.groupId == gid }
                members += req.contactIds.map { DMember(gid, it) }
                return scope.json(groups[idx].toResponse())
            }
            when (method) {
                HttpMethod.Patch -> {
                    if (idx < 0) return scope.error(HttpStatusCode.NotFound, "Группа не найдена")
                    val req = decodeBody<ContactGroupUpdateRequest>(request)
                    val upd = groups[idx].copy(name = req.name ?: groups[idx].name)
                    groups[idx] = upd
                    return scope.json(upd.toResponse())
                }
                HttpMethod.Delete -> {
                    if (idx >= 0) groups.removeAt(idx)
                    members.removeAll { it.groupId == gid }
                    return scope.empty()
                }
            }
        }
        if (path == "/api/v1/user/me/sims" && method == HttpMethod.Get) {
            return scope.json(sims.filter { it.assignedUserId == me.id }.map { it.toResponse() })
        }
        if (seg.size == 6 && seg[3] == "me" && seg[4] == "sims" && method == HttpMethod.Patch) {
            val sid = seg[5].toIntOrNull() ?: return scope.error(HttpStatusCode.NotFound, "SIM не найдена")
            val sim = sims.firstOrNull { it.id == sid } ?: return scope.error(HttpStatusCode.NotFound, "SIM не найдена")
            // В демо подпись не персистим, просто отражаем запрошенное значение.
            val req = decodeBody<SimCardUpdateRequest>(request)
            return scope.json(sim.toResponse().copy(label = req.label ?: sim.label))
        }
        if (path == "/api/v1/user/templates" && method == HttpMethod.Get) {
            return scope.json(templates.filter { it.userId == me.id || it.isGlobal }.map { it.toResponse() })
        }
        if (path == "/api/v1/user/templates" && method == HttpMethod.Post) {
            val req = decodeBody<TemplateCreateRequest>(request)
            val t = DTemplate(nextId++, req.name, req.content, req.category, req.isGlobal, me.id)
            templates += t
            return scope.json(t.toResponse())
        }
        if (seg.size == 5 && seg[3] == "templates" && seg[4].toIntOrNull() != null) {
            val tid = seg[4].toInt()
            val idx = templates.indexOfFirst { it.id == tid }
            when (method) {
                HttpMethod.Patch -> {
                    if (idx < 0) return scope.error(HttpStatusCode.NotFound, "Шаблон не найден")
                    val req = decodeBody<TemplateUpdateRequest>(request)
                    val upd = templates[idx].copy(
                        name = req.name ?: templates[idx].name,
                        content = req.content ?: templates[idx].content,
                        isGlobal = req.isGlobal ?: templates[idx].isGlobal,
                    )
                    templates[idx] = upd
                    return scope.json(upd.toResponse())
                }
                HttpMethod.Delete -> {
                    if (idx >= 0) templates.removeAt(idx)
                    return scope.empty()
                }
            }
        }

        // --- Admin scope (read-only, для офлайн-входа под admin) ---
        if (path == "/api/v1/admin/settings/registration-mode" && method == HttpMethod.Get) {
            return scope.json(RegistrationModeResponse(mode = "closed"))
        }
        if (path == "/api/v1/admin/pending-registrations" && method == HttpMethod.Get) {
            return scope.json(emptyList<PendingRegistrationResponse>())
        }
        if (path == "/api/v1/admin/gateways" && method == HttpMethod.Get) {
            return scope.json(gateways.map { it.toResponse() })
        }
        if (path == "/api/v1/admin/users" && method == HttpMethod.Get) {
            return scope.json(users.map { UserResponse(it.id, it.username, enumValue(it.role), isActive = it.isActive) })
        }
        if (path == "/api/v1/admin/messages" && method == HttpMethod.Get) {
            return scope.json(messages.sortedByDescending { it.createdAt }.take(50).map { it.toAdminResponse() })
        }

        return scope.error(HttpStatusCode.NotFound, "Не поддерживается в офлайн-демо: ${method.value} $path")
    }

    private fun dialogsFor(userId: Int): List<DialogSummary> {
        val mine = userSimIds(userId)
        return messages.filter { it.simCardId in mine }
            .groupBy { it.externalPhone }
            .map { (phone, list) ->
                val last = list.maxBy { it.createdAt }
                DialogSummary(externalPhone = phone, lastText = last.text, lastAt = last.createdAt, lastDirection = dirValue(last.direction))
            }
            .sortedByDescending { it.lastAt ?: "" }
    }

    private fun DMessage.toResponse() = MessageResponse(id, simCardId, externalPhone, dirValue(direction), text, enumValue(status), createdAt)
    private fun DMessage.toAdminResponse(): AdminMessageResponse {
        val sim = sims.firstOrNull { it.id == simCardId }
        val owner = sim?.assignedUserId?.let { uid -> users.firstOrNull { it.id == uid } }
        return AdminMessageResponse(id, simCardId, sim?.label, owner?.username, externalPhone, dirValue(direction), text, enumValue(status), createdAt)
    }
    private fun DContact.toResponse() = ContactResponse(id, name, phoneNumber)
    private fun DGroup.toResponse(): ContactGroupResponse {
        val ids = members.filter { it.groupId == id }.map { it.contactId }
        return ContactGroupResponse(id, name, ids, ids.size)
    }
    private fun DSim.toResponse() = SimCardResponse(id, portNumber, phoneNumber, label, status, gatewayId, assignedUserId)
    private fun DTemplate.toResponse() = TemplateResponse(id, name, content, category, isGlobal)
    private fun DGateway.toResponse() = GatewayResponse(id, name, enumValue(type), host, port, username, isActive, null, lastStatus)
}

// --- Хелперы запроса/ответа ---

private fun bodyText(request: HttpRequestData): String = when (val b = request.body) {
    is TextContent -> b.text
    is OutgoingContent.ByteArrayContent -> b.bytes().decodeToString()
    else -> ""
}

private inline fun <reified T> decodeBody(request: HttpRequestData): T =
    demoJson.decodeFromString(bodyText(request))

private fun decode(s: String): String = s.replace("+", " ")
    .replace(Regex("%([0-9A-Fa-f]{2})")) { it.groupValues[1].toInt(16).toChar().toString() }

private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

private inline fun <reified T> MockRequestHandleScope.json(body: T, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
    respond(demoJson.encodeToString(body), status, jsonHeaders)

private fun MockRequestHandleScope.error(status: HttpStatusCode, detail: String): HttpResponseData =
    respond(demoJson.encodeToString(ApiErrorDetail(detail)), status, jsonHeaders)

private fun MockRequestHandleScope.unauthorized(): HttpResponseData =
    respond(demoJson.encodeToString(ApiErrorDetail("Требуется авторизация")), HttpStatusCode.Unauthorized, jsonHeaders)

private fun MockRequestHandleScope.empty(): HttpResponseData =
    respond("", HttpStatusCode.NoContent, jsonHeaders)
