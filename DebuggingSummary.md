# Introduction #

This page lists some details for the bugs I've fixed, to help give new developers a feel for what kind of things have already been solved.


# Proper Result Sorting #

![http://ornagai-mobile.googlecode.com/svn/trunk/images/07%20-%20debug_position_with_duplicates.png](http://ornagai-mobile.googlecode.com/svn/trunk/images/07%20-%20debug_position_with_duplicates.png)

Consider the left-most image. The word "abacus" was searched for, so it appears directly in place of where the word "abacus" is located in the dictionary. This is sensible, but what happens if you search for a word that doesn't exist? Consider "abacl" --if this _were_ a word, it would appear directly after "aback" and before "abacus". So, our "not found" result is located there. Now, consider a word that matches partly, but not all the way: "abacu". This should also appear directly before "abacus".

So, these three screenshots represent the correct operation of the dictionary. If we were just storing words as a list of strings, it would be easy to figure out where to insert the result list. However, if you have a look at our DictionaryFormat page, you'll see that the binary format makes this a lot more difficult.


# Skipping Existing Primary Results #

![http://ornagai-mobile.googlecode.com/svn/trunk/images/08%20-%20results%20without%20duplicates.png](http://ornagai-mobile.googlecode.com/svn/trunk/images/08%20-%20results%20without%20duplicates.png)

If you search for the word "eagle", there are several matches (see above). The "primary" matches ("eagle", "eagle eye", "eagle-eyed") also appear directly after the result list. This is confusing to new users, and annoying if you have a lot of results (e.g., search for "a" and see how long it takes to scroll through everything twice). The left image shows this phenomenon in action.

The center and right panels show how it should look: if the result set contains a primary match, that result will no longer be in the regular word list. So, the only duplicate is "golden eagle", since removing that from the "g" section would make little sense. Note also that finding no result (or only secondary matches) will not affect the list.

This is one of those bugfixes that is difficult to describe, but easy to notice if you just use the software. Try loading a custom, text-format dictionary and seeing how weird the interface feels --due to the un-sorted nature of the text dictionary format, we are not able to remove primary matches from the regular list.


# Better Error Messages #

![http://ornagai-mobile.googlecode.com/svn/trunk/images/09%20-%20better_error_dialogs.png](http://ornagai-mobile.googlecode.com/svn/trunk/images/09%20-%20better_error_dialogs.png)

Exceptions are caught and displayed by LWUIT in a Dialog component (left image). In our case, the default style made this unreadable. Moreover, we wanted to give users an idea what exactly went wrong. For example, if they made a dictionary containing characters outside the BMP, the default dialog would only show an "IllegalArgumentException". Finally, since this represents an **error**, having the "Ok" button return you to the program was bad --we wanted to exit the program!

The right-hand picture shows our improved error dialog. It's not perfect (the line breaking is a bit finicky), but it's a far step above the previous method.


# Hardware Testing #

![http://ornagai-mobile.googlecode.com/svn/trunk/images/10%20-%20works_on_hardware.png](http://ornagai-mobile.googlecode.com/svn/trunk/images/10%20-%20works_on_hardware.png)

The photo is fuzzy because it was taken with my webcam, but that's the real program running on my real phone. I'm pleased to report that the default dictionary works perfectly (the list is a little sluggish, but we can forgive it). The custom dictionary dialog is a bit glitchy, so the next round of debugging will focus on that.

Hopefully, LWUIT will hide most of the details of different platforms, allowing hardware testing to proceed at pace.