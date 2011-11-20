package vchat;

import java.io.Serializable;

public class VideoMsg implements Serializable {
  byte[] data;
  int seqnum;

  VideoMsg(byte[] d, int n) {
    data = d;
    seqnum = n;
  }
}
