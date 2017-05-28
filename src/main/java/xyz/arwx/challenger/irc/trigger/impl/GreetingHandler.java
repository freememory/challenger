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

/**
 * Created by macobas on 26/05/17.
 */
public class GreetingHandler extends TriggerHandler {
    private static final Logger logger = LoggerFactory.getLogger(GreetingHandler.class);
    public GreetingHandler(Vertx v, String name, IrcConfig.Trigger trigger, HandlerConfig hc) {
        super(v, name, trigger, hc);
    }

    @Override
    public void handleTrigger(TriggerMessage trigger) {
        JsonObject pubMsg = new JsonObject()
                        .put("event", Events.Privmsg)
                        .put("target", trigger.channel)
                        .put("message", String.format("Hi there, %s! Beep boop.", trigger.from));
        logger.info("Publishing {}", pubMsg.encode());
        v.eventBus().publish(IrcVerticle.InboundAddress, pubMsg);
    }
}
