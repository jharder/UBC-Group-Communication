/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gctestfinal;

import java.io.Serializable;

/**
 *
 * @author eric
 */
public class FrameMessage implements Serializable {
                
            long timeinmillis;
            byte[] buf;
            int image_length;
             
            public FrameMessage(byte[] buffer, int d) {
                buf = buffer;
                image_length = d;
            }

}
