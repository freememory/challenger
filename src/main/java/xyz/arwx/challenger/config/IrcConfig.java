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
    public int rejoinTimeMs;
}
