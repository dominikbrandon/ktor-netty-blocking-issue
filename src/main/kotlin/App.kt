import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.time.Duration

fun main() {
    val logger = LoggerFactory.getLogger("main")
    val client = Client()
    embeddedServer(Netty, port = 8080, configure = {
        connectionGroupSize = 1
        workerGroupSize = 1
        callGroupSize = 1
    }) {
        routing {
            get("/status/health") {
                logger.info("health check")
                call.respondText(status = HttpStatusCode.OK) { "" }
            }
            get("/long-call") {
                withContext(Dispatchers.IO) {
                    val responseBody: String = client.veryLongCall()
                    call.respond(responseBody)
                }
            }
        }
    }.start(wait = true)
}

class Client {
    companion object {
        private val logger = LoggerFactory.getLogger(Client::class.java)
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(Duration.ofSeconds(30))
        .build()

    fun veryLongCall(): String {
        logger.info("Sending request")
        val req = Request.Builder()
            .url("https://httpbin.org/delay/10")
            .get()
            .build()
        client.newCall(req).execute().use {
            return it.body!!.string()
        }
    }
}