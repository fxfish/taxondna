CHANGING THE MEMORY LIMIT FOR SEQUENCEMATRIX
---------------------------------------------

Loading large datasets in SequenceMatrix can take a lot
of memory. Unfortunately, Java applications like
SequenceMatrix have strict memory limits and -- at the
moment -- the program is unable to determine when its 
memory is at risk of running out. If the application 
abruptly stops responding while loading a file, increase 
the memory limit as described in this file.

1. Open SequenceMatrix.bat using Notepad. The easiest
   way to do this is to open Notepad first (Start ->
   Applications -> Accessories -> Notepad). Then use
   the menu to open the SequenceMatrix.bat file
   (File -> Open -> Find SequenceMatrix.bat -> Select).
   Alternatively, you may be able to right-click on
   SequenceMatrix.bat itself and click on "Edit".
2. Look for the -Xmx1000M command. This indicates that
   the memory limit is set at 1000M (1,000 Megabytes).
   Change the '1000' to a higher number, such as 
   -Xmx2000M, would increase the limit to 2,000 Megabytes.
   Save the file now and exit Notepad.
3. In order for these changes to take effect, you will
   need to run SequenceMatrix by double-clicking on the
   SequenceMatrix.bat file. Running SequenceMatrix
   by double-clicking the SequenceMatrix.jar file will 
   not activate your increased memory limit. 

Note that if you set the memory limit too high (say, to
-Xmx2000M when the computer only has 2GB of memory),
Java might refuse to start at all. If this happens, set 
the memory to a lower value (in this example, to perhaps
-Xmx1500M) and try again.
