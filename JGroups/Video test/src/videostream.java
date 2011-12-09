import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class videostream
{
 public FileInputStream fis;
 public int frame_nb;
 public ArrayList<byte[]> data;
 public boolean read;
 public int counter;
 public String filename;
 public static ArrayList<String> filenames;
 public static ArrayList<videostream> streams;

 public videostream(String pfilename,ArrayList<byte[]> pdata)
 {
 	filename=pfilename;
  frame_nb=0;
  if(pdata==null)
  {
   try
			{
				fis=new FileInputStream(filename);
			}
			catch(FileNotFoundException e)
			{
				System.out.print("An error occurred while creating a file stream ("+filename+").\n");
			}
   data=new ArrayList<byte[]>();
   read=false;
  }
  else
  {
  	fis=null;
  	data=pdata;
  	read=true;
  }
  counter=0;
  filenames.add(filename);
  streams.add(this);
 }
 
 public static videostream makestream(String filename)
 {
 	int x=0;
 	int len=filenames.size();
 	for(;x<len;x++)
 	{
 		if(filename.equals(filenames.get(x)))
 		{
 			videostream oldstream=streams.get(x);
 			if(oldstream.read)
 			{
 				try
					{
						return (new videostream(filename,oldstream.data));
					}
					catch(Exception e)
					{
						return null;
					}
 			}
 		}
 	}
 	videostream newstream;
 	try
		{
			newstream=new videostream(filename,null);
		}
		catch(Exception e)
		{
			return null;
		}
		return newstream;
 }
 
 public static void setup()
 {
 	filenames=new ArrayList<String>();
 	streams=new ArrayList<videostream>();
 }

 public byte[] getnextframe()
 {
 	while(!read)
 	{
	  int length=0;
	  String length_string;
	  byte[] frame_length=new byte[5];
	  try
			{
				fis.read(frame_length,0,5);
			}
			catch(IOException e)
			{
				System.out.print("An error occured while reading from a file ("+filename+").\n");
			}
	  length_string=new String(frame_length);
	  try
	  {
	   length=Integer.parseInt(length_string);
	  }
	  catch(NumberFormatException e)
	  {
	  	read=true;
	  	counter=0;
	  	break;
	  }
	  byte[] frame=new byte[length];
	  int datalength=0;
			try
			{
				datalength=fis.read(frame,0,length);
			}
			catch(IOException e)
			{
				System.out.print("An error occured while reading from a file ("+filename+").\n");
			}
	  if(datalength==0)
	  {
	  	read=true;
	  	return frame;
	  }
	  byte[] newdata=new byte[datalength];
	  System.arraycopy(frame,0,newdata,0,datalength);
	  data.add(newdata);
	  return frame;
 	}
 	byte[] olddata=data.get(counter);
 	int n=olddata.length;
 	byte[] frame=new byte[n];
 	System.arraycopy(olddata,0,frame,0,n);
 	counter++;
 	if(counter>=data.size())
 	{
 		counter=0;
 	}
 	return frame;
 }
}