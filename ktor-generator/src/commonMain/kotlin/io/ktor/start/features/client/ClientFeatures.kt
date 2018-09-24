package io.ktor.start.features.client

import io.ktor.start.*
import io.ktor.start.features.server.*
import io.ktor.start.project.*
import io.ktor.start.util.*

object AuthBasicClientFeature : ClientFeature(CoreClientEngine) {
    override val id = "ktor-client-auth-basic"
    override val title = "Auth Basic feature HttpClient"
    override val description = "Supports basic authentication for the Http Client"
    override val documentation = "https://ktor.io/clients/http-client.html#basicauth"

    override fun BlockBuilder.renderFeature(info: BuildInfo) {
        addImport("io.ktor.client.features.json.*")
        addImport("io.ktor.client.features.auth.basic.*")
        append(CoreClientEngine.CLIENT_FEATURES) {
            +"install(BasicAuth)" {
                +"username = \"test\""
                +"password = \"pass\""
            }
        }
    }
}

object JsonClientFeature : ClientFeature(CoreClientEngine, ApplicationKt) {
    override val id = "ktor-client-json-jvm"
    override val title = "Json serialization for HttpClient"
    override val description = "Supports basic authentication for the Http Client"
    override val documentation = "https://ktor.io/clients/http-client.html#jsonfeature"
    override val repos = super.repos + listOf("https://kotlin.bintray.com/kotlinx")
    override val artifacts = listOf(
        "io.ktor:ktor-client-json-jvm:\$ktor_version",
        "io.ktor:ktor-client-gson:\$ktor_version"
    )

    override fun BlockBuilder.renderFeature(info: BuildInfo) {
        addImport("io.ktor.client.features.json.*")
        addImport("io.ktor.client.request.*")
        addImport("java.net.URL")
        addImport("kotlinx.coroutines.experimental.*")
        addApplicationClasses {
            +"data class JsonSampleClass(val hello: String)"
        }
        append(CoreClientEngine.CLIENT_FEATURES) {
            "install(JsonFeature)" {
                +"serializer = GsonSerializer()"
            }
        }
        append(CoreClientEngine.CLIENT_USAGE) {
            "runBlocking" {
                +"// Sample for making a HTTP Client request"
                +"/*"
                "val message = client.post<JsonSampleClass>" {
                    +"url(URL(\"http://127.0.0.1:8080/path/to/endpoint\"))"
                    +"contentType(ContentType.Application.Json)"
                    +"body = JsonSampleClass(hello = \"world\")"
                }
                +"*/"
            }
        }
    }
}

object WebSocketClientFeature : ClientFeature(CoreClientEngine, CioClientEngine, WebsocketsFeature) {
    override val id = "ktor-client-websocket"
    override val title = "WebSockets HttpClient support"
    override val description = "HttpClient feature to establish bidirectional communication using WebSockets"
    override val documentation = "https://ktor.io/clients/http-client.html#websockets"

    override fun BlockBuilder.renderFeature(info: BuildInfo) {
        addImport("io.ktor.client.*")
        addImport("io.ktor.client.features.websocket.*")
        addImport("io.ktor.client.features.websocket.WebSockets")
        addImport("io.ktor.http.*")
        addImport("io.ktor.http.cio.websocket.*")
        addImport("io.ktor.http.cio.websocket.Frame")
        addImport("kotlinx.coroutines.experimental.*")
        addImport("kotlinx.coroutines.experimental.channels.*")

        fileText("src/WsClientApp.kt") {
            putImports(applicationKtImports)
            SEPARATOR {
                +"object WsClientApp" {
                    +"@JvmStatic"
                    +"fun main(args: Array<String>)" {
                        +"runBlocking" {
                            +"val client = HttpClient(CIO).config { install(WebSockets) }"

                            +"client.ws(method = HttpMethod.Get, host = \"127.0.0.1\", port = 8080, path = \"/myws/echo\")" {
                                // this: WebSocketSession"
                                +"send(Frame.Text(\"Hello World\"))"
                                +"for (message in incoming.map { it as? Frame.Text }.filterNotNull())" {
                                    +"println(\"Server said: \" + message.readText())"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}