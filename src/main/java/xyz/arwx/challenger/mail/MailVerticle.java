package xyz.arwx.challenger.mail;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.arwx.challenger.config.MailConfig;
import xyz.arwx.challenger.irc.Events;
import xyz.arwx.challenger.irc.IrcVerticle;
import xyz.arwx.challenger.utils.JsonMapper;

import javax.mail.*;
import javax.mail.Message;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by macobas on 05/07/17.
 */
public class MailVerticle extends AbstractVerticle
{
    public static final  String InboundAddress = MailVerticle.class.getName();
    private static final Logger logger         = LoggerFactory.getLogger(MailVerticle.class);
    private MailConfig config;
    private IdleThread idleRunner;
    private Thread idleThread;
    private Map<String, String> nickToURLMap = new HashMap<>();

    public void start()
    {
        logger.info("Starting MailVerticle");
        config = JsonMapper.objectFromJsonObject(config(), MailConfig.class);
        try
        {
            startMailThreads();
        }
        catch (MessagingException e)
        {
            e.printStackTrace();
        }

        vertx.eventBus().consumer(InboundAddress, m -> {
            processIfInteresting((JsonObject)m.body());
        });

        vertx.setPeriodic(1 * 60 * 1000, l -> {
            Set<String> nicks = new HashSet<String>(nickToURLMap.keySet());
            nicks.forEach(this::isSessionStillActive);
        });

        vertx.setPeriodic(31 * 60 * 1000, l -> alert());
    }

    private void alert()
    {
        nickToURLMap.entrySet().forEach(e -> {
            JsonObject pubMsg = new JsonObject()
                    .put("event", Events.Privmsg)
                    .put("target", config.alertChannel)
                    .put("message", String.format(
                            "LiveTrack Alert! %s running RIGHT NOW! URL: %s", e.getKey(), e.getValue()));
            logger.info("Publishing {}", pubMsg.encode());
            vertx.eventBus().publish(IrcVerticle.InboundAddress, pubMsg);
        });
    }

    private String getTextFromMessage(Message message) throws MessagingException, IOException
    {
        String result = "";
        if (message.isMimeType("text/plain"))
        {
            result = message.getContent().toString();
        } else if (message.isMimeType("text/html")) {
            String html = (String) message.getContent();
            result += html;
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }

        return result;
    }

    private String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart)  throws MessagingException, IOException{
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result +=  bodyPart.getContent();
                break;
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result = html;
            } else if (bodyPart.getContent() instanceof MimeMultipart){
                result += getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
            }

            result += "\n";
        }
        return result;
    }

    public List<String> getToList(Message m)
    {
        List<String> ret = new ArrayList<>();
        try
        {
            for(Address addr : m.getAllRecipients())
            {
                InternetAddress inaddr = (InternetAddress)addr;
                ret.add(inaddr.getAddress());
            }
        }
        catch (MessagingException e)
        {
            e.printStackTrace();
        }

        return ret;
    }

    private void startMailThreads() throws MessagingException
    {
        Session sesh = getMailSession();
        IMAPStore store = null;
        Folder inbox = null;

        store = (IMAPStore) sesh.getStore("imaps");
        store.connect(config.userName, config.password);

        if (!store.hasCapability("IDLE"))
            throw new RuntimeException("IDLE not supported");

        inbox = (IMAPFolder) store.getFolder("INBOX");
        inbox.addMessageCountListener(new MessageCountAdapter()
        {
            @Override
            public void messagesAdded(MessageCountEvent event) {
                Message[] messages = event.getMessages();
                for (Message message : messages)
                {
                    try
                    {
                        JsonObject msg = new JsonObject()
                                .put("from", ((InternetAddress)message.getFrom()[0]).getAddress())
                                .put("to", new JsonArray(getToList(message)))
                                .put("subject", message.getSubject())
                                .put("body", getTextFromMessage(message));
                        vertx.eventBus().publish(MailVerticle.InboundAddress, msg);
                    }
                    catch (MessagingException|IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        });

        idleRunner = new IdleThread(inbox, config);
        idleThread = new Thread(idleRunner);
        idleThread.setDaemon(false);
        idleThread.start();
    }

    private void processIfInteresting(JsonObject msg)
    {
        if(!msg.getString("from").toLowerCase().endsWith("@garmin.com"))
            return;

        JsonArray to = msg.getJsonArray("to");
        List<String> recipients = to.stream().map(String.class::cast).filter(s->s.toLowerCase().startsWith("challenge.runbot+")).collect(Collectors.toList());
        if(recipients.size() == 0)
            return;

        Set<String> nicks = new HashSet<>();
        for(String recipient : recipients)
        {
            String lc = recipient.toLowerCase();
            String[] split = lc.split("@");
            if(split.length != 2)
                continue;
            String name = split[0];
            nicks.add(name.replace("challenge.runbot+", ""));
        }

        if(nicks.size() == 0)
            return;

        String body = msg.getString("body");
        Pattern p = Pattern.compile("(http://livetrack\\.garmin\\.com/session/[a-z0-9\\-]+?/token/[A-Z0-9]+?)\\W");
        Matcher m = p.matcher(body);
        Set<String> urls = new HashSet<>();
        while(m.find())
            urls.add(m.group(1));
        if(urls.size() != 1)
            return;

        String[] urlA = urls.toArray(new String[] {});
        JsonObject pubMsg = new JsonObject()
                .put("event", Events.Privmsg)
                .put("target", config.alertChannel)
                .put("message", String.format(
                        "LiveTrack Alert! %s STARTED running now! URL: %s", String.join(",", nicks), urlA[0]));
        logger.info("Publishing {}", pubMsg.encode());
        nicks.forEach(n -> {
            nickToURLMap.put(n, urlA[0]);
        });
        vertx.eventBus().publish(IrcVerticle.InboundAddress, pubMsg);
    }

    public void isSessionStillActive(String nick)
    {
        String liveTrackURL = nickToURLMap.get(nick);
        String serviceURL = liveTrackURL.replace("http://livetrack.garmin.com/session",
                "http://livetrack.garmin.com/services/session");
        vertx.createHttpClient().getAbs(serviceURL, response -> {
           if(response.statusCode() != 200)
               nickToURLMap.remove(nick);
            response.bodyHandler(buf -> {
                JsonObject o = buf.toJsonObject();
                if(o.getString("sessionStatus").equals("Expired"))
                {
                    logger.info("{} session ended", nick);
                    nickToURLMap.remove(nick);
                }
            });
        }).end();
    }

    private static class IdleThread implements Runnable
    {
        private Folder folder;
        private MailConfig mailConfig;
        private volatile boolean running = true;
        private static final Logger logger = LoggerFactory.getLogger(IdleThread.class);

        IdleThread(Folder f, MailConfig mc)
        {
            folder = f;
            mailConfig = mc;
        }

        public synchronized void kill()
        {
            if (!running)
                return;
            this.running = false;
        }

        @Override
        public void run()
        {
            while(running)
            {
                try {
                    ensureOpen();
                    logger.info("Enter idle");
                    ((IMAPFolder) folder).idle();
                } catch (Exception e) {
                    // something went wrong
                    // wait and try again
                    e.printStackTrace();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        // ignore
                    }
                }
            }
        }

        public void ensureOpen() throws MessagingException
        {
            if (folder != null) {
                Store store = folder.getStore();
                if (store != null && !store.isConnected()) {
                    store.connect(mailConfig.userName, mailConfig.password);
                }
            } else {
                throw new MessagingException("Unable to open a null folder");
            }

            if (folder.exists() && !folder.isOpen() && (folder.getType() & Folder.HOLDS_MESSAGES) != 0)
            {
                logger.info("Open folder {}", folder.getFullName());
                folder.open(Folder.READ_ONLY);
                if (!folder.isOpen())
                    throw new MessagingException("Unable to open folder " + folder.getFullName());
            }
        }
    }

    private Session getMailSession()
    {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", config.host);
        properties.put("mail.imaps.port", config.port.toString());
        Session emailSession = Session.getInstance(properties);
        return emailSession;
    }
}
