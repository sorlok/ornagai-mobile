# Introduction #

This is a scratch wiki page to enable collaboration.


# New Changes #

The original dictionary only allowed you to look up words directly; if another word preceded your word, you couldn't find it. For example, if you searched for "rope", you would not find "jump rope". The main reason the original dictionary didn't support this feature was because of search time; a secondary reason was storage space.

I've managed to load the entire dictionary (using a binary format) and a lookup table for word-by-word searching. All matches appear in **red**, and the matches are placed in the position of the searched-for word, in **gray**. The **blue** bar represents the cursor. If your word doesn't match, the words "Not found" are added to the list.

Here's a screenshot:

![http://ornagai-mobile.googlecode.com/svn/trunk/images/03-phone_complex_search.png](http://ornagai-mobile.googlecode.com/svn/trunk/images/03-phone_complex_search.png)


# From Here #

All text is encoded properly; now we just need to pull definitions from the list and display them. I think the "Not found" entry will bring the search box back into focus when you click it.

We also need to check up on small searches; I know that "a" will fail, for some reason. Finally, we should check searching for "aa" (a weird "null node" case) and see how to avoid errors caused by this and other obscure search patterns.

I want to make a smaller MZ dictionary, with only one-word entries, to test our custom (file) dictionary loader.

Burmese searching will have to wait for the next release; I want a stable English release first.