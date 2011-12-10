package vchat;
/*VideoStream.java*/

import java.io.*;

public class VideoStream {
  static final int NUM_REPS = 4;
  int rep;

	FileInputStream fis; //video file
	int frame_nb; //current frame nb
	String filename;

	//-----------------------------------
	//constructor
	//-----------------------------------
	public VideoStream(String filename) throws Exception {
		this.filename = filename;
		rep = 1;
		initialize(filename);
	}

	//-----------------------------------
	// getnextframe
	//returns the next frame as an array of byte and the size of the frame
	//-----------------------------------
	public int getNextFrame(byte[] frame) throws Exception {
		int length = 0;
		String length_string;
		byte[] frame_length = new byte[5];
		
		if (fis.available() <= 0 && rep <= NUM_REPS) {
			// Restart video
			initialize(filename);
			rep++;
		}	else if (fis.available() <= 0) {
		  fis.close();
		  return -1;
		}

		//read current frame length
		fis.read(frame_length, 0, 5);

		//transform frame_length to integer
		length_string = new String(frame_length);
		length = Integer.parseInt(length_string);

		return fis.read(frame, 0, length);
	}
	
	private void initialize(String filename) throws FileNotFoundException {
		fis = new FileInputStream(filename);
	}
}