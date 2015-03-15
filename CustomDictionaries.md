# Introduction #

Starting with version 2.0, the Ornagai Mobile Dictionary can load custom user dictionaries. The process is simple, and has been tested successfully on hardware.


# Creating a Dictionary #

Before creating a dictionary, try downloading one of our pre-built dictionaries and testing it on your phone. We have two identical dictionaries for Basic English:
  * http://ornagai-mobile.googlecode.com/files/basic_english_bin.mzdict.zip
  * http://ornagai-mobile.googlecode.com/files/basic_english_text.mzdict.zip

If you would like to create your own dictionary, please see the OrnagaiCreator wiki page. The Ornagai Dictionary Creator is very easy to use, and will create an optimized binary dictionary for you.
  * http://ornagai-mobile.googlecode.com/files/dictionary_creator_1.0.zip

Of course, you could always create a dictionary by hand. The DictionaryFormat wiki page explains the internal details of our two formats. Creating a dictionary by hand is very difficult, and we do not recommend that you try this. However, we've included the necessary information for curious hackers:
  * DictionaryFormat


# Installing a dictionary #

You will need to transfer the dictionary you downloaded/created to your phone. We recommend putting it on your SD card, since this makes it more portable. Then, start the Ornagai Mobile Dictionary on your phone.

| ![http://ornagai-mobile.googlecode.com/svn/trunk/images/J08%20-%20program_running.jpg](http://ornagai-mobile.googlecode.com/svn/trunk/images/J08%20-%20program_running.jpg) |
|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

You should choose the "Options" tab. You might be prompted to allow Ornagai Mobile to read the filesystem; make sure you click "Ok".

| ![http://ornagai-mobile.googlecode.com/svn/trunk/images/J10%20-%20options_panel_nofc.jpg](http://ornagai-mobile.googlecode.com/svn/trunk/images/J10%20-%20options_panel_nofc.jpg) | ![http://ornagai-mobile.googlecode.com/svn/trunk/images/J09%20-%20options_panel.jpg](http://ornagai-mobile.googlecode.com/svn/trunk/images/J09%20-%20options_panel.jpg) |
|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

You should see a section for loading an "External Dictionary" (right image). If you see an error message instead (left image), that means one of two things:
  * Your downloaded the "limited" release of ornagai-mobile. Please download the standard release from our downloads page.
  * Your phone does not support loading external files using Java. In this case, you cannot use custom dictionaries with ornagai-mobile.

| ![http://ornagai-mobile.googlecode.com/svn/trunk/images/J11%20-%20data_warning.jpg](http://ornagai-mobile.googlecode.com/svn/trunk/images/J11%20-%20data_warning.jpg) | ![http://ornagai-mobile.googlecode.com/svn/trunk/images/J12%20-%20file_roots.jpg](http://ornagai-mobile.googlecode.com/svn/trunk/images/J12%20-%20file_roots.jpg) |
|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------|

Click on the "Browse..." button to bring up a file explorer. You might have to accept a security warning (left image); please click "Ok". Eventually, you will see a list of the "root" locations on your phone (right image). Click on the one where you saved the dictionary (it might be named "SD Card" or "Memory Card" or "C:/", or something similar).

**NOTE:** If you are using the standard distribution, you will be given a LOT of security certificate prompts. This is not our fault; [phone manufacturers have made it impossible to develop for Java phones](http://javablog.co.uk/2007/08/09/how-midlet-signing-is-killing-j2me/). If your phone supports private certificates, you can try downloading the "signed" distribution from our downloads section; that will only prompt you one time, when you install the program.

| ![http://ornagai-mobile.googlecode.com/svn/trunk/images/J14%20-%20file_types_known.jpg](http://ornagai-mobile.googlecode.com/svn/trunk/images/J14%20-%20file_types_known.jpg) | ![http://ornagai-mobile.googlecode.com/svn/trunk/images/J13%20-%20file_types_unknown.jpg](http://ornagai-mobile.googlecode.com/svn/trunk/images/J13%20-%20file_types_unknown.jpg) |
|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

You must then browse to your dictionary file. If you are using the normal (unsigned) distribution, you will see a lot of "unknown" files and folders (right image). If you are using the signed distribution, you will see a nice folder layout, which marks folder as "full" or "empty", and hides files that do not end in **.mzdict.zip (left image). Either way, you can browse to your dictionary, and then click "Ok".**

| ![http://ornagai-mobile.googlecode.com/svn/trunk/images/J15%20-%20updated_path.jpg](http://ornagai-mobile.googlecode.com/svn/trunk/images/J15%20-%20updated_path.jpg) |
|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------|

You should now see the path to your dictionary listed in the options box. Click "Save" to load the custom dictionary. You will be sent back to the main Ornagai Mobile start page.

| ![http://ornagai-mobile.googlecode.com/svn/trunk/images/J16%20-%20custom_search.jpg](http://ornagai-mobile.googlecode.com/svn/trunk/images/J16%20-%20custom_search.jpg) |
|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

You can now search your custom dictionary.

# Troubleshooting #

The custom dictionary feature is **EXPERIMENTAL**. We have tested it heavily, but we expect some bugs to remain. Please contact us if you experience any problems.

In addition, you can check out the Troubleshooting wiki page for answers to common questions, like what to do if your dictionary fails to load, or if you get constant loading prompts while using your dictionary.