package xyz.arwx.challenger.irc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.arwx.challenger.config.IrcConfig;
import xyz.arwx.challenger.config.TriggerConfig;
import xyz.arwx.challenger.trigger.TriggerHandler;
import xyz.arwx.challenger.trigger.message.SendableMessage;
import xyz.arwx.challenger.trigger.message.TriggerMessage;
import xyz.arwx.challenger.utils.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by macobas on 23/05/17.
 */
public class IrcVerticle extends AbstractVerticle
{
    public static final  String InboundAddress = IrcVerticle.class.getName();
    private static final Logger logger         = LoggerFactory.getLogger(IrcVerticle.class);
    private ChallengeBot  bot;
    private IrcConfig     config;
    private TriggerConfig triggerConfig;
    private boolean manuallyDisconnected = false;
    private boolean isReconnecting       = false;
    private List<TriggerHandler> triggerHandlers;

    public void start()
    {
        logger.info("Starting IrcVerticle");
        JsonObject c = config();
        config = JsonMapper.objectFromJsonObject(c.getJsonObject("irc"), IrcConfig.class);
        triggerConfig = JsonMapper.objectFromJsonObject(c.getJsonObject("triggers"), TriggerConfig.class);
        bot = new ChallengeBot(config, triggerConfig, vertx);
        setupEventHandlers();
        setupTriggerHandlers();
    }

    private void setupTriggerHandlers()
    {
        triggerHandlers = triggerConfig.getTriggerHandlers(vertx);
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
                    SendableMessage smsg = JsonMapper.objectFromJsonObject(msg, SendableMessage.class);
                    bot.sendMessage(smsg.target, smsg.message.toIrc());
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
                case Events.Who:
                    User[] users = bot.getUsers(msg.getString("channel"));
                    message.reply(new JsonArray(Arrays.asList(users).stream().map(User::getNick).map(String::toLowerCase).collect(Collectors.toList())));
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

    public static void deploy(Vertx vx, IrcConfig irc, TriggerConfig tc)
    {
        vx.deployVerticle(IrcVerticle.class.getName(), new DeploymentOptions().setConfig(new JsonObject().put("irc", JsonMapper.objectToJsonObject(irc))
            .mergeIn(JsonMapper.objectToJsonObject(tc))), res -> {
           if(!res.failed())
               vx.eventBus().send(IrcVerticle.InboundAddress, new JsonObject().put("event", Events.Connect));
           else
               System.exit(-1);
        });
    }
}
