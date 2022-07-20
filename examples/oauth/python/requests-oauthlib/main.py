#!/usr/bin/env python3

from requests_oauthlib import OAuth2Session

client_id = r'<client_id>'
client_secret = r'<client_secret>'
redirect_uri = 'https://localhost:9090/login'
scope = [
	'user.me',
]

oauth = OAuth2Session(
	client_id,
	redirect_uri=redirect_uri,
	scope=scope,
)

authorization_url, state = oauth.authorization_url(f'https://pr0gramm.com/oauth/authorize')

print(f'Please go to:')
print(authorization_url)
print()

authorization_response = input('Enter the full callback URL:\n')

token = oauth.fetch_token(
	f'https://pr0gramm.com/api/oauth/createAccessToken',
	client_secret=client_secret,
	authorization_response=authorization_response,
)

# your task: save the token for that user securely

response = oauth.get(f'https://pr0gramm.com/api/user/me').json()
print(f'Hallo {response["name"]}!')
