package xyz.arwx.challenger.irc;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.jibble.pircbot.PircBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.arwx.challenger.config.IrcConfig;
import xyz.arwx.challenger.trigger.TriggerHandler;
import xyz.arwx.challenger.trigger.message.TriggerMessage;
import xyz.arwx.challenger.utils.JsonMapper;

import java.util.Map;

import static xyz.arwx.challenger.irc.Events.*;
import static xyz.arwx.challenger.trigger.TriggerHandler.ALL_PRIV_MSGS;

/**
 * Created by macobas on 23/05/17.
 */
public class ChallengeBot extends PircBot
{
    private IrcConfig config;
    private Vertx     v;
    public static final  String PublishAddress = ChallengeBot.class.getName();
    private static final Logger logger         = LoggerFactory.getLogger(ChallengeBot.class);

    public ChallengeBot(IrcConfig config, Vertx vertx)
    {
        this.config = config;
        this.v = vertx;
        init();
    }

    private void init()
    {
        setName(config.nick);
        setAutoNickChange(config.autoNickRetry);
    }

    @Override
    public void onConnect()
    {
        raise(Connect);
    }

    @Override
    public void onDisconnect()
    {
        raise(Disconnect);
    }

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message)
    {
        for (Map.Entry<String, IrcConfig.Trigger> trig : config.triggers.entrySet())
        {
            if (message.matches(trig.getValue().regex))
            {
                trigger(new JsonObject()
                        .put("from", sender)
                        .put("channel", channel)
                        .put("message", message)
                        .put("trigger", trig.getKey()));
                logger.info("Got trigger {} from {}, message: {}", trig.getKey(), sender, message);
            }
        }
    }

    @Override
    public void onJoin(String channel, String sender, String login, String hostName)
    {
        if (sender.equals(getNick()))
            raise(Join, new JsonObject().put("channel", channel));
    }

    @Override
    public void onPrivateMessage(String sender, String login, String hostname, String message)
    {
        boolean sent = false;
        for (Map.Entry<String, IrcConfig.Trigger> trig : config.triggers.entrySet())
        {
            if (message.matches(trig.getValue().regex))
            {
                trigger(new JsonObject()
                        .put("from", sender)
                        .put("message", message)
                        .put("trigger", trig.getKey()));
                logger.info("Got trigger {} from {}, message: {}", trig.getKey(), sender, message);
                sent = true;
            }
        }

        if (!sent)
            trigger(new JsonObject()
                    .put("from", sender)
                    .put("message", message)
                    .put("trigger", ALL_PRIV_MSGS));
    }


    private void trigger(JsonObject trigInfo)
    {
        TriggerMessage tm = JsonMapper.objectFromJsonObject(trigInfo, TriggerMessage.class);
        tm.source = TriggerMessage.Source.IRC;
        v.eventBus().publish(TriggerHandler.TriggerAddress(trigInfo.getString("trigger")), JsonMapper.objectToJsonObject(tm));
    }

    private void raise(String event, JsonObject payload)
    {
        v.eventBus().publish(PublishAddress, new JsonObject().put("event", event).put("payload", payload));
    }

    private void raise(String event)
    {
        raise(event, new JsonObject());
    }

    public boolean connect()
    {
        if (isConnected())
            return true;

        try
        {
            connect(config.server, config.port);
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
}
