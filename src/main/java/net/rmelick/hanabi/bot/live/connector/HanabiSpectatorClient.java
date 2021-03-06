package net.rmelick.hanabi.bot.live.connector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.rmelick.hanabi.bot.ieee.LiveGameRunner;
import net.rmelick.hanabi.bot.live.connector.schemas.java.Hello;
import net.rmelick.hanabi.bot.live.connector.schemas.java.Init;
import net.rmelick.hanabi.bot.live.connector.schemas.java.Notify;
import net.rmelick.hanabi.bot.live.connector.schemas.java.Ready;
import net.rmelick.hanabi.bot.live.connector.schemas.java.Table;
import net.rmelick.hanabi.bot.live.connector.schemas.java.TableSpectate;
import net.rmelick.hanabi.bot.live.connector.schemas.java.TableStart;
import net.rmelick.hanabi.bot.live.connector.schemas.java.Warning;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * A client that observes a single game
 * This should make it much easier to adapt the gamestate since we can get perfect information
 */
public class HanabiSpectatorClient extends AbstractHanabiClient {
    private static final Logger LOG = Logger.getLogger(HanabiSpectatorClient.class.getName());

    private final ObjectMapper _objectMapper = new ObjectMapper();
    private final AtomicBoolean _alreadySpectating = new AtomicBoolean(false);
    private final Long _gameID;
    private final LiveGameRunner _liveGameRunner;

    public HanabiSpectatorClient(String username, String userPassword, Long gameID, LiveGameRunner liveGameRunner) {
        super(username, userPassword);
        _gameID = gameID;
        _liveGameRunner = liveGameRunner;
    }

    public void init() throws IOException, InterruptedException {
        super.connectWebsocket();

    }

    @Override
    public boolean handleCommand(String command, String body) throws IOException {
        LOG.info(String.format("Received command %s %s", command, body));
        switch (command) {
            case "table":
                return handleTableUpdate(_objectMapper.readValue(body, Table.class));
            case "tableStart":
                return handleTableStart(_objectMapper.readValue(body, TableStart.class));
            case "tableList":
                return handleTableList(_objectMapper.readValue(body, new TypeReference<List<Table>>() {}));
            case "init":
                return handleInit(_objectMapper.readValue(body, Init.class));
            case "action":
                return handleAction();
            case "notify":
                return handleNotify(_objectMapper.readValue(body, Notify.class));
            case "notifyList":
                return handleNotifyList(_objectMapper.readValue(body, new TypeReference<List<Notify>>() {}));
            case "warning":
                return handleWarning(_objectMapper.readValue(body, Warning.class));
            case "tableProgress":
            case "user":
            case "userLeft":
            case "chat":
            case "tableGone":
            case "connected":
            case "clock":
            case "noteList":
            case "chatList":
            case "spectators":
            case "userInactive":
                // ignore for now, it's super spammy
                return true;
            default:
                LOG.info(String.format("Unknown command %s %s", command, body));
                return true;
        }
    }

    /**
     * Updates have happened
     * @param notify
     * @return
     */
    private boolean handleNotify(Notify notify) {
        switch (notify.getType()) {
            case CLUE:
                return _liveGameRunner.recordClue(notify);
            case STRIKE:
                return _liveGameRunner.recordStrike(notify);
            case DISCARD:
                return _liveGameRunner.recordDiscard(notify);
            case PLAY:
                return _liveGameRunner.recordPlay(notify);
            case DRAW:
                return _liveGameRunner.recordDraw(notify);
            case TURN:
                // TODO but do we need to track it for strikes, etc.
                return true; // the PlayerClient handles notification that it is it's turn,
            case DECK_ORDER:
                return true; // end of game
            case TEXT:
            case STATUS:
                return true; // don't care about display stuff
            default:
                throw new IllegalArgumentException("Unknown notify");
        }
    }

    private boolean handleNotifyList(List<Notify> notifies) {
        LOG.info("handling notifyList");
        _liveGameRunner.initialEvents(notifies);
        return true;
    }

    /**
     * it's our turn
     * @return
     */
    private boolean handleAction() {
        return false;
    }

    /**
     * tableStart  - reply hello
     * init - reply ready
     * action (once it's our turn)
     * @param init
     * @return
     */
    private boolean handleInit(Init init) {
        LOG.info("handling init");
        _liveGameRunner.init(init);
        Ready ready = new Ready();
        String socketMessage = CommandParser.serialize("ready", ready);
        LOG.info(String.format("Sending socket message %s", socketMessage));
        getWebSocket().sendText(socketMessage, true);
        return true;
    }

    /**
    Created or updated public/js/src/lobby/websocketInit.ts:97
     */
    private boolean handleTableUpdate(Table table) {
        if (table.getID() == _gameID) {
            if (table.getRunning()) {
                spectateTable(); // first time we see it's running, we join to spectate
            }
        }
        return true;
    }

    private boolean handleTableStart(TableStart tableStart) {
        if (tableStart.getReplay()) {
            return true;
        }
        Hello hello = new Hello();
        String socketMessage = "hello {}";
        LOG.info(String.format("Sending socket message %s", socketMessage));
        getWebSocket().sendText(socketMessage, true);
        return true;
    }

    private boolean handleWarning(Warning warning) {
        if ("You are already spectating another table.".equals(warning.getWarning())) {
            String socketMessage = "tableUnattend {}";
            LOG.info(String.format("Sending socket message %s", socketMessage));
            getWebSocket().sendText(socketMessage, true);
            return true;
        } else {
            return false;
        }
    }

    /**
     * When connecting, the table we want to spectate may already be in progress actually
     * @param tableList
     * @return
     */
    private boolean handleTableList(List<Table> tableList) {
        for (Table table : tableList) {
            handleTableUpdate(table);
        }
        return true;
    }


    public void spectateTable() {
        if (_alreadySpectating.getAndSet(true)) {
            LOG.warning("Already spectating");
        } else {
            TableSpectate command = new TableSpectate();
            command.setTableID(_gameID);
            String socketMessage = CommandParser.serialize("tableSpectate", command);
            LOG.info(String.format("Sending socket message %s", socketMessage));
            getWebSocket().sendText(socketMessage, true);
        }
    }
}
