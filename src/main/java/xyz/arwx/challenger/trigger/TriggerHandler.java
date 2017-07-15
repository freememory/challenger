package xyz.arwx.challenger.trigger;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.arwx.challenger.config.HandlerConfig;
import xyz.arwx.challenger.config.IrcConfig;
import xyz.arwx.challenger.trigger.message.TriggerMessage;
import xyz.arwx.challenger.utils.JsonMapper;

/**
 * Created by macobas on 26/05/17.
 */
public abstract class TriggerHandler
{
    public IrcConfig.Trigger trigger;
    public Vertx             v;
    public String            name;
    public HandlerConfig     hc;
    public static final  String TriggerBaseAddress = TriggerHandler.class.getName();
    public static final  String ALL_PRIV_MSGS      = "ALL_PRIV_MSGS";
    private static final Logger logger             = LoggerFactory.getLogger(TriggerHandler.class);

    public TriggerHandler(Vertx v, String name, IrcConfig.Trigger trigger, HandlerConfig hc)
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
