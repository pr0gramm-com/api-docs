#!/usr/bin/env node

const app = require("express")();
const { AuthorizationCode } = require("simple-oauth2");

const clientId = "<client_id>";
const clientSecret = "<client_secret>";

const port = 9090;
app.listen(port, (err) => {
    if (err) return console.error(err);
    console.log(`Express server listening at http://localhost:${port}`);

    const client = new AuthorizationCode({
        client: {
            id: clientId,
            secret: clientSecret,
        },
        auth: {
            tokenHost: "https://pr0gramm.com",
            tokenPath: "/api/oauth/createAccessToken",
            authorizePath: "/oauth/authorize",
        },
    });

    // Authorization uri definition
    const authorizationUri = client.authorizeURL({
        redirect_uri: "http://localhost:3000/callback",
        scope: "user.me",
        state: "insert random state here",
    });

    // Initial page redirecting to pr0gramm
    app.get("/auth", (req, res) => {
        console.log(authorizationUri);
        res.redirect(authorizationUri);
    });

    // Callback service parsing the authorization token and asking for the access token
    app.get("/callback", async (req, res) => {
        try {
            const accessToken = await client.getToken({
                redirect_uri: "http://localhost:3000/callback",
                code: req.query.code,
            }, {
                headers: {
                    "User-Agent": "simple-oauth2/1.0", // Cloudflare wants a User-Agent, otherwise they will block this request with 403
                },
            });

            console.log("The resulting token: ", accessToken.token);

            const userInfo = await fetch(`https://pr0gramm.com/api/user/me`, {
                 headers: {
                    "Authorization": `Bearer ${accessToken.token.access_token}`,
                },
            }).then(r => r.json());

            console.log(userInfo);

            return res.send(`Hallo ${userInfo.name} :)`);
        } catch (error) {
            console.error("Access Token Error", error.message);
            return res.status(500).json("Authentication failed");
        }
    });

    app.get("/", (req, res) => {
        res.send("Hi!<br><a href=/auth>Log in with pr0gramm</a>");
    });
});
