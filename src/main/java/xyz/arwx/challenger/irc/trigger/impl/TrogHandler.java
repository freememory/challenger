package xyz.arwx.challenger.irc.trigger.impl;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.arwx.challenger.config.HandlerConfig;
import xyz.arwx.challenger.config.IrcConfig;
import xyz.arwx.challenger.irc.Events;
import xyz.arwx.challenger.irc.IrcVerticle;
import xyz.arwx.challenger.irc.trigger.TriggerHandler;
import xyz.arwx.challenger.irc.trigger.TriggerMessage;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by macobas on 28/05/17.
 */
public class TrogHandler extends TriggerHandler
{
    private static final Logger logger = LoggerFactory.getLogger(TrogHandler.class);

    // Training Log!
    public static class TroggerInfo
    {
        public TroggerInfo(LocalDateTime timeStarted)
        {
            this.timeStarted = timeStarted;
            timerId = -1;
        }

        public LocalDateTime timeStarted;
        public long          timerId;
        public String        trogText;
    }

    public Map<String, TroggerInfo> currentTroggers = new HashMap<>();

    public TrogHandler(Vertx v, String name, IrcConfig.Trigger trigger, HandlerConfig hc)
    {
        super(v, name, trigger, hc);
    }

    @Override
    public void init()
    {
        super.init();
        v.eventBus().consumer(TriggerAddress(ALL_PRIV_MSGS), this::handlePrivateMsg);
    }

    private void handlePrivateMsg(Message<JsonObject> message)
    {
        JsonObject pm = message.body();
        if (!currentTroggers.containsKey(pm.getString("from")))
            return;
        TroggerInfo ti = currentTroggers.get(pm.getString("from"));
        if (ti.trogText == null)
            ti.trogText = pm.getString("message");
        else
            ti.trogText += "\n" + pm.getString("message");

        setTimer(pm.getString("from"));
    }

    @Override
    public void handleTrigger(TriggerMessage trigger)
    {
        if (!trigger.isPrivateMessage())
            return;

        if (trigger.message.equals("!trog") && !currentTroggers.containsKey(trigger.from))
        {
            v.eventBus().publish(IrcVerticle.InboundAddress, new JsonObject()
                    .put("event", Events.Privmsg)
                    .put("target", trigger.from)
                    .put("message", "Ok, trogging started. Note that after 10 idle minutes, all sent private messages will be entered as your trog entry."));
            startTrogging(trigger.from);
        }
        else if (trigger.message.equals("!gort") && currentTroggers.containsKey(trigger.from))
        {
            v.eventBus().publish(IrcVerticle.InboundAddress, new JsonObject()
                    .put("event", Events.Privmsg)
                    .put("target", trigger.from)
                    .put("message", "Ok, trogging complete. Will queue for addition."));
            stopTrogging(trigger.from);
        }
    }

    private void stopTrogging(String from)
    {
        TroggerInfo ti = currentTroggers.get(from);
        logger.debug("Trog from {} complete - text {}", from, ti.trogText);
        currentTroggers.remove(from);
        v.cancelTimer(ti.timerId);
        // Send to DbVerticle
    }

    private void startTrogging(String from)
    {
        TroggerInfo info = new TroggerInfo(LocalDateTime.now());
        currentTroggers.put(from, info);
        setTimer(from);
    }

    private void setTimer(String from)
    {
        TroggerInfo info = currentTroggers.get(from);
        if (info.timerId != -1)
            v.cancelTimer(info.timerId);
        info.timerId = v.setTimer(1000 * 60 * 10, _id -> stopTrogging(from));
    }
}
