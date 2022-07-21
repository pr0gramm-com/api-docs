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

public class MainVerticle extends AbstractVerticle {

  private final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) {
    var server = vertx.createHttpServer();
    var router = Router.router(vertx);
    var port = 8888;

    var credentials = new OAuth2Options()
      .setFlow(OAuth2FlowType.AUTH_CODE)
      .setClientId("<client-id>")
      .setClientSecret("<client-secret>")
      .setSite("https://pr0gramm.com")
      .setTokenPath("/api/oauth/createAccessToken")
      .setAuthorizationPath("/oauth/authorize")
      .setHeaders(new JsonObject()
        .put("User-Agent", "vertx-auth-oauth2"));

    var oauth2 = OAuth2Auth.create(vertx, credentials);

    var authorization_uri = oauth2.authorizeURL(new JsonObject()
      .put("redirectUri", "http://localhost:" + port + "/callback")
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
      var token = new JsonObject()
        .put("code", authCode)
        .put("redirect_uri", "http://localhost:" + port + "/callback");
      var response = ctx.response();
      oauth2.authenticate(token)
        .onSuccess(user -> {
          LOGGER.info("User: " + user.principal().toString());
          response.end();
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
