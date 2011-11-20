package vchat;

public class VideoMsg implements Serializable {
  byte[] data;
  int seqnum;

  VidoeMsg(byte[] d, int n) {
    data = d;
    seqnum = n;
  }
}
