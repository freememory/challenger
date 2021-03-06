package xyz.arwx.challenger.trigger.impl;

import io.vertx.core.Vertx;
import xyz.arwx.challenger.config.TriggerConfig;
import xyz.arwx.challenger.trigger.TriggerHandler;
import xyz.arwx.challenger.trigger.message.TextPayload;
import xyz.arwx.challenger.trigger.message.TriggerMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by macobas on 26/05/17.
 */
public class EightBallHandler extends TriggerHandler
{

    public List<String> responses = new ArrayList<>();
    public Random rando;

    public EightBallHandler(Vertx v, String name, TriggerConfig.Trigger trigger, TriggerConfig.HandlerConfig hc)
    {
        super(v, name, trigger, hc);
        rando = new Random();
    }

    @Override
    public void handleTrigger(TriggerMessage trigger)
    {
        List<String> respList = (List<String>) hc.config.get("responses");
        int r = rando.nextInt(respList.size());
        String response = respList.get(r);
        trigger.constructResponse(new TextPayload(response)).send();
    }
}
