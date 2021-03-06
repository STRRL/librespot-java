# librespot-api
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/xyz.gianlu.librespot/librespot-api/badge.svg)](https://maven-badges.herokuapp.com/maven-central/xyz.gianlu.librespot/librespot-api)

This module depends on `librespot-core` and provides an API to interact with the Spotify client.

## Available endpoints

All the endpoints will respond with `200` if successful or `204` if there isn't any active session.

### Player
- `POST \player\load` Load a track from a given uri. The request body should contain two parameters: `uri` and `play`.
- `POST \player\pause` Pause playback.
- `POST \player\resume` Resume playback.
- `POST \player\next` Skip to next track.
- `POST \player\prev` Skip to previous track.
- `POST \player\set-volume` Set volume to a given `volume` value from 0 to 65536.
- `POST \player\volume-up` Up the volume a little bit.
- `POST \player\volume-down` Lower the volume a little bit.
- `POST \player\current` Retrieve information about the current track (metadata and time).

### Metadata
- `POST \metadata\{type}\{uri}` Retrieve metadata. `type` can be one of `episode`, `track`, `album`, `show`, `artist` or `playlist`, `uri` is the standard Spotify uri.

### Search
- `POST \search\{query}` Make a search.

### Tokens
- `POST \token\{scope}` Request an access token for a specific scope.

### Events

You can subscribe for players events by creating a WebSocket connection to `/events`.
The currently available events are:
- `contextChanged`
- `trackChanged`
- `playbackPaused`
- `playbackResumed`
- `trackSeeked`
- `metadataAvailable`
- `playbackHaltStateChanged`
- `sessionCleared`
- `sessionChanged`
- `inactiveSession`

## Examples

`curl -X POST -d "uri=spotify:track:xxxxxxxxxxxxxxxxxxxxxx&play=true" http://localhost:24879/player/load`

`curl -X POST http://localhost:24879/metadata/track/spotify:track:xxxxxxxxxxxxxxxxxxxxxx`
