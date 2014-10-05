FFT Export
=========

FFT Export is a Java-based command-line program for creating FFT analysis files from audio files. It is heavily based on [Spanform](https://github.com/malictus/spanform), 
a C++ GUI program for spectral analysis that I wrote previously.

FFT Export was written in 2001 and uses the now-mostly-defunct Java Media Framework; I'm just moving it to GitHub for safe-keeping and in case anyone is interested.

FFT Export Syntax 
-----------------

`java FFTExport [options]`

All command-line options are optional. The options are listed below:
 
`-afile [file]`

Audio file, including full path info, to use as input. Audio can be in any format readable by the Java Media Framework. If not specified on the command-line, selection is made via a dialog box once the program starts.  

`-astart [seconds]`  

Offset (in seconds) into the sound file to begin analysis. Default is beginning of sound file.  

`-aend [seconds]`

Time (in seconds) into the sound file to stop analysis. Default is end of sound file.

`-ofile [file]`

Output analysis file, including full path info. Analysis file is plain text. If the file already exists, you will be prompted whether you wish to overwrite the existing file. If not specified, selection is made via a dialog box once the program starts.

`-pow2 [number]`

Power of two to use for FFT analysis. Must be a power of 2 between 256 and 16384. Default is 2048. Larger numbers will create larger, more detailed analysis files, but will require more time to create. Larger numbers will also cause the analysis to 'blur' sounds together somewhat.

`-cutoff [number]`

Hi-frequency cutoff (in Hz). Spectral data higher than the cutoff will be 
	ignored and not included in output analysis file. Default is 7000 and 
	maximum is 20000. 
	
`-t [seconds]`

Specifies how often (in seconds) an FFT snapshot is taken of the sound. 
	Very small numbers will result in huge analysis files. The default 
	value of 0.05 seconds will work well for short sounds, but might need to
	be adjusted for longer sounds.
	
`-noham`

Use rectangular windowing instead of the default Hamming window.
	
`-savetemp`

The program creates a temporary '.au' file during the analysis process.
	Choosing this option will prevent the temp file from being deleted
	when the program is closed.
	
`-nonverbose`

Don't write information to command line as portions of the 
	task are completed.
	
`-freqout`

Write frequency information to analysis file. This makes the file easier
	to read; this information can also be inferred without writing it explicitly
	to the file (see below). 
	
`-help` or `-man`

	Displays information about command-line options. 
	All other command-line options will be ignored.
	
`-version`

	Displays version information. All other command-line options 
	will be ignored.

About the Analysis File Format
==============================

The Analysis File is plain-text.

Line 1 - Version Information
Line 2 - Name of original file
Line 3 - Information about the temporary file from which the actual analysis is
	taken
Line 4 - Power of 2 used for FFT processing
Line 5 - Hi frequency cutoff
Line 6 - Windowing used (Rectangular or Hamming)

Line 12 to end - Analysis Frames

Each analysis frame consists of four lines.
	Line 1 - Frame Number
	Line 2 - Two values, the maximum volume for the frame and the time of the 
		frame. The maximum volume is the maximum raw sample value for that
		frame, out of 32678. This is important because frames with very 
		low volumes contain 'static' and should probably be ignored. The 
		time reading is simply the time of the middlemost moment of this
		frame, in seconds.
	Line 3 - FFT data. This line is a series of numbers separated by spaces.
		Each number represents the amount of spectral activity at a certain
		frequency. Values have been scaled to 100, so the most prominent
		frequency in each frame will have a value of 100. To find the
		frequency represented by each value, use the following formula:
			(samplerate[44100] * x) / powerof2
		Power of 2 can be found on line 4 (see above). 'x' is 1 for the 
		first value on the line, and increases by 1 for each value
		(1, 2, 3, 4, etc.)
		If you wish to see the frequencies directly, choose the 
		'-freqout' option. This line then becomes many lines,
		with the frequencies indicated.
	Line 4 - Separating space.
		 

