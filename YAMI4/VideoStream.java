package yami;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class VideoStream {

	FileInputStream fis; //video file
	String filename;
	int frameNum = 0;

	//-----------------------------------
	//constructor
	//-----------------------------------
	public VideoStream(String filename) throws Exception {
		this.filename = filename;
		Initialize(filename);
	}

	//-----------------------------------
	// getNextFrame
	//returns the next frame as an array of byte and the size of the frame
	//-----------------------------------
	public int getNextFrame(byte[] frame) throws Exception {
		int length = 0;
		String length_string;
		byte[] frame_length = new byte[5];
		
		if (fis.available() <= 0) {
			frameNum = 0;
			fis.close();
			Initialize(filename);
		}

		//read current frame length
		fis.read(frame_length, 0, 5);

		//transform frame_length to integer
		length_string = new String(frame_length);
		length = Integer.parseInt(length_string);
		fis.read(frame, 0, length);
		frameNum++;
		
		return frameNum; 
	}
	
	private void Initialize(String filename) throws FileNotFoundException {
		fis = new FileInputStream(filename);
	}
}