package xyz.arwx.challenger.trigger.impl;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.arwx.challenger.config.TriggerConfig;
import xyz.arwx.challenger.trigger.TriggerHandler;
import xyz.arwx.challenger.trigger.message.TextPayload;
import xyz.arwx.challenger.trigger.message.TriggerMessage;

/**
 * Created by macobas on 26/05/17.
 */
public class GreetingHandler extends TriggerHandler
{
    private static final Logger logger = LoggerFactory.getLogger(GreetingHandler.class);

    public GreetingHandler(Vertx v, String name, TriggerConfig.Trigger trigger, TriggerConfig.HandlerConfig hc)
    {
        super(v, name, trigger, hc);
    }

    @Override
    public void handleTrigger(TriggerMessage trigger)
    {
        trigger.constructResponse(new TextPayload(String.format("Hi there, %s! Beep boop.", trigger.from))).send();
    }
}
