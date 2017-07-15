package xyz.arwx.challenger.trigger.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.vertx.core.json.JsonObject;

/**
 * Created by macobas on 14/07/17.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "$type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "text", value = TextPayload.class)
})
public interface MessagePayload
{
    default String toIrc()
    {
        return toString();
    }

    default JsonObject toSlack()
    {
        return new JsonObject().
                put("text", toIrc());
    }
}
