package xyz.arwx.challenger.irc.trigger.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.arwx.challenger.config.HandlerConfig;
import xyz.arwx.challenger.config.IrcConfig;
import xyz.arwx.challenger.irc.Events;
import xyz.arwx.challenger.irc.IrcVerticle;
import xyz.arwx.challenger.irc.trigger.TriggerHandler;
import xyz.arwx.challenger.irc.trigger.TriggerMessage;

/**
 * Created by macobas on 05/07/17.
 */
public class DieHandler extends TriggerHandler
{
    private static final Logger logger = LoggerFactory.getLogger(DieHandler.class);

    public DieHandler(Vertx v, String name, IrcConfig.Trigger trigger, HandlerConfig hc)
    {
        super(v, name, trigger, hc);
    }

    @Override
    public void handleTrigger(TriggerMessage trigger)
    {
        logger.info("Going to exit now");
        JsonObject pubMsg = new JsonObject()
                .put("event", Events.Privmsg)
                .put("target", trigger.returnTarget())
                .put("message", "Gotta go!");
        logger.info("Publishing {}", pubMsg.encode());
        v.eventBus().publish(IrcVerticle.InboundAddress, pubMsg);
        v.setTimer(6000, l -> System.exit(1));
    }
}
