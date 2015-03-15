# Introduction #

This document will describe how the 1.0 source works.


# Platform and Libraries #

Ornagai mobile is developed on J2ME and a ready made GUI library called lwuit:
> https://lwuit.dev.java.net
... which is a Swing-like library for J2ME. 3D transitions and animitions are handled by lwuit. The project is developed and managed using Netbeans, and the LWUIT resource editor.


# Burmese Text (in Zawgyi display) #

A special component (drawing canvas) is created to render Burmese text from a given string. The dictionary data is stored in Zawgyi-2008 format (that is, "Old Zawgyi"). We will use "Zawgyi" to refer to this, and "Zawgyi 2009" to refer to Ko Soe Min's newer version. The Zawgyi glyphs are converted to a bitmap representation using the BitMap font generator from http://www.angelcode.com/products/bmfont/ .

Upon exporting the font into a bitmap, a PNG file and font descriptor files (in text/XML) is generated. For details of what fields are included in the font descriptor file, please see tutorials using the BitMap font generator site.

By using the character location data from the font-descriptor file and the canvas's dimension, the actual drawing location for each character from given text string is calculated and rendered to display the Burmese text.

**NOTE:** This technique my not be applicable in Zawgyi 2009 or Unicode Myanmar fonts (e.g., Myanmar3, Padauk, Parabaik).


# Dictionary Data and Searching #

There is no database involved in the dictionary. Rather, a series of (segmented) text files stores 1~10kb of dictionary data. There are dozens of these, named by prefix.

By storing all dictionary data in one flat text file, we will have to load the whole file into memory to search for a word. Loading the whole file into an array of strings will cause a memory error on small phones. Conversely, scanning the file linearly each time a search is made will be very slow (most phones have slow Java I/O).

The data are clustered into separate groups based on their prefix. This way the dicionary data can be split into multiple files based on their same prefix and the list of prefixes are saved too.

This process is done using a Ruby script.

When users search, the prefix list is search first and when the prefix matches, the entry file of the matching prefix is loaded into memory for display. If no prefix matches, no search result is listed.

This technique is not a permanent solution, the drawback is as the number of entries grows, the number of files will grow and the prefix list will grow. So the developer has to balance the number of characters in prefix and number of entries distributed into separate files. (Note: Many small files are bad for compression).

Storing files in File System will have no problem for users who has enough disk space, putting all of them in JAR file help for compression but does not solve the problem when number of dictionary entries grow.

I am exploring J2Me based database systems, whichever applicable. Symbian has Sqlite built in. If we write symbian app, this problem does not exist.


# Seth's Notes #

I found a 25kb Java version of the 7-zip library, which we could embed in our application. By 7-zipping our default databases as flat CSV files, we can get the JAR to a reasonable size. This also allows people to easily construct their own dictionaries.

Loading a list of strings is definitely too memory-intensive. Instead, we could probably load prefix-tree, and use some runtime compression to store the dictionary entries. I will do some profiling to see if this is fast enough.

ProGuard could also be used to shrink the JAR.

It's pretty easy to convert ZG2009 to ZG2008. So we can store the dictionary in ZG2009 format (which makes segmentation easy, if we need it) and then convert it before it's displayed.

I was thinking of having the 7-zipped database load by default, and then allowing optional loading from a file if the phone supports it. This allows maximum compatibility and flexibility.

I would like to try to incorporate a lot of your "scripted" steps into the ant build file for Netbeans. This would make it easier for new users to build the project.

It's a lot of writing, but most of these ideas are simple, and should be easy for me to implement. :)