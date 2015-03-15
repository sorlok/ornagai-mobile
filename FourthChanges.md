# Introduction #

This is a scratch wiki page to enable collaboration.


# New Changes #

I've imported some code from the WaitZar utility library, which allows me to properly segment multi-line text components. Now, you won't see definitions breaking over syllable boundaries.

Here's a screenshot:

![http://ornagai-mobile.googlecode.com/svn/trunk/images/05-proper_segmentation.png](http://ornagai-mobile.googlecode.com/svn/trunk/images/05-proper_segmentation.png)


# From Here #

I want to add some basic support for Zawgyi2009. It should be relatively easy to do a simple conversion that is 95~99% accurate, which is enough to label "experimental".

After that, the only major thing left to do is to debug the binary format. We might also "hide" the duplicate results of a binary search (can't do it with textformat, unfortunately, as no ordering is guaranteed).

I hope to release a JAR for people to test, along with the Ornagai conversion tool. That will give me time to write up documentation and fix the licensing files, as well as taking screenshots on an actual device, and debugging any reported errors.

Yep, we're definitely approaching a release.