# Introduction #

This is a scratch wiki page to enable collaboration.


# New Changes #

I put the results component on the same page as the search, to make navigation between this and the search bar easier. (Otherwise, it's almost impossible to re-start a search from the List component).

Also, I re-positioned the dictionary text. If there's exactly two "|" characters in a definition, we split into three Strings and format them to show properly as "**Word**", "_Part of Speech_" and "Definition". If any of these contain Zawgyi text, the special formatting is not used, since we have to use our bitmapped font. I'll try to get around that by double-drawing for bold text, since I know most parts-of-speech are in English (and the word really should look bold).

Here's a screenshot:

![http://ornagai-mobile.googlecode.com/svn/trunk/images/01%20-%20phone_formats%20results.png](http://ornagai-mobile.googlecode.com/svn/trunk/images/01%20-%20phone_formats%20results.png)


# From Here #

We need to segment the text, so that we don't break to a new line halfway through a word. That also requires that we overhaul "ZawgyiComponent" to allow it to measure strings (right now, it only thinks in single-character intervals).

After that, I'll move on to importing the dictionary. I want to add in the Burmese-English conversion, but we need the data first.

I want to add a three-letter string to the dictionary name, e.g., "wpd", which specifies the order of the order of the **w** ord, **p** art of speech, and **d** efinition in the dictionary file. Currently, the MM2EN dictionary reverses this, so I'd like to make it easy to change (just rename the file).

To input Burmese, I think we could list all the consonants (standard reverse-pyramid format found in Burmese text books) as buttons. (There should be one more button called "Other", since there are a few non-standard consonants.) Then, the user can click on each button to spell out his word consonant by consonant.

E.g., for "မဂၤလာပါ", the user could choose "မလပ".

At this point, the dictionary "search" button would pull up all words matching:
"%မ%လ%ပ%", where "%" means "any letters".

...we would modify the regex slightly to take pat-sint words into account. This is is just the general idea.

The Chinese do a similar thing with Pinyin input, where some margin of error is tolerable (e.g., at a karaoke booth). It's fast and easy to learn (the other option is to import WaitZar~ or one of the Myanmar Mobile solutions, both of which are too "heavy" just for casual searching, or a soft keyboard of all letters, which is waaaay too hard to learn for just our app).



