package xyz.arwx.challenger.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by macobas on 26/05/17.
 */

public class HandlerConfig
{
    public Map<String, Object> config = new HashMap<>();
    // I mean we can use Jackson for this but meh.
    public String $type;
}
