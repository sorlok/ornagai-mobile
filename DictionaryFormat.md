# Introduction #

To make an Ornagai Mobile dictionary, you will need a general ZIP archiver, like WinZIP 7-Zip:

  * http://www.winzip.com/index.htm
  * http://www.7-zip.org/


Make a file called ANYTHING.mzdict.zip, where **ANYTHING** is any valid file name. For example, you might call your dictionary **sep09.mzdict.zip**. Each dictionary (myanmar2english, english2myanmar) requires its own file. The format of the dictionary is determined by the contents of the zip file. _(Note that, at the moment, only english2myanmar is supported in hardware)_.

It is best **not** to use compression, as our lzma file will already be compressed, and you gain no benefit from compressing an already-compressed file.

Of course, you could also use the [OrnagaiCreator](OrnagaiCreator.md) tool, which will package the dictionary automatically for you.


# Simple Text Format #

If your phone is powerful and has lots of memory, just make a simple text list of words. Windows users should probably use Notepad++ as it is very good at handling Unicode.

http://notepad-plus.sourceforge.net/

In this case, your dictionary zip file will contain **one** file, named something like **words-tabwpd-zg2009.txt**
  * Each line of this file will contain three strings, separated by tabs
  * The file should be saved in UTF-8 encoding, without a BOM (which is irrelevant for UTF-8, by the way).
  * The **tabwpd** section of the file name determined which of these three strings is the **w** word being defined, the **p** art of speech for that word, and the **d** efinition for that word.
  * If you don't have one of these things (e.g., part of speech) then just add a tab entry for it and then immediately tab again. For example: "myanmar\t\tျမန္မာ".
  * The **zg2009** part should list the file's encoding. At the moment, we only support zg2009 and zg2008.
  * The **words** and **.txt** portions of the file name should not be changed.
  * Here is an example text dictionary:
    * http://ornagai-mobile.googlecode.com/files/basic_english_text.mzdict.zip

Please note that, although the simple text format takes up very little space (thanks to excellent ZIP encoding of text), it features very little optimization **in code**. We recommend using the Optimized Binary Format (below) unless you have a good reason not to.


# Why We Chose a Binary Format #

Originally, we wanted to allow for an optimized version of this text format. However, there's really no point: if a phone can't load a zipped lzma-text file, then it probably can't load, say, a Huffman-encoded file any easier. The solution for very low-end phones is to completely re-encode the file using a custom binary format.

Although this doesn't really reduce the size of the file -and might actually increase it!-, it has a subtle impact on performance and memory: after reading from the LZMA stream, there are less bytes to process and store. Moreover, all integer values can be read directly from the stream, instead of having to be parsed. Finally, this approach allows fast loading of the initial word list, and segmented loading of the definitions (which are only needed after searching). Moreover, by using Weak References, powerful phones can keep the entire dictionary in memory, while weak phones can evict entries if space becomes tight, to load them from the file again when memory becomes available.

There's one more potential gotcha: some phones have **very** bad Java input streams. Speaking from experience, some phones perform a "skip()" just as slowly as they would have performed a "read()". Thus, reading a file located at the end of the alphabet (e.g., the w-z entries) could require "reading" the entire file! To get around this, adamant users can repackage the jar to include their own, non-zipped dictionary. (Simply modifying the JAR using 7-zip will work). This guarantees a solution even in the worst-possible case.


# Optimized Binary Format #

An optimized dictionary contains several files:
  * One **word\_list-zg2009.bin**, where the **zg2009** part specifies the encoding for all text in this file.
  * Several **lump\_NUMBER.bin** files, where **NUMBER** starts at 1 and increases by 1 without skipping any numbers. These store dictionary entries in search order.
  * The files **lookup.bin** and **lookup\_var.bin**. These provide a serialized version of our lookup data structure that is also runtime-accessible. In other words, using this bitstream, you don't have to create any objects at all to perform complex text searches.


## The word\_list-zg2009.bin File ##

All data is stored in big-endian format. So, the value **0xFF00CC** might appear at the beginning of the file as **0xFF** **0x00** **0xCC**. We call each segment a **byte** (note that this clashes slightly with Java's definition of byte), so we can say that **0xFF00CC** is a **3-byte word**. When bits are stored, they are always stored in a stream, and there is always no more than one stream per file, and this stream is always the last data structure in the file. We store bits big-endian (which is non-standard), so that, e.g., "101" and "11101011" are stored as as **0xBD** **0x60** (10111101 011XXXXX) not as **0xBD** **0x06** (10111101 XXXXX110). Since we provide tools for converting between formats, we don't consider this to be such a big deal.

Note that maximum sizes **must** be enforced by the person creating the file. We created these hard limits to be well outside the range a normal phone would ever be able to hold; if you have, say, more than 16 million words, then you should use the text format described earlier. Same thing if you want to use characters outside the basic multilingual plane (BMP).

The first few bytes of the word\_list file form a kind of header that helps us understand the rest of the file:
  * **3 bytes**: The number of words in this dictionary. _Maximum: ~16 million_
  * **2 bytes**: The number of unique letters in the word list. _Maximum: ~65,000_
  * **2 bytes**: The length of the longest word in the dictionary. _Maximum: ~65,000_
  * **2 bytes**: The number of "lump" files. _Maximum: ~65,000_

The next part of the file contains the following for each "lump" file, in order:
  * **3 bytes**: The number of words in this lump. _Maximum: ~16 million_

The next part of the file contains the following for each "letter", in order:
  * **2 bytes**: The unicode value for this character. _Only the BMP is supported_

The next part of the file is a bitstream, containing the following for each word:
  * **X bits**: The number of letters in this word.
  * _For each letter:_
    * **Y bits**: The id of this letter. Use the lookup table to translate this into a Unicode character.

Note that X and Y are easy to determine from the largest word length and the number of letters, respectively. At the very end of the stream, there may be some trailing bits which have an undefined value. This will only happen if the bitstream doesn't end on a byte boundary.

**Important Note**: For this file and the lump file, all words **must** be stored in proper, lexicographical search order. This order is determined by a case-insensitive match on the letters a through z, up to the first letter outside this range. So, in the phrase **a dog's dinner**, the word is sorted to simply **a**. It would be grouped with **a bridge too far** and **a priori**; the full list of **a** matches would then be sorted by case-insensitive lexicographical match. This is all handled by the conversion tool. In fact, any lexicographical ordering is allowed, so long as it is the same ordering produced by an in-order traversal of the lookup tree, discussed below.

**Side Note**: The phrase **a dog's dinner** is also indexed by **dog** and **dinner**. (And, technically, by **s**). This makes it much easier to find words, and is an improvement in the 2.0 dictionary software.


## Each of the lump\_NUMBER.bin Files ##

The word\_list file is the only thing that needs to be loaded to search for a word. In order to display its results, however, at least one **NUMBER.bin** file ("lump" file) must be included. Typically, each lump file should contain about 100kb of uncompressed definition data, but you can use as many or as few files as you want, within the limit. Experiment with the size of each lump file to find a value that loads quickly and responsively on your phone.

Lump files also contain pseudo-headers:
  * **3 bytes**: The number of definitions in this lump file. _Maximum: ~16 million_
  * **2 bytes**: The number of unique letters in this lump file. _Maximum: ~65,000_

Following this are a series of "size" values, one for each definition:
  * **2 bytes**: The number of **letters** in this definition. _Maximum: ~65,000_

The next part of the file contains the following for each "letter", in order:
  * **2 bytes**: The unicode value for this character. _Only the BMP is supported_

Following this is a bitstream containing the definitions for this lump. Its length is determined by the number of **letters** times the number of bits-per-letter (easily calculated from header data) divided by 8 (bits-per-byte). Any remaining bits are undefined.


## lookup.bin and lookup\_var.bin ##

These files also use the bitstream concept to store data. Essentially, the data is used to represent a lookup tree structure for searching. A visual representation of the tree looks like this:

![http://ornagai-mobile.googlecode.com/svn/trunk/images/02-lookup_visualization.png](http://ornagai-mobile.googlecode.com/svn/trunk/images/02-lookup_visualization.png)

The tree is made up of **nodes**, which have an id. These **nodes** point to each other via **arcs**. Each **arc** is labeled with a letter --the letter one types to proceed to that arc. Moreover, each **node** can have **matches**. If a node has **matches**, they may be **primary** (from the first word in a phrase) or **secondary** (from any words after the first). When you search for a word, you start at node 0 (the **root node**) and proceed down the tree until you reach your result. All primary and secondary matches are then shown to the user.

Each word in the dictionary appears only **once** in the primary set of **one** node. Thus, we can identify each word with an id, starting from zero, in the order:

  * car
  * car park
  * car phone
  * cat
  * cat burglar
  * to
  * to the last
  * to the nearest
  * top
  * top-hat
  * top up

This is the order that the wordlist (above) must follow.

Representing this tree requires some additional decoration data, described below.

The lookup tree is divided into two files, lookup.bin and lookup\_vary.bin. The split is designed to keep both files of manageable size.

Lookup.bin contains the following:
  * **3 bytes**: The number of nodes in this lookup table. _Maximum: ~16 million_
  * **3 bytes**: The maximum "child" nodes of any one node in this table. _Maximum: ~16 million_
  * **3 bytes**: The maximum number of matches (primary or secondary) by any one node in this table. _Maximum: ~16 million_
  * **3 bytes**: The maximum word bit-id contained in this table. Each primary and secondary match points to a word by its bit-offset into the wordlist, see above. _Maximum: ~16 million_
  * **3 bytes**: The maximum node bit-id contained in this table. Each node has a pointer to its starting bit-offset in the lookup\_vary.bin file, so that we don't have to count through each entry. _Maximum: ~16 million_

Lookup.bin then contains a bitstream, containing, for each node:
  * **C bits**: The total number of primary matches reachable from this node, including its own direct matches. (Uses the "wordID" value to determine **C**)
  * **X bits**: The bit-offset of this node's lookup\_vary.bin data
  * **Y bits**: The number of children this node has.
  * **Z bits**: The number of primary matches this node has.
  * **Z bits**: The number of secondary matches this node has.

As you can see, all this data is of fixed size, so we can easily find the data for a given node ID simply by multiplying the total size of this amount by the id of the node. Note that all node/word bit-ids are from the beginning of the **bitstream** in a file, not from the beginning of the file.

Lookup\_vary.bin contains only a bitstream. For each node:
  * For each child node, in alphabetical order:
    * **5 bits**: The letter value leading to this child, stored from [0..26). Case-insensitive.
    * **W bits**: The node ID of the child pointed to by this letter.
  * For each primary match, in alphabetical order:
    * **B bits**: The word bit ID of this match in the wordlist stream.
  * For each secondary match, in alphabetical order:
    * **B bits**: The word bit ID of this match in the wordlist stream.

Here is an example binary dictionary:
  * http://ornagai-mobile.googlecode.com/files/basic_english_bin.mzdict.zip


## Complex, but not complicated ##

This means of storing the lookup tree is complex, but it saves **megabytes** of memory compared with using objects in Java. Moreover, it is very easy to reduce it down to a few simple functions, of the form:
```
   getPROPERTY(int nodeID)
   getPROPERTY(int nodeBitID)
   getPROPERTY(int wordID)
   getPROPERTY(int wordBitID)
```

So, if you have a node id and you want to figure out the number of children it has, there will be a function for you of the form
```
   getNumChildren(nodeID)
```

Likewise, if you want the third primary match (which is located in the second lookup file, and thus requires the nodeBitID) you can call:
```
   nodeBitID = getNodeBitID(nodeID)
   getNodePrimaryChildMatch(nodeBitID, 2)
```

This requires a small amount of patience and diligence, but it is by no means overly complicated. The whole point is to allow complex searching of the entire dictionary using very little memory, and it accomplishes that quite admirably.



## How a match is performed ##

Given a **word** to match for, a search proceeds down the search tree until it finds results (or matches nothing). It then forms a result set, combining with the secondary matches if possible. The search returns a **nextWordID**, a **results[.md](.md)** set, and a **nodeMatchID**. The **nodeMatchID** tells us at which node we found our results; it's zero if the word was not fully matched. The **results[.md](.md)** set combines primary and secondary results, orders them, and is inserted at **nextWordID** in the List display.

The following algorithm defines how this works:

```
  Node n = root node
  loop: for each letter L in the search word:
    Node prevN = n
    int nextID = 0
    loop: for each child C accessible from n:
      if C.key matches L:
        n = C.value
        break
      else:
        nextID += n.total_num_children
    if L is the last letter in the search word, or if n==prevN (no match):
      if n != prevN:
        if (n.primary.size>0)
          return (nextID, n.primary + n.secondary, n.id)
        else
          return (nextID, "Not found" + n.secondary, n.id)
      else:
        return (nextID, "Not found", 0)
```

(NOTE: This algorithm has changed slightly; we need to update the wiki page.)

The benefit of this approach over other tree-walking approaches is that it always returns a value, and always in constant time. Even in the event of bizarre input, we at least get reasonable results. Also, it is done iteratively (no need for recursive calls, or for storing a parent ID).


## Bits-Per-Number ##

In Java, given a total of X items, we can easily compute the number of bits required to store X using:
```
  Integer.toBinaryString(X-1).length();
```

It's nice and easy to read, so this is what I use in my source. Feel free to use a more mathematical approach yourself, for the five times it needs to be calculated.



## Note to Programmers ##

Indices into java arrays are (I think) integers, so you can have at most ~2 billion indices. If we treat each index of a word/definition as a bit-position (which makes sense, considering the bitstream approach) then that limits our word/dictionary list to 255 MB of _whatever_ (at most, **char** values). This limit only applies to the binary format, and since few phones can handle 255 MB in memory at once, I think it's a safe limit. This is particularly true when one considers that definitions, which are more likely to be bloated, can be broken up into lumps.


# Actual Results #

Converting unicode letters to a bitstream has an adverse affect on compression, so the "optimized" JAR is actually slightly bigger. This is perfectly normal; the gains outweigh the benefits in other areas.

**Original text file size**:  3,151 kb

| | Size of dictionary | Memory needed for dictionary data | File I/O required to load dictionary list |
|:|:-------------------|:----------------------------------|:------------------------------------------|
| **Text Format** | 15% of original | 3,151 kb | 500 kb |
| **Optimized Format** | 20% of original | 288 kb | 111 kb |

So, for an additional 144kb in file size, we require MUCH less memory and can access definitions faster (less File I/O to get started). Our custom-designed format appears to be a success.