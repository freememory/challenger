package xyz.arwx.challenger;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.sqlite.SQLiteJDBCLoader;
import xyz.arwx.challenger.db.DbVerticle;
import xyz.arwx.challenger.irc.Events;
import xyz.arwx.challenger.irc.IrcVerticle;
import xyz.arwx.challenger.mail.MailVerticle;

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
        final File tmp = new File(System.getProperty("java.io.tmpdir")); if (!tmp.exists() || !tmp.isDirectory() || !tmp.canRead() || !tmp.canWrite()) { throw new Exception("error with tmpDir"); } SQLiteJDBCLoader.initialize();
        init();
        JsonObject config = getConfig("challengeBot.json");

        // deploy the verticles
        DeploymentOptions dOpts = new DeploymentOptions().setConfig(config);
        vertx.deployVerticle(IrcVerticle.class.getName(), dOpts, res -> {
            if (res.failed())
                System.exit(-1);
            else
            {
                vertx.eventBus().send(IrcVerticle.InboundAddress, new JsonObject().put("event", Events.Connect));
            }
        });

        vertx.deployVerticle(MailVerticle.class.getName(), new DeploymentOptions().setConfig(getConfig("mail.json")).setWorker(true));
        DeploymentOptions dbOpts = new DeploymentOptions().setConfig(config.getJsonObject("dbConfig"));
        vertx.deployVerticle(DbVerticle.class.getName(), dbOpts);
    }

    /**
     * Does any necessery preparations
     */
    private static void init()
    {
        vertx = Vertx.vertx();
    }

    /**
     * Reades the resource as a Json file
     *
     * @return
     * @throws IOException
     */
    private static JsonObject getConfig(String rez) throws IOException
    {
        InputStream is = Challenger.class.getClassLoader().getResourceAsStream(rez);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null)
            sb.append(line);
        JsonObject config = new JsonObject(sb.toString());
        return config;
    }
}
