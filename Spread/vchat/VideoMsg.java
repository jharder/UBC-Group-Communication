package vchat;

import java.io.Serializable;
import java.util.zip.CRC32;

public class VideoMsg implements Serializable {
  byte[] payload = {0};
  int seqnum = 0;
  int type;
  long checksum = 0;

  VideoMsg(byte[] d, int n) {
    payload = d;
    seqnum = n;
    type = Util.VIDEO_FRAME;
    CRC32 crc = new CRC32();
    crc.update(d);
    checksum = crc.getValue();
  }
  
  /**
   * Special message for use as a ping or for terminating a test
   * @param t Util.PING_MSG for a ping, Util.TERM_MSG to terminate
   */
  VideoMsg(int t) {
    type = t;
  }
}
