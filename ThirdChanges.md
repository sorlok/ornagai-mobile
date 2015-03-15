# Introduction #

This is a scratch wiki page to enable collaboration.


# New Changes #

PDAs running the Ornagai Mobile dictionary have an option to load the dictionary from a file. Unfortunately, there is no "file chooser" included in either J2ME or LWUIT. Since I had to spend a long time with the ListCellRenderer previously, I decided to write my own. It displays all top-level data sources (e.g., SD cards, main memory) and then displays folders for as either "empty" (no files/folders inside) or "full" (at least one file or folder inside). It only counts files that match the filter (in our case "**.mzdict.txt"). It's a single list; there's a "Back" item at the top of every folder except the root one to go back to the previous list. No lifecycle management needs to be done; just call a static function, passing in the LWUIT Form that called the method, and a simple Action to be performed when the user hits "Ok" or "Cancel".**

Here's a screenshot:

![http://ornagai-mobile.googlecode.com/svn/trunk/images/04-file_browser.png](http://ornagai-mobile.googlecode.com/svn/trunk/images/04-file_browser.png)


# From Here #

I thought this came out quite nice. It's a shame we only need it in a single place; I'd like to try matching more items with various icons for music, documents, etc. (This is currently very easy with the API I designed.)

I've also made a dictionary that contains only the words in Basic English, a simplified version of English developed by Charles Kay Ogden. I'll use this to test both external dictionaries, and loading the text-only dictionary format. After that I've got a tiny amount of debugging left to do (searching for "a" crashes the program) and then we'll be ready for a release.

Here's the basic english file, for your perusal:

http://code.google.com/p/ornagai-mobile/source/browse/trunk/resources/dictionary_sep09/basic_english_wordlist.txt