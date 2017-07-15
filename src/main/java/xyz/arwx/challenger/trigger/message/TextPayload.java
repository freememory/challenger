package xyz.arwx.challenger.trigger.message;

/**
 * Created by macobas on 14/07/17.
 */
public class TextPayload implements MessagePayload
{
    public String text;
    public TextPayload(String text) { this.text = text; }
}
