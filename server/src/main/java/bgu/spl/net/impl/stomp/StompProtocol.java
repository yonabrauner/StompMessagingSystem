package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;

public class StompProtocol implements StompMessagingProtocol<String>{
    private boolean shouldTerminate = false;
    private Frame frame;
    private Connections<String> connections;
    private int connectionId;
    private String message;
    private String errorMsg;
    private String errorHeaders;

    @Override
    public void start(int connectionId, Connections<String> connections){
        this.connections = connections;
        this.connectionId = connectionId;
        // this.shouldTerminate = false;
        // this.errorMsg = "ERROR"+"\n";
        this.errorMsg = "";
        this.errorHeaders = "";
    }

    @Override
    public void process(String message){
        this.message = message;
        this.frame = new Frame(message);
        switch (frame.command){
            case "CONNECT":

                handleConnect();
                break;

            case "SEND" : 

                handleSend();
                break;
            
            case "SUBSCRIBE" :

                handleSubscribe();
                break;

            case "UNSUBSCRIBE" :

                handleUnsubscribe();
                break;

            case "DISCONNECT" :

                handleDisconnect();
                break;
            
            default:

                errorHeaders = errorHeaders.concat("message: unrecognized frame");
                errorMsg = errorMsg.concat("could not preforrm required action." + "\n");
                error();

        }
    }  


    @Override
    public boolean shouldTerminate(){
        return shouldTerminate;
    }
    
    private void handleConnect(){
        boolean success = true;
        boolean receipt = false;
        String receiptId = "";
        if (frame.headers.containsKey("accept-version")){                   // check that version header exists
            if (!frame.headers.get("accept-version").equals("1.2")){        // check that version is acceptable
                success = false;
                errorMsg = errorMsg.concat("STOMP version not acceptable. STOMP 1.2 is the required version." + "\n");
                errorHeaders = errorHeaders.concat("message: STOMP version unacceptable" + "\n");
            }
        }
        else {
            errorMsg = errorMsg.concat("missing version header, which is REQUIRED for user login." + "\n");
            errorHeaders = errorHeaders.concat("message: malformed frame received" + "\n");
            success = false;
        }
        if (frame.headers.containsKey("host")){                              // check that host header exists
            if (!frame.headers.get("host").equals("stomp.cs.bgu.ac.il")){    // check that host is acceptable
                success = false;
                errorMsg = errorMsg.concat("host is unacceptable. please make sure you are using a correct host:port." + "\n");
                errorHeaders = errorHeaders.concat("message: unacceptable host" + "\n");
            }
        }
        else {
            success = false;
            errorMsg = errorMsg.concat("missing host header, which is REQUIRED for user login." + "\n");
            errorHeaders = errorHeaders.concat("message: malformed frame received" + "\n");
        }
        // if (connections.alreadyLoggedIn(frame.headers.get("login"))){                      
        //     success = false;                                                                         // check that user is not already logged in
        //     errorHeaders = errorHeaders.concat("message: already logged in" + "\n");
        //     errorMsg = errorMsg.concat("the client is already logged in, log out before trying again." + "\n");
        // }
        if (frame.headers.containsKey("login") && frame.headers.containsKey("passcode")){            // check that passcode and username headers exist
            String username = frame.headers.get("login"), passcode = frame.headers.get("passcode");
            if (connections.usernameExists(username)) {                                              // if user name exists,
                if (connections.passcodeFitsUsername(username, passcode)){                           // check that passcode is acceptable
                    if (connections.alreadyLoggedIn(username)){                                      // check that user is not already logged in through another client
                        success = false;
                        errorMsg = errorMsg.concat("user already logged in. make sure to log out completely before attempting to log in." + "\n");
                        errorHeaders = errorHeaders.concat("message: user logged in" + "\n");
                    }
                    else{
                        connections.addUser(connectionId, username, passcode);                               // add user to list of users
                    }
                }
                else{
                    success = false;
                    errorMsg = errorMsg.concat("the user: " + username + " exists but the passcode entered is wrong." + "\n");
                    errorHeaders = errorHeaders.concat("message: incorrect passcode" + "\n");
                }
            }
            else                                                                                     // user name does not exist,
                connections.addUser(connectionId, username, passcode);                               // add user to list of users
        }
        else {
            errorMsg = errorMsg.concat("missing login or passcode header, which are REQUIRED for user login." + "\n");
            errorHeaders = errorHeaders.concat("message: malformed frame received" + "\n");
            success = false;
        }
        if (frame.headers.containsKey("receipt")){                                                   // check if receipt header exists
            receiptId = frame.headers.get("receipt");
            receipt = true;
            errorHeaders = errorHeaders.concat("receipt-id: " + receiptId + "\n");
        }
        if (success){                                                                                // check if there has been an error
            String msg = "CONNECTED" + "\n" + "version:1.2" + "\n" + "\n" + "\u0000";
            connections.send(connectionId, msg);
            errorHeaders = "";
            errorMsg = "";

            if (receipt)
                receipt(receiptId);
        }
        else
            error();
    }

    private void handleSend(){
        boolean success = true;
        boolean receipt = false;
        String receiptId = "";
        String destination = "";
        if (frame.headers.containsKey("destination")){                                              // check that destination header exists
            destination = frame.headers.get("destination");
        }
        else{
            // success = false;
            errorHeaders = errorHeaders.concat("message: malformed frame recieved" + "\n");
            errorMsg = errorMsg.concat("missing destination header, which is REQUIRED for message propagation." + "\n");
        }
        if (frame.headers.containsKey("receipt")){                                                  // check if receipt header exists
            receiptId = frame.headers.get("receipt");
            receipt = true;
            errorHeaders = errorHeaders.concat("receipt-id: " + receiptId + "\n");
        }
        if (destination != "" && !connections.isSubbedToGame(connectionId, destination)){           // check that user is subscribed to game
            success = false;
            errorHeaders = errorHeaders.concat("message: not subscribed to channel" + "\n");
            errorMsg = errorMsg.concat("in order to send to that channel, you must be subscribed to it." + "\n");
        }
        // if (!connections.isLoggedIn(connectionId)){                                                 // check that user is logged in
        //     errorHeaders = "message: not logged in";
        //     errorMsg = "user not logged in, please log in and try again.";
        //     success = false;
        // }
        if (success){                                                                               // check if there has been an error
            String msg = "message-id:" + connections.getMessageId() + "\n" + "destination:" + destination + "\n" + "\n" + frame.body;
            connections.send(destination,msg);
            errorHeaders = "";
            errorMsg = "";
            if (receipt)
                receipt(receiptId);
        }
        else
            error();
    }

    private void handleSubscribe(){
        boolean success = true;
        boolean receipt = false;
        String receiptId = "";
        String destination = "";
        String subId = "";
        if (frame.headers.containsKey("destination")){                                                // check that destination header exists
            destination = frame.headers.get("destination");
        }
        else{
            success = false;
            errorHeaders = errorHeaders.concat("message: malformed frame recieved" + "\n");
            errorMsg = errorMsg.concat("missing destination header, which is REQUIRED for subscription." + "\n");
        }
        if (frame.headers.containsKey("id")){                                                       // check that id header exists
            subId = frame.headers.get("id");
        }
        else{
            success = false;
            errorHeaders = errorHeaders.concat("message: malformed frame recieved" + "\n");
            errorMsg = errorMsg.concat("missing id header, which is REQUIRED for subscription." + "\n");
        }
        if (connections.alreadySubscribed(connectionId, subId)){                                   // check that not subbed with same Id
            success = false;
            errorHeaders = errorHeaders.concat("message: illegal subId" + "\n");
            errorMsg = errorMsg.concat("you are already subscribed to a channel with that Id, please enter another one." + "\n");
        }
        if (frame.headers.containsKey("receipt")){                                                  // check if receipt header exists
            receiptId = frame.headers.get("receipt");
            receipt = true;
            errorHeaders = errorHeaders.concat("receipt-id: " + receiptId + "\n");
        }
        // if (!connections.isLoggedIn(connectionId)){                                                 // check that user is logged in
        //     errorHeaders = "message: not logged in";
        //     errorMsg = "user not logged in, please log in and try again.";
        //     success = false;
        // }
        if (success){
            connections.addSub(connectionId, destination, subId);
            if (receipt)
                receipt(receiptId);
        }
        else
            error();
    }

    private void handleUnsubscribe(){
        boolean success = true;
        boolean receipt = false;
        String receiptId = "";
        String subId = "";
        String destination = "";
        if (frame.headers.containsKey("id")){                                                       // check if id header exists
            subId = frame.headers.get("id");
        }
        else{
            success = false;
            errorHeaders = errorHeaders.concat("message: malformed frame recieved" + "\n");
            errorMsg = errorMsg.concat("missing id header, which is REQUIRED for unsubscription." + "\n");
        }
        if (frame.headers.containsKey("receipt")){                                                  // check if receipt header exists
            receiptId = frame.headers.get("receipt");
            receipt = true;
            errorHeaders = errorHeaders.concat("receipt-id: " + receiptId + "\n");
        }
        destination = connections.isSubbedWithSubId(connectionId, subId);
        if (subId != "" && destination == ""){                                                      // check that user is subscribed to the channel
            success = false;
            errorHeaders = errorHeaders.concat("message: illegal id" + "\n");
            errorMsg = errorMsg.concat("user not subscribed to any channel with given id." + "\n");
        }
        // if (!connections.isLoggedIn(connectionId)){                                                 // check that user is logged in
        //     errorHeaders = "message: not logged in";
        //     errorMsg = "user not logged in, please log in and try again.";
        //     success = false;
        // }
        if (success){
            connections.removeSub(connectionId, destination, subId);
            if (receipt)
                receipt(receiptId);
        }
        else 
            error();
    }

    private void handleDisconnect(){
        boolean success = true;
        String receiptId = "";
        if (frame.headers.containsKey("receipt")){                                                  // check if receipt header exists
            receiptId = frame.headers.get("receipt");
            errorHeaders = errorHeaders.concat("receipt-id: " + receiptId + "\n");
        }
        else{
            success = false;
            errorHeaders = errorHeaders.concat("message: malformed frame recieved" + "\n");
            errorMsg = errorMsg.concat("missing receipt header, which is REQUIRED for disconnecting." + "\n");
        }
        // if (!connections.isLoggedIn(connectionId)){                                                 // check that user is logged in
        //     errorHeaders = "message: not logged in";
        //     errorMsg = "user not logged in, please log in and try again.";
        //     success = false;
        // }
        if (success){
            receipt(receiptId);
            connections.disconnect(connectionId);
            shouldTerminate = true;
        }
        else
            error();
    }

    public void error(){
        if (errorHeaders == "" && errorMsg == ""){
            errorHeaders = "unrecognized error";
            errorMsg = "unrecognized error encountered. please check everything.";
        }
        String msg = "ERROR" + "\n" + errorHeaders + "\n" + "The message:" + "\n" + "-----------------------" 
                    + "\n" + message + "\n" + "-----------------------" + "\n" + errorMsg + "\n" + "\u0000";
        connections.send(connectionId, msg);
        connections.disconnect(connectionId);
        this.shouldTerminate = true;
        this.errorMsg = "";
        this.errorHeaders = "";
    }

    private void receipt(String receiptId){
        String msg = "RECEIPT" + "\n" + "receipt-id:" + receiptId + "\n" + "\n" + "\u0000";
        connections.send(connectionId, msg);
    }

    public String prepareMsg (String msg){
        if (msg.startsWith("message-id:")){                                     // checks if sending a not-ready MESSAGE frame
            String channel = Frame.extractDestHeader(msg);
            msg = "MESSAGE" + "\n" + "subscription:" + connections.getSubId(connectionId, channel.trim()) + "\n" + msg;
        }  
        return msg;
    }

}
