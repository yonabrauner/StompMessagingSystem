package bgu.spl.net.impl.stomp;
import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class StompEncoderDecoder implements MessageEncoderDecoder<String>{

    private byte[] bytes = new byte[1 << 10];
    private int len = 0;
    
    @Override
    public String decodeNextByte(byte nextByte) {
        if (nextByte == '\u0000') {                 // if null character - message is through, return it.
            return popString();
        }

        pushByte(nextByte);                     
        return null;                                // message not fully recieved yet.
    }
    
    @Override
    public byte[] encode(String message) {
        return (message + "\u0000").getBytes();     // sends in UTF-8 by default. add the null character.
    }

    private void pushByte(byte nextByte) {          
        if (len >= bytes.length) {                  
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
    }

    private String popString() {
        String result = new String(bytes, 0, len);
        len = 0;
        return result;
    }
}
