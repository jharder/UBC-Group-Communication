/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gctestfinal;

import java.io.Serializable;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 *
 * @author eric
 */
public class RainMessage implements Serializable {
    
    public static int FRAME_MSG = 10;
    public static int STRING_MSG = 11;
    public static int PING_MSG = 12;
                
            Object payload;            
            int type;
            long chsm;
 
            public RainMessage(Object msg, int type) {
                this.payload = msg;
                this.type = type;
            }
            
            public RainMessage(Object msg, int type, Checksum cs) {
                this.payload = msg;
                this.type = type;
                this.chsm = cs.getValue();
            }

}
