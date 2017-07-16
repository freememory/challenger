package xyz.arwx.challenger.trigger.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by macobas on 14/07/17.
 */
public class TextPayload implements MessagePayload
{
    public String text;

    @JsonCreator
    public TextPayload(@JsonProperty("text")String text) { this.text = text; }

    @Override
    public String toString()
    {
        return text;
    }
}
