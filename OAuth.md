# OAuth 2.0
pr0gramm.com supports OAuth 2.0 using *Grant Code Authorization* to do API requests.

This page uses the hypothetical application `kellerbay.com` as a placeholder.
Everything that relates to Kellerbay needs to be replaced by your application.

## Table of Contents
- [OAuth 2.0](#oauth-20)
	- [Table of Contents](#table-of-contents)
	- [You'll need](#youll-need)
		- [Scopes](#scopes)
	- [Example Integrations](#example-integrations)
		- [Node.js](#nodejs)
			- [`simple-oauth2`](#simple-oauth2)
		- [Python](#python)
		- [Laravel (PHP)](#laravel-php)
		- [Passport.js (Node)](#passportjs-node)
		- [Your Example](#your-example)

## You'll need
| Name                | Description                                                     | Where to get                                      | Secret? | Format        | Remarks |
| ------------------- | --------------------------------------------------------------- | ------------------------------------------------- | ------- | ------------- | ------- |
| `client_id`         | ID of your client/application                                   | [/contact](https://pr0gramm.com/contact) **see note below** | No      | `^\d+$`       |         |
| `client_secret`     | Secret needed for access token creation                         | Comes along with `client_id`                      | Yes!    | `^[0-9a-f]+$` |         |
| `access_token_uri`  | `https://pr0gramm.com/api/oauth/createAccessToken`              | <--                                               | No      | URL           |         |
| `authorization_uri` | `https://pr0gramm.com/oauth/authorize`                          | <--                                               | No      | URL           |         |
| `redirect_uri`      | The URL that user will return to; `https://kellerbay.com/login` | Your choice                                       | No      | URL           |         |
| `scopes`            | The access that your application is requesting; e.g. `user.me`  | Your choice, see below                            | No      |               |         |


You'll also need to generate a random `state` and check whether it is the same after getting the user back from pr0gramm. [This is important](https://stackoverflow.com/questions/26132066).


> [!IMPORTANT]
> When requesting a client ID and a client secret, include this information in your initial request:
> - **Display Name** - What the user will see as Application name
> - **Public Description** - What the user should see on their app list
> - **URL** - A URL to the service - e.g. `https://kellerbay.com`
> - **Logo URL** - A URL to a logo, which is >= 64x64px
> - **Callback URL prefix** - The prefix to the allowed callback URLs. For example, `https://kellerbay.com/auth`. https is **required**. **localhost** is always allowed.
>
> If one of these is missing, we cannot proceed.

### Scopes
There are many scopes. Every API endpoint is represented by a scope. For example `/api/a/b` has the scope `a.b`. The most important scopes are listed here:
| Name      | Description                                                                                                     | Endpoint       |
| --------- | --------------------------------------------------------------------------------------------------------------- | -------------- |
| `user.me` | Gets the user identifier (constant across renames), ban information and other public information about the user | `/api/user/me` |

**You should probably use `user.me`**, since this is the only endpoint that will be accessible with the `access_token` in case the user is banned.
You can check the `banInfo` field if and how long the user is banned.

Cloudflare blocks all API requests that don't have a User-Agent with error 403. Make sure to specify a User-Agent header when doing requests. This also applies to the OAuth library you are using.
You don't need to supply a valid browser agent, just be honest about what your application is doing.

## Example Integrations
You can also find these (and more!) integration samples in [`examples/oauth`](examples/oauth).
### Node.js

#### [`simple-oauth2`](https://www.npmjs.com/package/simple-oauth2)
```js
const { AuthorizationCode } = require("simple-oauth2");
const config = {
	client: {
		// See above
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
	state: "<state>", // you have to generate state, see note above
});

// Redirect the user to `authorizationUri`
// Examples of simple-oauth2 show how, for example:
// https://github.com/lelylan/simple-oauth2/blob/511e7a44a8e2a2cc7f07ca435e0130d0e3a401c2/example/github.js

// In your handler for redirect_uri (`https://kellerbay.com/login/callback?code=<random-code>`):
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
const userInfo = await fetch("https://pr0gramm.com/api/user/name", {
	headers: {
		Authorization: `Bearer ${tokenResponse.token.access_token}`
	}
}).then(r => r.json());

console.log(`Hallo ${userInfo.name}`);
```

### Python
[`requests-oauthlib`](https://github.com/requests/requests-oauthlib) (OAuth support for the `requests` library)
```py
#!/usr/bin/env python3
from requests_oauthlib import OAuth2Session
# pip3 install --user requests_oauthlib

client_id = r'<client_id>'
client_secret = r'<client_secret>'
redirect_uri = 'https://kellerbay.com/login/callback'
scope = ['user.me']

oauth = OAuth2Session(
	client_id,
	redirect_uri=redirect_uri,
	scope=scope,
)

authorization_url, state = oauth.authorization_url(f'https://pr0gramm.com/oauth/authorize')

print(f'Please go to:')
print(authorization_url)

authorization_response = input('Enter the full callback URL:\n')

token = oauth.fetch_token(
	'https://pr0gramm.com/api/oauth/createAccessToken',
	client_secret=client_secret,
	authorization_response=authorization_response,
)

# your task: save the token for that user securely

# Example authenticated API call
response = oauth.get('https://pr0gramm.com/api/user/name').json()
print(f'Hallo {response["name"]}')
```

### Laravel (PHP)

[`Pr0gramm Socialite Provider`](https://github.com/SocialiteProviders/Providers/tree/master/src/Pr0gramm) (Requires [`Laravel Socialite`](https://laravel.com/docs/9.x/socialite))

Read the official documentation on the website ([`Socialite Providers`](https://socialiteproviders.com/Pr0gramm/)) for installation instructions.

## Passport.js (Node)
[passport-pr0gramm](https://github.com/holzmaster/passport-pr0gramm), an auth strategy for [Passport.js](https://www.passportjs.org/). Can be used with Express, Fastify, Restify and all other frameworks that support Passport.js.

### Your Example
Are you using a different library? We're open for contributions and love to get more samples!
