package xyz.arwx.challenger.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.Vertx;
import xyz.arwx.challenger.trigger.TriggerHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by macobas on 16/07/17.
 */
public class TriggerConfig
{
    @JsonCreator
    public TriggerConfig(Map<String, Trigger> triggers)
    {
        this.triggers = triggers;
    }

    public static class HandlerConfig
    {
        @JsonCreator
        public HandlerConfig(@JsonProperty("config") Map<String, Object> config, @JsonProperty("$type")String $type)
        {
            this.config = config;
            this.$type = $type;
        }

        public Map<String, Object> config = new HashMap<>();
        // I mean we can use Jackson for this but meh.
        public String $type;
    }

    public static class Trigger
    {
        @JsonCreator
        public Trigger(@JsonProperty("regex") String regex, @JsonProperty("handlers")List<HandlerConfig> handlers)
        {
            this.regex = regex;
            this.handlers = handlers;
        }

        public String              regex;
        public List<HandlerConfig> handlers;
    }

    public Map<String, Trigger> triggers;

    @JsonIgnore
    public List<TriggerHandler> getTriggerHandlers(Vertx vx)
    {
        List<TriggerHandler> ret = new ArrayList<>();
        triggers.forEach((k, v) -> {
            if (v.handlers != null)
                v.handlers.forEach(hc -> {
                    try
                    {
                        TriggerHandler th = (TriggerHandler) Class.forName(hc.$type).getConstructor(Vertx.class, String.class, Trigger.class, HandlerConfig.class)
                                                                  .newInstance(vx, k, v, hc);
                        ret.add(th);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                });
        });
        return ret;
    }

}
