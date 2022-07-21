package com.example.pr0auth;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class MainVerticle extends AbstractVerticle {

  private final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) {
    var server = vertx.createHttpServer();
    var router = Router.router(vertx);
    var client = WebClient.create(vertx, new WebClientOptions().setUserAgent("vertx-auth-oauth2"));
    var port = 8888;

    // Normally you would extract this code into a OAuth2Auth provider class. See for example:
    // https://github.com/vert-x3/vertx-auth/blob/master/vertx-auth-oauth2/src/main/java/io/vertx/ext/auth/oauth2/providers/GithubAuth.java
    var credentials = new OAuth2Options()
      .setFlow(OAuth2FlowType.AUTH_CODE)
      .setClientId("<client id>")
      .setClientSecret("<client secret>")
      .setSite("https://pr0gramm.com")
      .setTokenPath("/api/oauth/createAccessToken")
      .setAuthorizationPath("/oauth/authorize")
      .setHeaders(new JsonObject()
        .put("User-Agent", "vertx-auth-oauth2")); // The User-Agent is necessary to get pass CloudFlare

    var oauth2 = OAuth2Auth.create(vertx, credentials);

    var authorization_uri = oauth2.authorizeURL(new JsonObject()
      .put("redirect_uri", "http://localhost:" + port + "/callback")
      .put("scope", "user.me")
      .put("state", "insert random state here"));

    router.route("/").handler(ctx -> {
      var response = ctx.response();
      response.putHeader("content-type", "text/html");
      response.end("Hi!<br><a href=/auth>Log in with pr0gramm</a>");
    });

    router.route("/auth").handler(ctx -> {
      var response = ctx.response();
      response.putHeader("Location", authorization_uri)
        .setStatusCode(302)
        .end();
    });

    router.route("/callback").handler(ctx -> {
      var authCode = ctx.request().getParam("code");
      // Note that the redirectUri parameter is not in snake_case
      var tokenRequest = new JsonObject()
        .put("code", authCode)
        .put("redirectUri", "http://localhost:" + port + "/callback");
      var response = ctx.response();

      oauth2.authenticate(tokenRequest)
        .onSuccess(user -> {
          LOGGER.info("User Principal: " + user.principal().toString());
          String token = user.get("access_token");

          client.getAbs("https://pr0gramm.com/api/user/me")
            .putHeader("Authorization", "Bearer " + token)
            .send()
            .onSuccess(res -> {
              var userInfo = res.bodyAsJsonObject();
              LOGGER.info("User Info: " + userInfo.toString());
              response.end(userInfo.toString());
            })
            .onFailure(err -> {
              LOGGER.error("Error: " + err.getMessage());
              response.end("Error: " + err.getMessage());
            });
        })
        .onFailure(err -> {
          LOGGER.error("Access Token Error: " + err.getMessage(), err);
          response.end();
        });
    });

    server.requestHandler(router).listen(port, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        LOGGER.info("HTTP server started on port " + port);
      } else {
        startPromise.fail(http.cause());
      }
    });
  }
}
