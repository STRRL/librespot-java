package xyz.gianlu.librespot.dealer;

import com.google.protobuf.Message;
import com.spotify.connectstate.model.Connect;
import com.spotify.metadata.proto.Metadata;
import okhttp3.*;
import okio.BufferedSink;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.gianlu.librespot.core.ApResolver;
import xyz.gianlu.librespot.core.Session;
import xyz.gianlu.librespot.debug.PutStateDebugger;
import xyz.gianlu.librespot.mercury.MercuryClient;
import xyz.gianlu.librespot.mercury.model.EpisodeId;
import xyz.gianlu.librespot.mercury.model.TrackId;

import java.io.IOException;

/**
 * @author Gianlu
 */
public class ApiClient {
    private static final Logger LOGGER = Logger.getLogger(ApiClient.class);
    private final Session session;
    private final String baseUrl;

    public ApiClient(@NotNull Session session) {
        this.session = session;
        this.baseUrl = "https://" + ApResolver.getRandomSpclient();
    }

    @NotNull
    private static RequestBody protoBody(@NotNull Message msg) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.get("application/protobuf");
            }

            @Override
            public void writeTo(@NotNull BufferedSink sink) throws IOException {
                sink.write(msg.toByteArray());
            }
        };
    }

    @NotNull
    private Request buildRequest(@NotNull String method, @NotNull String suffix, @Nullable Headers headers, @Nullable RequestBody body) throws IOException, MercuryClient.MercuryException {
        Request.Builder request = new Request.Builder();
        request.method(method, body);
        if (headers != null) request.headers(headers);
        request.addHeader("Authorization", "Bearer " + session.tokens().get("playlist-read"));
        request.url(baseUrl + suffix);
        return request.build();
    }

    public void sendAsync(@NotNull String method, @NotNull String suffix, @Nullable Headers headers, @Nullable RequestBody body, @NotNull Callback callback) throws IOException, MercuryClient.MercuryException {
        session.client().newCall(buildRequest(method, suffix, headers, body)).enqueue(callback);
    }

    /**
     * Sends a request to the Spotify API.
     *
     * @param method  The request method
     * @param suffix  The suffix to be appended to {@link #baseUrl} also know as path
     * @param headers Additional headers
     * @param body    The request body
     * @param tries   How many times the request should be reattempted (0 = none)
     * @return The response
     * @throws IOException                    The last {@link IOException} thrown by {@link Call#execute()}
     * @throws MercuryClient.MercuryException If the API token couldn't be requested
     */
    @NotNull
    public Response send(@NotNull String method, @NotNull String suffix, @Nullable Headers headers, @Nullable RequestBody body, int tries) throws IOException, MercuryClient.MercuryException {
        IOException lastEx;
        do {
            try {
                return session.client().newCall(buildRequest(method, suffix, headers, body)).execute();
            } catch (IOException ex) {
                lastEx = ex;
            }
        } while (tries-- > 0);

        throw lastEx;
    }

    @NotNull
    public Response send(@NotNull String method, @NotNull String suffix, @Nullable Headers headers, @Nullable RequestBody body) throws IOException, MercuryClient.MercuryException {
        return send(method, suffix, headers, body, 0);
    }

    public void putConnectState(@NotNull String connectionId, @NotNull Connect.PutStateRequest proto) throws IOException, MercuryClient.MercuryException {
        int code = PutStateDebugger.registerRequest(proto);

        try (Response resp = send("PUT", "/connect-state/v1/devices/" + session.deviceId(), new Headers.Builder()
                .add("X-Spotify-Connection-Id", connectionId).build(), protoBody(proto))) {
            if (resp.code() != 200) LOGGER.warn(String.format("PUT %s returned %d", resp.request().url(), resp.code()));
            PutStateDebugger.registerResponse(code, resp);
        }
    }

    @NotNull
    public Metadata.Track getMedata4Track(@NotNull TrackId track) throws IOException, MercuryClient.MercuryException {
        try (Response resp = send("GET", "/metadata/4/track/" + track.hexId(), null, null)) {
            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return Metadata.Track.parseFrom(body.byteStream());
        }
    }

    @NotNull
    public Metadata.Episode getMedata4Episode(@NotNull EpisodeId episode) throws IOException, MercuryClient.MercuryException {
        try (Response resp = send("GET", "/metadata/4/episode/" + episode.hexId(), null, null)) {
            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return Metadata.Episode.parseFrom(body.byteStream());
        }
    }
}
