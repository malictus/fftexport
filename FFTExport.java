/*
 * FFT Export
 * James Halliday
 * 2001
 * FFTExport.java - main class
*/

//Version 1.0

////////////////IMPORTS//////////////////////////////////////////////////
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.Math;
import java.text.*;
import java.util.*;

//////////////////FFTExport Class///////////////////////////////////////
public class FFTExport extends JFrame implements KeyListener
{
//////////////////CONSTANTS/////////////////////////////////////////////
  //Version Number
  final String Vers = "1.0";

//////////////////VARIABLES////////////////////////////////////////////
  //the two files we'll need, audio file and output file
  File afile;
  File ofile;
  //the temporary standardized files we'll make to do the transcoding
  File tempfile;
  //start and end points in audio file
  double astart;
  double aend;
  //cutoff frequency
  int cutoff;
  //time between FFT snapshots
  double t;
  //power of 2 for FFT processing
  int pow2;
  //hamming window or not for FFT?
  boolean noham;
  //verbose or not?
  boolean verbose;
  //saving temp or not?
  boolean savetemp;
  //outputting freq or not?
  boolean freqout;
  //thread
  exporter exportthread;

/////////CONSTRUCTOR/////////////////////////////////////////////
  public FFTExport()
  {
    super("FFTExport");
    this.addKeyListener(this);
    //Set Title Of Window
    String titl = "Export In Progress - Press 'x' To Cancel";
    setTitle(titl);
    //Initial Size and Location of Window
    setBounds(65, 65, 370, 0);
    //Don't Close Window When Clicked
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
  }

  ////////MAIN////////////////////////////////////////////////
  public static void main(String[] args)
  {
    FFTExport sink = new FFTExport();
    //Step 1 - Set some defaults
    sink.setdefaults();
    //Step 2 - Parse command-line arguments
    sink.parseargs(args);
    //Step 3 - Show Any File Dialogs Necessary
    sink.showdialogs();
    //Step 4 - Show Waiting Window and Start Thread
    sink.show();
    sink.exportthread = new exporter("exportthread", sink);
    sink.exportthread.start();
  }

  ///////////////////EXPORT THREAD////////////////////////
  static class exporter extends Thread
  {
    FFTExport sinkk;
    //constructor
    exporter (String name, FFTExport s)
    {
      super(name);
      sinkk = s;
    }
    //run method
    public void run()
    {
      if (sinkk.verbose)
      {
        //show beginning info
        System.out.println();
        System.out.println("FFT Export Version " + sinkk.Vers);
        System.out.println("Press 'x' to cancel process");
        System.out.println();
      }
      //first step, transcode input audio file to a standard '.au' audio file
      //make transcoded tempfile
      JMFaccess j = new JMFaccess();
      sinkk.tempfile = j.dotranscode
        (sinkk.afile, sinkk.astart, sinkk.aend, this, sinkk.verbose, JMFaccess.BASICAU, JMFaccess.LINEAR, JMFaccess.BIG_ENDIAN, JMFaccess.SIGNED, JMFaccess.MONO, 16, 44100);
      //make sure transcoding worked
      if (sinkk.tempfile == null)
        sinkk.whoops("Transcoding process failed or canceled.");
      //delete temp file when program exits
      if (!(sinkk.savetemp))
        sinkk.tempfile.deleteOnExit();
      //tempfile ready; ready to write header info
      if (sinkk.verbose)
        System.out.println("Writing Header Info To Output File");
      //we'll use this format for all decimals in output file
      DecimalFormat myformat = new DecimalFormat("############0.000");
      FileWriter out = null;
      try
      {
        out = new FileWriter(sinkk.ofile);
        out.write("Sound Sink " + sinkk.Vers + " Analysis File\n");
        out.write(sinkk.afile.toString() + " - Name of original audio file" + "\n");
        out.write("16 bit, mono, 44100Hz sampling rate - Temp File Info\n");
        out.write(sinkk.pow2 + " - Power of 2 for FFT processing\n");
        out.write(sinkk.cutoff + " - Hi frequency cutoff\n");
        if (sinkk.noham)
          out.write("Rectangular - FFT Window\n\n");
        else
          out.write("Hamming - FFT Window\n\n");
        out.write("-----------------\n");
        out.write("-Analysis Frames-\n");
        out.write("-----------------");
      }
      catch (Exception e)
        {sinkk.whoops("Problem writing to output file.");}
      //time to read from audio file and start ball rolling
      RandomAccessFile aud = null;
      try
      {
        aud = new RandomAccessFile(sinkk.tempfile, "r");
        //move to begining of audio file
        aud.seek(0);
        //skip over .au header
        aud.skipBytes(24);
      }
      catch (Exception e)
        {sinkk.whoops("Trouble reading tempfile.");}
      //vars needed for audio processing loop
      double innie[] = new double[sinkk.pow2];
      short buffie;
      long oldpoint;
      int counter = 0;
      long framecounter = 1;
      long perk = 0;
      float currtime;
      int perc = 0;
      int oldperc = -1;
      short maxvol = 0;
      FFT fft = new FFT();
      if (sinkk.verbose)
          System.out.println("Writing FFT data");
      /////////////////AUDIO PROCESSING LOOP!!/////////////////////////
      try
      {
        //do we have enough values to continue?
        while ((aud.getFilePointer() + (sinkk.pow2 * 2)) < aud.length())
        {
          //check for cancel
          if (this.interrupted())
          {
            aud.close();
            out.close();
            sinkk.whoops("User canceled.");
          }
          //update UI if needed
          if (sinkk.verbose)
          {
            perc = (int)(((double)(aud.getFilePointer()) / (double)(aud.length())) * 10d);
            if (perc > oldperc)
            {
              System.out.println("Create Analysis File: " + (perc * 10) + "% complete");
              oldperc = perc;
            }
          }
          //set values to start inner loop
          counter = 0;
          oldpoint = aud.getFilePointer();
          maxvol = 0;
          //read in a frame of data
          while (counter < sinkk.pow2)
          {
            //get a single sample value here
            buffie = aud.readShort();
            //put sample value into array, including adjustment for Hamming window if needed
            if (sinkk.noham)
              innie[counter] = (double)buffie;
            else
              innie[counter] = ((double)buffie * (0.54d - (0.46d * Math.cos(2d * 3.14159265358979323846d * (double)counter / ((double)sinkk.pow2 - 1d)))));
            //set volumemax, if this sample is higher than current max
            if (innie[counter] > Math.abs(maxvol))
              maxvol = buffie;
            counter = counter + 1;
          }
          //FFT-ize it!!!
          innie = fft.doFFT(innie, sinkk.pow2);
          //write info for this frame to analysis file
          out.write("\n\n\tFrame " + framecounter + ":\n");
          out.write("Maxvol: " + maxvol + "\t");
          //find time for this frame (use the middle of the frame as a reference point)
          currtime = ((oldpoint - 24f + sinkk.pow2) / 2f) / 44100f;
          out.write("Time: " + myformat.format(currtime + sinkk.astart) + "\n");
          counter = 0;
          double framemax = 0;
          //find maximum output value for this frame
          while ((counter < (sinkk.pow2 / 2)) && (((44100 * counter) / sinkk.pow2) < sinkk.cutoff))
          {
            if (framemax < Math.abs(innie[counter]))
              framemax = Math.abs(innie[counter]);
            counter = counter + 1;
          }
          counter = 0;
          //write to output file
          while ((counter < (sinkk.pow2 / 2)) && (((44100 * counter) / sinkk.pow2) < sinkk.cutoff))
          {
            if (sinkk.freqout)
                out.write("freq:" + myformat.format((44100d * counter) / sinkk.pow2) + " val:");
            if (framemax == 0)
              out.write("0.000 ");
            else
              out.write(myformat.format(Math.abs((innie[counter] / framemax) * 100)) + " ");
            if (sinkk.freqout)
              out.write("\n");
            counter = counter + 1;
          }
          //set file pointer to its new position
          aud.seek(oldpoint + (long)(sinkk.t * 44100d * 2d));
          //increment framecounter
          framecounter = framecounter + 1;
        }
      }
      catch (Exception e)
      {
        try
        {
          out.close();
          aud.close();
          sinkk.whoops("Error creating analysis file.");
        }
        catch (Exception errur) {}
      }
      //close files
      try
      {
        out.close();
        aud.close();
      }
      catch (Exception e) {}
      //all done, so let's exit
      System.out.println("Analysis File Created Successfully!");
      System.exit(0);
    }
  }
  ///////////////////METHODS/////////////////////////////////
  //Key Has Been Pressed
  public void keyReleased(KeyEvent evt)
  {
    if (evt.getKeyChar() == 'x')
      exportthread.interrupt();
  }
  public void keyPressed(KeyEvent evt)
  {}
  public void keyTyped(KeyEvent evt)
  {}
  //set default values before reading command line args
  void setdefaults()
  {
    afile = null;
    ofile = null;
    astart = 0;
    aend = -1;
    cutoff = 7000;
    t = 0.05d;
    pow2 = 2048;
    noham = false;
    verbose = true;
    savetemp = false;
    freqout = false;
  }
  //parse the command-line arguments
  void parseargs(String[] args)
  {
    int i = 0;
    while (i < args.length)
    {
      //audio file was specified
      if (args[i].equals("-afile"))
      {
        i++;
        if (i >= args.length)
          whoops("Invalid command-line arguments.");
        afile = new File(args[i]);
        if (!(afile.isFile() && afile.canRead()))
          whoops("Input audio file path is invalid.");
      }
      else if (args[i].equals("-astart"))
      {
        i++;
        if (i >= args.length)
          whoops("Invalid command-line arguments.");
        try
        {
          Double Dastart = Double.valueOf(args[i]);
          astart = Dastart.doubleValue();
          if (astart < 0)
            throw new NumberFormatException();
        }
        catch (Exception e) {whoops("Invalid time-point specified");}
      }
      else if (args[i].equals("-aend"))
      {
        i++;
        if (i >= args.length)
          whoops("Invalid command-line arguments.");
        try
        {
          Double Daend = Double.valueOf(args[i]);
          aend = Daend.doubleValue();
          if (aend < 0)
            throw new NumberFormatException();
        }
        catch (Exception e) {whoops("Invalid time-point specified");}
      }
      //output file was specified
      else if (args[i].equals("-ofile"))
      {
        i++;
        if (i >= args.length)
          whoops("Invalid command-line arguments.");
        ofile = new File(args[i]);
        //if not exist, create, if exist ask if overwrite
        if (ofile.exists() && ofile.isFile())
        {
          //overwrite?
          int response = JOptionPane.showConfirmDialog(null, "Output file already exists. Overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
          if (response == JOptionPane.NO_OPTION)
            {whoops("Process canceled.");}
        }
        else
        {
          //create new file
          try
          {
            boolean diditwork = ofile.createNewFile();
            if (diditwork == false)
              whoops("Output file cannot be created.");
          }
          catch (IOException e)
            {whoops("Output file cannot be created.");}
        }
      }
      else if (args[i].equals("-pow2"))
      {
        i++;
        if (i >= args.length)
          whoops("Invalid command-line arguments.");
        try
        {
          Integer Pow2 = Integer.valueOf(args[i]);
          pow2 = Pow2.intValue();
          if (!((pow2 == 256) | (pow2 == 512) | (pow2 == 1024) | (pow2 == 2048) | (pow2 == 4096) | (pow2 == 8192) | (pow2 == 16384)))
            throw new NumberFormatException();
        }
        catch (Exception e) {whoops("Power of 2 is not valid. Choose a power of 2 between 256 and 16384.");}
      }
      else if (args[i].equals("-cutoff"))
      {
        i++;
        if (i >= args.length)
          whoops("Invalid command-line arguments.");
        try
        {
          Integer Cutoff = Integer.valueOf(args[i]);
          cutoff = Cutoff.intValue();
          if (cutoff > 20000)
            cutoff = 20000;
          if (cutoff < 20)
            throw new NumberFormatException();
        }
        catch (Exception e) {whoops("Invalid cutoff value specified.");}
      }
      else if (args[i].equals("-t"))
      {
        i++;
        if (i >= args.length)
          whoops("Invalid command-line arguments.");
        try
        {
          Double T = Double.valueOf(args[i]);
          t = T.doubleValue();
          if (t <= 0)
            throw new NumberFormatException();
        }
        catch (Exception e) {whoops("Invalid t value specified.");}
      }
      else if (args[i].equals("-noham"))
        {noham = true;}
      else if (args[i].equals("-nonverbose"))
        {verbose = false;}
      else if (args[i].equals("-help"))
        {showhelp();}
      else if (args[i].equals("-man"))
        {showhelp();}
      else if (args[i].equals("-version"))
        {showversion();}
      else if (args[i].equals("-savetemp"))
        {savetemp = true;}
      else if (args[i].equals("-freqout"))
        {freqout = true;}
      else
        {whoops("Invalid command-line arguments.");}
      i++;
    }
  }
  //show help info
  void showhelp()
  {
    System.out.println("FFTExport options");
    System.out.println("-afile [file] = Full Path to Audio File To Analyze");
    System.out.println("-astart [seconds] = Position in Audio File To Start Analysis (in seconds)");
    System.out.println("-aend [seconds] = Position in Audio File To End Analysis (in seconds)");
    System.out.println("-ofile [file] = Full Path to Plain-Text Output File To Create");
    System.out.println("-pow2 [number] = Power of 2 for FFT between 256 and 16384");
    System.out.println("-cutoff [number] = Hi Frequency Cutoff (in Hz). Maximum is 20000; default is 7000.");
    System.out.println("-t [seconds] = How often (in seconds) to take an FFT snapshot. Default is 0.05 seconds");
    System.out.println("-noham = Use rectangular window for FFT. Without this option, default is Hamming window.");
    System.out.println("-savetemp = DON'T delete the temporary transcoded file upon finishing the export.");
    System.out.println("-nonverbose = Don't write information to command line as portions of the task are completed.");
    System.out.println("-freqout = Include frequency information in analysis file.");
    System.out.println("-help = Display help information");
    System.out.println("-man = Display help information");
    System.out.println("-version = Display version information");
    System.exit(0);
  }
  //show version info
  void showversion()
  {
    System.out.println("FFT Export Version " + Vers);
    System.exit(0);
  }
  //show dialog boxes for files that weren't specified on command line and make file references to them
  void showdialogs()
  {
    if (afile == null)
    {
      //prompt for audio file
      JFileChooser chooze = new JFileChooser();
      int returnval = chooze.showDialog(null, "Audio File");
      if (!(returnval == JFileChooser.APPROVE_OPTION))
        whoops("Process Canceled.");
      afile = chooze.getSelectedFile();
      if (!(afile.isFile() && afile.canRead()))
          whoops("Audio file path chosen is invalid.");
    }
    if (ofile == null)
    {
      //prompt for output file
      JFileChooser chooze = new JFileChooser();
      int returnval = chooze.showDialog(null, "Output File");
      if (!(returnval == JFileChooser.APPROVE_OPTION))
        whoops("Process Canceled.");
      ofile = chooze.getSelectedFile();
      //if not exist, create, if exist ask if overwrite
      if (ofile.exists() && ofile.isFile())
      {
        //overwrite?
        int response = JOptionPane.showConfirmDialog(null, "Output file already exists. Overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (response == JOptionPane.NO_OPTION)
          {whoops("Process canceled.");}
      }
      else
      {
        //create new file
        try
        {
          boolean diditwork = ofile.createNewFile();
          if (diditwork == false)
            whoops("Output file cannot be created.");
        }
        catch (IOException e)
          {whoops("Output file cannot be created.");}
      }
    }
  }
  //something went wrong - show why and exit
  void whoops(String s)
  {
    System.out.println("Execution Halted");
    System.out.println("Reason: " + s);
    System.exit(0);
  }
}
