/*
 * FFT Export
 * James Halliday
 * 2001
 * FFT.java - handle FFT Stuff Here
*/

//Version 1.0

//////////////////////////IMPORTS//////////////////////////
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.lang.Math;

/////////////////////////JMFaccess CLASS//////////////////////
public class FFT
{
  int numbits;
  //constructor
  public FFT()
  {}

////////////////////////METHODS/////////////////////////////
  public double[] doFFT(double[] innie, int pow2)
  {
    //find numbits
    numbits = 0;
    //find numbits, used for internal FFT processing, based on power2
    for (;;)
    {
      if ((pow2 & (1 << numbits)) != 0)
        break;
      numbits = numbits + 1;
    }
    //temporary arrays for FFT creation
    double outarray[] = new double[pow2];
    double ioutarray[] = new double[pow2];
    //counters, counters, counters!
    int count = 0;
    int count2 = 0;
    int mark = 0;
    //vars for internal FFT processing
    int blockend = 0;
    int blocksize = 0;
    double deltaangle = 0;
    double alpha = 0;
    double beta = 0;
    double ar, ai;
    int k;
    double tr, ti;
    double deltaar;
    //next - reverse bits in array
    count = 0;
    mark = 0;
    while (count < pow2)
    {
      mark = reversebits(count);
      outarray[mark] = innie[count];
      //also set ioarray to zero here
      ioutarray[count] = 0;
      count++;
    }
    //next = actual FFT calculations
    blockend = 1;
    blocksize = 2;
    //big loop
    while (blocksize <= pow2)
    {
      deltaangle = (2.0d * 3.14159265358979323846d) / (double)blocksize;
      alpha = Math.sin(0.5d * deltaangle);
      alpha = 2.0d * alpha * alpha;
      beta = Math.sin(deltaangle);
      count = 0;
      //medium loop
      while (count < pow2)
      {
        ar = 1.0d;
        ai = 0.0d;
        mark = count;
        count2 = 0;
        //little loop
        while (count2 < blockend)
        {
          k = mark + blockend;
          tr = (ar*outarray[k]) - (ai*ioutarray[k]);
          ti = (ai*outarray[k]) + (ar*ioutarray[k]);
          outarray[k] = outarray[mark] - tr;
          ioutarray[k] = ioutarray[mark] - ti;
          outarray[mark] += tr;
          ioutarray[mark] += ti;
          deltaar = (alpha*ar) + (beta*ai);
          ai -= ((alpha*ai) - (beta*ar));
          ar -= deltaar;
          //little loop incrementers
          mark = mark + 1;
          count2 = count2 + 1;
        }
        //medium loop incrementer
        count += blocksize;
      }
      //big loop incrementers
      blockend = blocksize;
      blocksize <<= 1;
    }
    //FFT DONE
    return outarray;
  }
  //reversebits - an internal process of FFT, from an algorithm of Don Cross
  public int reversebits(int index)
  {
    int i, rev;
    for (i=rev=0; i < numbits; i++)
    {
      rev = (rev << 1) | (index & 1);
      index >>= 1;
    }
    return rev;
  }
}
