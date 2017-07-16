package xyz.arwx.challenger.trigger;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.arwx.challenger.config.TriggerConfig;
import xyz.arwx.challenger.trigger.message.TriggerMessage;
import xyz.arwx.challenger.utils.JsonMapper;

/**
 * Created by macobas on 26/05/17.
 */
public abstract class TriggerHandler
{
    public TriggerConfig.Trigger trigger;
    public Vertx             v;
    public String            name;
    public TriggerConfig.HandlerConfig     hc;
    public static final  String TriggerBaseAddress = TriggerHandler.class.getName();
    public static final  String ALL_PRIV_MSGS      = "ALL_PRIV_MSGS";
    private static final Logger logger             = LoggerFactory.getLogger(TriggerHandler.class);

    public TriggerHandler(Vertx v, String name, TriggerConfig.Trigger trigger, TriggerConfig.HandlerConfig hc)
    {
        this.trigger = trigger;
        this.v = v;
        this.name = name;
        this.hc = hc;
        init();
    }

    public static String TriggerAddress(String trig)
    {
        return TriggerBaseAddress + "." + trig;
    }

    public String TriggerAddress()
    {
        return TriggerAddress(name);
    }

    protected void init()
    {
        logger.info("Initialized TriggerHandler for {}", name);
        v.eventBus().consumer(TriggerAddress(), this::handleTriggerMessage);
    }

    private void handleTriggerMessage(Message<JsonObject> trigger)
    {
        handleTrigger(JsonMapper.objectFromJsonObject(trigger.body(), TriggerMessage.class));
    }

    public abstract void handleTrigger(TriggerMessage trigger);
}
