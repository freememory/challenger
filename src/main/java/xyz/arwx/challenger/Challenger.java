package xyz.arwx.challenger;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.sqlite.SQLiteJDBCLoader;
import xyz.arwx.challenger.config.ChallengerConfig;
import xyz.arwx.challenger.db.DbVerticle;
import xyz.arwx.challenger.irc.Events;
import xyz.arwx.challenger.irc.IrcVerticle;
import xyz.arwx.challenger.mail.MailVerticle;
import xyz.arwx.challenger.utils.JsonMapper;

import java.io.*;

/**
 * Created by macobas on 25/05/17.
 */
public class Challenger
{
    // THE GOD OBJECT
    public static Vertx vertx;

    public static void main(String[] args) throws Exception
    {
        init();
        Config c = ConfigFactory.parseResources("challengeBot.conf");
        ChallengerConfig cconfig = JsonMapper.objectFromJsonObject(new JsonObject(c.root().render(ConfigRenderOptions.concise())), ChallengerConfig.class);
        cconfig.deployVerticles(vertx);
    }

    /**
     * Does any necessery preparations
     */
    private static void init() throws Exception
    {
        final File tmp = new File(System.getProperty("java.io.tmpdir")); if (!tmp.exists() || !tmp.isDirectory() || !tmp.canRead() || !tmp.canWrite()) { throw new Exception("error with tmpDir"); } SQLiteJDBCLoader.initialize();
        vertx = Vertx.vertx();
    }
}
