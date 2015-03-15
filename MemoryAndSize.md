# Introduction #

J2ME is very diverse in terms of platforms, so I don't know, e.g., how much memory is "too much to use". So, I'll profile the current source and aim to improve on that for the release. That way, we won't lose any existing users.


# Statistics #

_About Stats_
  * Runtime Memory --> Runtime.totalMemory()-Runtime.freeMemory(), called after searching for one word and displaying its definition.
  * Size of JAR --> Under default NetBeans settings, no ProGuard
  * Average Dictionary Size --> For the bundled dictionaries (ALL, and SEPARATE), using whatever optimizations our conversion tool provides, what's the zipped size? Don't re-zip if already zipped; the idea is to see how quickly users can download a new dictionary from a forum, etc.
  * Time-to-Load --> If you set "debug" to "true", then the dictionary loads and searches one word. I want to test this on a few phones, and on the emulator, since it's a good indication of I/O. Note that the current dictionary has hard-coded (i.e., in Java) optimizations to help load its entries, so it's possible that this time will increase. But as long as it's below a few seconds for the first load (which loads our indexing structure) and a half-second for the next loads, then it's ok.

**Version 1.0**
  * Runtime Memory: 2.52 mb
  * Size of JAR: 1.05 mb
  * Average Dictionary Size: 1.39 mb
  * Time-to-Load: 0.383 s on emulator, ??? s on phone

**Version 2.0**
  * Runtime Memory: 3.40 mb
  * Size of JAR: 1.56 mb
  * Average Dictionary Size: 1.11 mb
  * Time-to-Load: 0.176 s on emulator, ??? s on phone


## Thoughts ##

JAR size is up a bit, but this is expected (lots of new classes; dictionary is slightly inflated as a result of bitstreams, etc.)

Dictionary size is down slightly; also, it loads about twice as fast. Both good.

It takes 0.9 mb more runtime memory to allow searching for words within definitions. I consider this to be a worthwhile investment. We could probably get this down by 500kb or so if we compressed nodes in the tree; i.e., any nodes with no primary or secondary results could be merged into its parent. This would be a lot of work for a tiny gain, though --as long as total runtime memory remains below 4mb, I think most phones will be fine.


# J2ME Basic Memory Usage #

An empty, Netbeans-created J2ME project uses 1781 kb of memory. A reasonable project (including LWUIT) uses about 2059 kb, and this can be obfuscated down by about 100 kb. So, about 200 kb is used by the LWUIT engine, which is decent.

Our app. currently crashes at 4094 kb. The full bitstream is about 250kb, which seems reasonably small.

Before loading the bitstream, we have 2445 kb in use, and after reading it all we're up to 2815 kb. That seems roughly in line with our goals. By implication, we'd be up to 3200 kb after loading a single lump file. Perhaps we should reduce the size of lumps, or store ALL letters (Burmese included) in the header file?

That leaves about 200kb for the search tree, which is far too little for all the object loading we've got in place. I think another bitstream is a good idea, since it lets us build the tree in the applet, and just output the relevant data. It also lets us take advantage of the **a** to **z** nature of the search letters, storing each one with only 5 bits. Finally, it minimizes the number of references to one. We'll have to use bit-indices again, but they're not terribly difficult.

I'd rather not have to do any more memory management like this, but we're approaching the limit of the memory of small phones. And I want to allow fancy searching (with all words in-memory). Time to re-define the format!

NOTE: Ok, it seems that jazzlib is causing problems again, using about 1mb of memory just for reading names from the zip file. We'll have to go with your "unzipped distribution, zipped custom dictionary" approach.

UPDATE: Ok, I got the entire word list and (segmented) dictionary file loaded with 3342 kb of memory. With the average definitions file taking up 200kb, this means we can run the dictionary with 3.5 mb, assuming I can hijack the ListView model. Not bad (+500k) for instant-search features. :) Splitting a bitstream isn't too fun, though...


# Plans #

Before getting started, here's some basic plans for keeping all three values down:
  * I want to load all words (not definitions) in memory, with a tree format that allows searching for, say "case" and getting "test case" (second word) in a "similar words" box. (A bit fancy, but it's easier to implement now.) There are 287431 letters in the English dictionary word-list. If we store unique letters using 6-bit identifiers, the list of words will take 120 kb + 210 kb = 330 kb. (Array pointers + bit-array for words). The biggest entry has 65 letters, and the tree requires about 52 bytes-per-node, plus about 3\*4 bytes-per-word. Thus, the tree will take about 3.3 kb + 361.4 kb = 365 kb to store. So, keeping all words in memory for fast searches will require about 700 kb. This is good, as storing strings in a flat format would require slightly more space (10 kb) and would not allow alternatives searching. An in-order traversal of our tree will still provide dictionary entries in their proper order, by the way. We might need a custom iterator.
  * As for loading the definitions, if we LZMA'd them, we could theoretically keep them all in memory at 488 kb, see the last bullet. We'd have to use some tricks to randomly access the stream, however. Unfortunately, this is both dangerously close to the 1mb limit (with the Myanmar dictionary included), AND it obviates keeping both wordlists available for fast mode switching (which I'd like to allow, if memory permits). I'm afraid we'll have to keep the option for fragmented files, but I'd like to generate them automatically with our conversion tool.
  * Note that Huffman-encoding of the dictionary entries does not help much with file size. We could do a fixed-bit-width lookup, maybe. (Might as well, really, since it's easy to incorporate into the export tool) but I haven't done any size measurements yet.
  * The JAR file's going to be big; no question about that. My goal is to shave a small amount of space off of it, but if the size does balloon out, we at least have ProGuard as a fallback plan.
  * 7-zipped versions of the English TSV dictionary are about 488 kb + 25 kb for the library on a good day. We can definitely inline the library code, so I hope to get the dictionary size down by about 70%. This is necessary, as the myanmar dictionary roughly doubles the file size. If we choose to segment files, this will affect compression performance, but I think we can choose 200~500kb file chunks without affecting I/O performance. The current code uses "InputStream.read()", reading one byte at a time. Reading 1024 bytes at once will give a huge boost on most platforms.


# Regarding File Messiness #

To get this all working will require using an export tool, which will probably also be written in Java. I want to keep files as readable as possible, so I won't consider binary files (except, of course, to 7-zip the whole thing). Text files and TSV files are the way to go.

If people don't have access to the tool, I don't want to limit their ability to produce a dictionary. Moreover, as phones become more powerful, some will want, essentially, the source TSV files 7-zipped and stored. This allows, e.g., for adding new entries at runtime. Although this is far beyond the scope of the current modifications, I'd like to at least leave the option open.

So, the code to read in a dictionary should be able to distinguish between dictionaries with optimizations and those without, and should load accordingly. For example, if a 6-bit letter list for words is not provided, then all words should be read and stored internally as **chars**. Perhaps when the user loads a dictionary, we can calculate "how optimized" it actually is.