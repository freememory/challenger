package xyz.arwx.challenger.trigger.impl;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.arwx.challenger.config.TriggerConfig;
import xyz.arwx.challenger.trigger.TriggerHandler;
import xyz.arwx.challenger.trigger.message.TextPayload;
import xyz.arwx.challenger.trigger.message.TriggerMessage;

/**
 * Created by macobas on 05/07/17.
 */
public class DieHandler extends TriggerHandler
{
    private static final Logger logger = LoggerFactory.getLogger(DieHandler.class);

    public DieHandler(Vertx v, String name, TriggerConfig.Trigger trigger, TriggerConfig.HandlerConfig hc)
    {
        super(v, name, trigger, hc);
    }

    @Override
    public void handleTrigger(TriggerMessage trigger)
    {
        trigger.constructResponse(new TextPayload("Gotta go!")).send();
        v.setTimer(6000, l -> System.exit(1));
    }
}
