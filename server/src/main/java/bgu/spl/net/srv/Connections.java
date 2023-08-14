package bgu.spl.net.srv;

import java.util.concurrent.atomic.AtomicInteger;

public interface Connections<T> {

    boolean send(int connectionId, T msg);

    void send(String channel, T msg);

    void disconnect(int connectionId);

    boolean usernameExists(String username);

    boolean passcodeFitsUsername(String username, String passcode);

    boolean alreadyLoggedIn(String username);

    boolean alreadySubscribed(int connectionId, String subId);

    boolean isSubbedToGame(Integer connectionId, String game);

    String isSubbedWithSubId(Integer connectionId, String subId);

    void addUser(int connectionId, String username, String passcode);

    void addClient(ConnectionHandler<T> connectionHandler, int connectionId);

    void addSub(int connectionId, String destination, String subId);

    void removeSub(int connectionId, String game ,String subId);
    
    String getMessageId();

    String getSubId(int connectionId, String channel);

    boolean isLoggedIn(int connectionId);

    AtomicInteger messageId = new AtomicInteger(1);
}
