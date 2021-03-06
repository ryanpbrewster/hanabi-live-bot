package net.rmelick.hanabi.bot.live.connector;

import java.io.IOException;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

class WebsocketListener implements WebSocket.Listener {
    private static final Logger LOG = Logger.getLogger(WebsocketListener.class.getName());

    private final AbstractHanabiClient _client;
    private StringBuffer _incomingText;
    private final ExecutorService _executor = Executors.newFixedThreadPool(5);

    public WebsocketListener(AbstractHanabiClient client) {
        _client = client;
        _incomingText = new StringBuffer();
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket,
                                     CharSequence data, boolean last) {
        _incomingText.append(data);

        // check jdk.httpclient.websocket properties to see if we can set them so we don't need this silly aggregating
        if (!last) {
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        final String fullData = _incomingText.toString();
        _incomingText = new StringBuffer();

        _executor.submit(() -> processFullText(fullData));

        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    private void processFullText(String fullData) {
        CommandParser.ParsedCommand command = CommandParser.parseCommand(fullData);
        try {
            if (command != null) {
                boolean success = _client.handleCommand(command.command, command.body);
                if (!success) {
                    LOG.warning(String.format("Client %s failed to handle command %s", _client, command.command));
                }
            } else {
                LOG.warning("Empty command, fullData: " + fullData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        LOG.info("onOpen");
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode,
                                      String reason) {
        // TODO add complete client refresh if we lose the player or spectator
        LOG.info("onClose: " + statusCode + " " + reason);
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        LOG.warning("onError: " + error);
        error.printStackTrace();
        WebSocket.Listener.super.onError(webSocket, error);
    }
}
