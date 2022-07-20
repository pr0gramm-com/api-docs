#!/usr/bin/env node

const { AuthorizationCode } = require("simple-oauth2");
const config = {
	client: {
		id: "<client_id>",
		secret: "<client_secret>",
	},
	auth: {
		tokenHost: "https://pr0gramm.com/",
		tokenPath: "/api/oauth/createAccessToken",
		authorizePath: "/oauth/authorize",
	}
};

const client = new AuthorizationCode(config);

const authorizationUri = client.authorizeURL({
	redirect_uri: "https://kellerbay.com/login/callback",
	scope: "user.me",
	state: "<state>", // you have to generate state
});


console.log("Go to:");
console.log(authorizationUri);
console.log();

// Redirect the user to `authorizationUri`
// Examples of simple-oauth2 show how, for example:
// https://github.com/lelylan/simple-oauth2/blob/511e7a44a8e2a2cc7f07ca435e0130d0e3a401c2/example/github.js

// In your create a handler for redirect_uri (`https://kellerbay.com/login/callback?code=<random-code>`):
const tokenResponse = await client.getToken({
	redirect_uri: "https://kellerbay.com/login/callback",
	code, // value from query params (`?code=<random-code>`)
}, {
	headers: {
		"User-Agent": "simple-oauth2/1.0", // Cloudflare wants a User-Agent, otherwise they will block this request with 403
	},
});
// ...also check the state!

// your task: save the token for that user securely

// Using the token to make API requests:
const userInfo = await fetch("https://pr0gramm.com/api/user/me", {
	headers: {
		Authorization: `Bearer ${tokenResponse.token.access_token}`
	}
}).then(r => r.json());

console.log(`Hallo ${userInfo.name}`);
