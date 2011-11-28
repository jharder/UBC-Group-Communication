package vchat;

import java.io.Serializable;
import java.util.zip.CRC32;

public class VideoMsg implements Serializable {
  byte[] data;
  int seqnum;
  long checksum;

  VideoMsg(byte[] d, int n) {
    data = d;
    seqnum = n;
    CRC32 crc = new CRC32();
    crc.update(d);
    checksum = crc.getValue();
  }
}
