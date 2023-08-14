package bgu.spl.net.srv;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

public class ConnectionsImpl<T> implements Connections<T>{

    //   username   -- passcode   , logged in or out.
    private ConcurrentHashMap<String,String> authMap = new ConcurrentHashMap<String,String>();                                            
    // connectionId -- username   , active clients and users only.
    private ConcurrentHashMap<Integer,String> clientUserMap = new ConcurrentHashMap <Integer,String>();        
    // connectionId -- connectionHandler    , active clients only.
    private ConcurrentHashMap<Integer,ConnectionHandler<T>> handlerMap = new ConcurrentHashMap <Integer,ConnectionHandler<T>>();

    // game/channel -- List<connectionId>  , updates with subscriptions.
    private ConcurrentHashMap<String,List<Integer>> gameToConnectionsMap = new ConcurrentHashMap <String,List<Integer>>();              
    // connectionId -- (Subscription)Map<game/channel ----- subId>   , updates with subscriptions.
    private ConcurrentHashMap<Integer,ConcurrentHashMap<String,String>> subIdsMap = new ConcurrentHashMap<Integer,ConcurrentHashMap<String,String>>();    

    // private Integer uniqueId;
    private AtomicInteger messageId;

    public ConnectionsImpl(){
        // uniqueId = 1;
        messageId = new AtomicInteger(1);
    }

    public boolean send(int connectionId, T msg){
        ConnectionHandler<T> handler = handlerMap.get(connectionId);
        if (handler == null) return false;
        //append subscription id
        handler.send(msg);
        return true;
    }

    public void send(String channel, T msg){
        List<Integer> subs = gameToConnectionsMap.get(channel);     // list of all ConnectionId(s) that are subscribed to channel
        for (Integer connectionId : subs){
            send(connectionId, msg);
        }
    }

    public void disconnect(int connectionId) {
        // update handlerMap, clientUserMap, subIdsMap, gameToConnetionsMap
        clientUserMap.remove(connectionId);
        if (subIdsMap.containsKey(connectionId)){
            for (Entry<String, String> e : subIdsMap.get(connectionId).entrySet()){     
                String game = e.getKey(); 
                gameToConnectionsMap.get(game).remove((Integer)connectionId);
            }
            subIdsMap.remove(connectionId);
        }
        handlerMap.remove(connectionId);
    }

    public void addUser(int connectionId, String username, String passcode){
        // update authMap, clientUserMAp and subIdsMap
        authMap.put(username, passcode);
        clientUserMap.replace(connectionId, username);
        subIdsMap.put(connectionId, new ConcurrentHashMap<String,String>());
    }

    public void addClient(ConnectionHandler<T> connectionHandler,int connectionId){
        // update handlerMap, clientUserMap and subIdsMap
        handlerMap.put(connectionId, connectionHandler);
        clientUserMap.put(connectionId, "toBeDefined");
        // subIdsMap.put(uniqueId, new HashMap<String,String>());
    }

    public void addSub(int connectionId, String destination, String subId){
        // update gameToConnectionsMap and subIdMap
        if (!gameToConnectionsMap.containsKey(destination))                    // if game does not exists, create it
            gameToConnectionsMap.put(destination, new LinkedList<Integer>());             
        gameToConnectionsMap.get(destination).add(connectionId);
        subIdsMap.get(connectionId).put(destination, subId);
    }

    public void removeSub(int connectionId, String game, String subId){
        //update gameToConnections and subIdMap
        gameToConnectionsMap.get(game).remove((Integer)connectionId);
        subIdsMap.get(connectionId).remove(game);
    }

    public boolean usernameExists(String username){
        return authMap.containsKey(username);
    }

    public boolean passcodeFitsUsername(String username, String passcode){
        return authMap.get(username).equals(passcode);
    }

    public boolean alreadyLoggedIn(String username){
        return clientUserMap.containsValue(username);
    }

    public boolean alreadySubscribed(int connectionId, String  subId){
        return subIdsMap.get(connectionId).containsValue(subId);
    }

    public String getMessageId(){
        String output = messageId.toString();
        messageId.getAndIncrement();
        return output;
    }

    public String getSubId(int connectionId, String channel){
        if (subIdsMap.containsKey(connectionId)){
            if (subIdsMap.get(connectionId).containsKey(channel))
                return subIdsMap.get(connectionId).get(channel);
        }
        return "";
    }

    public boolean isLoggedIn(int connectionId){
        return clientUserMap.containsKey((Integer)connectionId) && clientUserMap.get((Integer)connectionId) != null;
    }

    public boolean isSubbedToGame(Integer connectionId, String game){
        if (gameToConnectionsMap.containsKey(game)){
            return gameToConnectionsMap.get(game).contains(connectionId);
        }
        return false;
    }

    // connectionId -- (Subscription)Map<subId ----- game/channel>   , updates with subscriptions
    public String isSubbedWithSubId(Integer connectionId, String subId){       
        for (Entry<String, String> e : subIdsMap.get(connectionId).entrySet()){
            if (e.getValue().equals(subId))
                return e.getKey();
        }
        // Set<String> map = subIdsMap.get(connectionId).keySet();     
        // for (String game : map){
        //     if (gameToConnectionsMap.get(game).contains(subId))
        //         return game;
        // }
        return "";
    }

}