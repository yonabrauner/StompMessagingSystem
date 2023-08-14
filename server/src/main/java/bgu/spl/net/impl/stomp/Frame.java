package bgu.spl.net.impl.stomp;
import java.util.HashMap;
import java.util.Map;

public class Frame{

    public String command;
    public Map<String,String> headers;
    public String body;

    public Frame (String message){
        headers = new HashMap<>();
        body = "";
        String[] splitMessage = message.split("\n");
        this.command = splitMessage[0];
        int i = 1;
        while (i < splitMessage.length && splitMessage[i].length() != 0){
            String[] header = splitMessage[i].split(":",2);
            this.headers.put(header[0].trim().toLowerCase(), header[1].trim().toLowerCase());
            i++;
        }
        while (i < splitMessage.length && !splitMessage[i].equals("\u0000")){
            body = body.concat(splitMessage[i]+"\n");
            i++;
        }   
    }

    public Frame (String command, Map<String, String> headers, String body){
        this.command = command;
        this.headers = headers;
        this.body = body;
    }

    static String extractDestHeader(String message){
        String[] splitMessage = message.split("\n",3);
        String[] destHeader = splitMessage[1].split(":");
        return destHeader[1];
    }
}