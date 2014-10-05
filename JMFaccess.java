/*
 * FFT Export
 * James Halliday
 * 2001
 * JMFaccess.java - handle Java Media Framework Stuff Here
*/

//Version 1.0

//////////////////////////IMPORTS//////////////////////////
import javax.media.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.control.*;
import javax.media.datasink.*;

/////////////////////////JMFaccess CLASS//////////////////////
public class JMFaccess implements ControllerListener, DataSinkListener
{
  //useful constants
  static final int MONO = 1;
  static final int STEREO = 2;
  static final String BASICAU = FileTypeDescriptor.BASIC_AUDIO;
  static final String AIFF = FileTypeDescriptor.AIFF;
  static final String WAV = FileTypeDescriptor.WAVE;
  static final String MP3 = FileTypeDescriptor.MPEG_AUDIO;
  static final String LINEAR = AudioFormat.LINEAR;
  static final String MP3_COMPRESSION = AudioFormat.MPEGLAYER3;
  static final int BIG_ENDIAN = AudioFormat.BIG_ENDIAN;
  static final int LITTLE_ENDIAN = AudioFormat.LITTLE_ENDIAN;
  static final int SIGNED = AudioFormat.SIGNED;
  static final int UNSIGNED = AudioFormat.UNSIGNED;
  //vars
  static boolean filedone;
  static boolean filesuccess;
  //constructor
  public JMFaccess()
  {}

////////////////////////METHODS/////////////////////////////
  public File dotranscode(File afile, double astart, double aend, Thread thred, boolean verbose, String filetype, String compression, int endian, int signed, int channels, int bitrate, int samprate)
  {
    //vars we'll need
    File tempfile = null;
    Processor JMFproc = null;
    //start transcoding
    if (verbose)
      System.out.println("Creating Processor");
    try
      {JMFproc = Manager.createProcessor(new MediaLocator("file://" + afile));}
    catch (Exception e)
    {
      System.out.println("Error: Input audio file is not in a valid format");
      return null;
    }
    if (JMFproc == null)
    {
      System.out.println("Error: Input audio file is not in a valid format");
      return null;
    }
    if (cancelcheck(thred))
      return null;
    JMFproc.addControllerListener(this);
    if (verbose)
      System.out.println("Configuring Processor");
    //configure
    JMFproc.configure();
    //wait for configuration before going on
    while (JMFproc.getState() < JMFproc.Configured)
    {
      if (cancelcheck(thred))
        return null;
    }
    //make temp sound file
    if (verbose)
      System.out.println("Creating Temp Sound File");
    try
      {tempfile = File.createTempFile("sinktemp", ".au");}
    catch (IOException ex)
    {
      System.out.println("Trouble Creating Temporary File.");
      return null;
    }
    if (verbose)
      System.out.println("Temp File Created: " + tempfile.getAbsolutePath());
    //turn file into an .au (basic audio) file
    JMFproc.setContentDescriptor(new FileTypeDescriptor(filetype));
      System.out.println("Getting Track Information");
    //get and set up tracks to be transferred
    TrackControl tcs[];
    AudioFormat af = new AudioFormat(compression, samprate, bitrate, channels, endian, signed);
    if ((tcs = JMFproc.getTrackControls()) == null)
    {
      System.out.println("Trouble getting track controls.");
      return null;
    }
    javax.media.Format supported[];
    javax.media.Format f;
    boolean flagg = false;
    for (int i = 0; i < tcs.length; i++)
    {
      supported = tcs[i].getSupportedFormats();
      if (supported == null)
        continue;
      for (int j = 0; j < supported.length; j++)
      {
        if (af.matches(supported[j]) && (f = af.intersects(supported[j])) != null && tcs[i].setFormat(f) != null)
        {
          // Success
          flagg = true;
        }
      }
    }
    if (flagg == false)
    {
      System.out.println("Trouble getting track controls.");
      return null;
    }
    if (cancelcheck(thred))
        return null;
    //realize processor
    if (verbose)
      System.out.println("Realizing Processor");
    JMFproc.realize();
    while (JMFproc.getState() < JMFproc.Realized)
    {
      if (cancelcheck(thred))
        return null;
    }
    //make datasink
    if (verbose)
      System.out.println("Creating DataSink");
    DataSink dsink = null;
    DataSource ds;
    ds = JMFproc.getDataOutput();
    try
    {
      dsink = Manager.createDataSink(ds, new MediaLocator("file://" + tempfile.toString()));
      dsink.open();
    }
    catch (Exception e)
    {
      System.out.println("Problem creating datasink.");
      return null;
    }
    dsink.addDataSinkListener(this);
    if (cancelcheck(thred))
        return null;
    //time to transfer file over
    if (verbose)
      System.out.println("Transcoding File");
    filedone = false;
    filesuccess = false;
    try
    {
      //set audio to appropriate start point
      JMFproc.setMediaTime(new Time(astart));
      //set endpoint
      if (aend <= 0)
        aend = JMFproc.getDuration().getSeconds();
      //make sure points were valid; otherwise analysis may be messed up
      if ((astart > JMFproc.getDuration().getSeconds()) || (aend > JMFproc.getDuration().getSeconds()) || (aend <= astart) || (astart < 0))
      {
        System.out.println("Invalid time-points specified in audio file.");
        dsink.close();
        JMFproc.close();
        return null;
      }
      JMFproc.setStopTime(new Time(aend));
      JMFproc.start();
      dsink.start();
    }
    catch (Exception e)
    {
      dsink.close();
      System.out.println("Trouble Transcoding File");
      return null;
    }
    //wait till file done
    int perc = 0;
    int tempperc = -1;
    while (filedone == false)
    {
      if (thred.interrupted())
      {
        try
        {
          dsink.stop();
          JMFproc.stop();
        }
        catch (IOException errur)
        {}
        dsink.close();
        JMFproc.close();
        System.out.println("User canceled.");
        return null;
      }
      if (verbose)
      {
        //update every 10%
        perc = (int)(((JMFproc.getMediaTime().getSeconds() - astart) * 10) / (aend - astart));
        if (perc > tempperc)
        {
          tempperc = perc;
          System.out.println("Transcode: " + (perc * 10) + "% complete");
        }
      }
    }
    //cleanup; temp file created!
    try
    {
      JMFproc.stop();
      dsink.stop();
    }
    catch (IOException e)
      {}
    dsink.close();
    JMFproc.close();
    if (verbose)
      System.out.println("Temp File Created");
    return tempfile;
  }
  //check for cancel
  boolean cancelcheck(Thread thred)
  {
    if (thred.interrupted())
    {
      System.out.println("Process Canceled");
      return true;
    }
    return false;
  }
  //event stuff
  public void controllerUpdate(ControllerEvent e)
  {
    if (e instanceof EndOfMediaEvent)
    {
      filedone = true;
      filesuccess = true;
    }
    if (e instanceof StopAtTimeEvent)
    {
      filedone = true;
      filesuccess = true;
    }
  }
  public void dataSinkUpdate(DataSinkEvent evt)
  {
    if (evt instanceof EndOfStreamEvent)
    {
      filedone = true;
      filesuccess = true;
    }
    else if (evt instanceof DataSinkErrorEvent)
    {
      filedone = true;
      filesuccess = false;
    }
  }
}
