package xyz.arwx.challenger.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.core.Vertx;
import xyz.arwx.challenger.trigger.TriggerHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by macobas on 23/05/17.
 */
public class IrcConfig
{
    public String  server;
    public Integer port;
    public List<String> channels = new ArrayList<>();
    public String   nick;
    public boolean  autoNickRetry;
    public int      reconnectTimeMs;
    public DbConfig dbConfig;

    public int rejoinTimeMs;

    public static class Trigger
    {
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
