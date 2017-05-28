package xyz.arwx.challenger.irc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.arwx.challenger.config.IrcConfig;
import xyz.arwx.challenger.irc.trigger.TriggerHandler;
import xyz.arwx.challenger.utils.JsonMapper;

import java.util.List;

/**
 * Created by macobas on 23/05/17.
 */
public class IrcVerticle extends AbstractVerticle
{
    public static final  String InboundAddress = IrcVerticle.class.getName();
    private static final Logger logger         = LoggerFactory.getLogger(IrcVerticle.class);
    private ChallengeBot bot;
    private IrcConfig    config;
    private boolean manuallyDisconnected = false;
    private boolean isReconnecting       = false;
    private List<TriggerHandler> triggerHandlers;

    public void start()
    {
        logger.info("Starting IrcVerticle");
        config = JsonMapper.objectFromJsonObject(config(), IrcConfig.class);
        bot = new ChallengeBot(config, vertx);
        setupEventHandlers();
        setupTriggerHandlers();
    }

    private void setupTriggerHandlers()
    {
        triggerHandlers = config.getTriggerHandlers(vertx);
    }

    private void setupEventHandlers()
    {
        vertx.eventBus().consumer(InboundAddress, message -> {
            JsonObject msg = (JsonObject) message.body();
            switch (msg.getString("event"))
            {
                case Events.Connect:
                    logger.info("Going to attempt to connect");
                    if (bot.connect())
                    {
                        logger.info("Connected!");
                        manuallyDisconnected = false;
                        isReconnecting = false;
                    }
                    else if (isReconnecting)
                        setupReconnectTimer();
                    break;
                case Events.Disconnect:
                    manuallyDisconnected = true;
                    bot.disconnect();
                    break;
                case Events.Privmsg:
                    bot.sendMessage(msg.getString("target"), msg.getString("message"));
                    break;
            }
        });

        vertx.eventBus().consumer(ChallengeBot.PublishAddress, message -> {
            JsonObject msg = (JsonObject) message.body();
            switch (msg.getString("event"))
            {
                case Events.Connect:
                    logger.info("Received connect message - going to attempt to /join!");
                    config.channels.forEach(bot::joinChannel);
                    break;
                case Events.Disconnect:
                    if (!manuallyDisconnected && config.reconnectTimeMs > 0)
                        setupReconnectTimer();
                    break;
                default:
                    logger.debug("Unknown message sent: {}", msg.encode());
                    break;
            }
        });
    }

    private void setupReconnectTimer()
    {
        vertx.setTimer(config.reconnectTimeMs, _tid -> vertx.eventBus().send(InboundAddress, new JsonObject().put("event", Events.Connect)));
    }
}
