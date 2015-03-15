# Introduction #

This is a scratch wiki page to enable collaboration.


# New Changes #

Ko Soe Min's Zawgyi-2009 format is now supported. This format was intended to free up space in the U+1050+ block. I have written a converter which changes Zawgyi-2009 to Zawgyi-2008. This is used directly **before** displaying text; dictionaries can be stored in Zawgyi 2009 and then displayed in Zawgyi 2008. Our converter has a few key qualities:
  * It is fast, and uses little memory (three passes, no regular expressions)
  * It is relatively accurate (still "experimental", but results are decent, see below)
  * It usually is safe to run the algorithm on Zawgyi-2008 text. In fact, it sometimes fixes minor syntax errors (see below).

Here's a screenshot:

![http://ornagai-mobile.googlecode.com/svn/trunk/images/06%20-%20format_converter.png](http://ornagai-mobile.googlecode.com/svn/trunk/images/06%20-%20format_converter.png)


# From Here #

From here, it's just bug-fixing until the final release. :D