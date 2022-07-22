package com.example

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.call
import io.ktor.server.auth.*
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
  embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
    authentication {
      oauth("pr0auth") {
        urlProvider = { "http://localhost:8080/callback" }
        providerLookup = {
          OAuthServerSettings.OAuth2ServerSettings(
            name = "pr0auth",
            authorizeUrl = "https://pr0gramm.com/oauth/authorize",
            accessTokenUrl = "https://pr0gramm.com/api/oauth/createAccessToken",
            requestMethod = HttpMethod.Post,
            clientId = "<< client id >>",
            clientSecret = "<< client secret >>",
            defaultScopes = listOf("user.me")
          )
        }
        client = HttpClient(Apache) {
          defaultRequest {
            header("User-Agent", "Ktor/1.0.0")
          }
        }
      }
    }

    routing {

      get("/") {
          call.respondText("Hi!<br><a href=/auth>Log in with pr0gramm</a>", ContentType.Text.Html)
      }

      authenticate("pr0auth") {
        get("auth") {
          call.respondRedirect("/callback")
        }

        get("/callback") {
          val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
          if (principal != null) {
            val client = HttpClient(Apache)
            call.respondText {
              client.get("https://pr0gramm.com/api/user/me") {
                headers {
                  append(HttpHeaders.Authorization, "Bearer ${principal.accessToken}")
                }
              }.body()
            }
          } else {
            call.respondText("Not authenticated")
          }
        }
      }
    }
  }.start(wait = true)
}
