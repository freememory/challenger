package xyz.arwx.challenger.trigger.message;

import xyz.arwx.challenger.Challenger;
import xyz.arwx.challenger.utils.JsonMapper;

/**
 * Created by macobas on 14/07/17.
 */
public class SendableMessage<M extends MessagePayload>
{
    public String target;
    public String event;
    public TriggerMessage.Source source;
    public M message;

    public void send()
    {
        Challenger.vertx.eventBus().publish(source.ebAddress(), JsonMapper.objectToJsonObject(this));
    }
}
