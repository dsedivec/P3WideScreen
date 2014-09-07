# Patch Patrician 3's Resolution

I bought [Patrician 3 from GOG][].  It was pretty fun but it's 1024x768 or 1280x1024 resolutions looked dumb on my very wide screen MacBook Pro at 1440x900.  I subsequently read about several people who had patched foreign or older versions of the game.  However, none of the easy methods for patching the game (hex editor instructions, patch utilities) worked on GOG's 2.0.0.5 version, so I made this Java program to make patching GOG's latest version easy for everyone--hopefully.

[Patrician 3 from GOG]: http://www.gog.com/game/patrician_3


## How To Use This

**NOTE: I take no responsibility if this breaks your game, deletes your save games, or turns your computer into a murderous AI bent on world domination!**

This will patch the Patrician 3 game to use a resolution that you select instead of the default 1280x1024.  **The game will still say 1280x1024 in its settings** but it will actually be running at your selected resolution instead.

1. Back up your game, including all save games!
2. Download and install Patrician 3 from GOG.
3. [Install a Java 8 JRE][java-8] or JDK, if you don't have one installed already.  **It must be Java version 8 or later!**
4. [Download the latest `P3WideScreen.jar` file][P3WideScreen.jar].
5. Run the `P3WideScreen.jar` file (hopefully you can just double click it, or else `java -jar P3WideScreen.jar`):
     a. Select the directory where you have installed Patrician 3.
     b. Enter in the resolution you'd like to use instead of 1280x1024.
     c. Hit the patch button.
6. Run Patrician 3 and select the 1280x1024 resolution.

[java-8]: http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html
[P3WideScreen.jar]: https://github.com/dsedivec/P3WideScreen/releases/

While the menus may be at a different resolution, once you're actually playing the game, you should be at your selected resolution instead of 1280x1024.


## Credits

All credit for this goes to the people who figured out how to patch this game in the first place.  That seems primarily to be [brotkohl at some German Patrician forum][brotkohl].  Thanks to [billyplod at the Kalypso Media forums][billyplod] for translating that.  Thanks to [whatnames at the Widescreen Gaming Forum][whatnames] for trying to pull together all the various sources of information about how to patch this game, and for detailing how he applied them to the GOG release of Patrician 3.

[brotkohl]: http://www.patrizierforum.net/index.php?page=Thread&threadID=3376&pageNo=1
[billyplod]: http://forum.kalypsomedia.com/showthread.php?tid=8713&pid=86997#pid86997
[whatnames]: http://www.wsgf.org/forums/viewtopic.php?t=26206

I used the [CPR format page on the XeNTaX wiki][cpr] to figure out how to get the needed BMP and INI out of Patrician's CPR file.  (Though the format page there is incomplete, as it doesn't mention that the CPR file can contain many headers.  Check out my `CPRFile` class.)

[cpr]: http://wiki.xentax.com/index.php?title=Patrician


## About the Code

I'm sorry if this code sucks ass.  I haven't used Java in anger for 8-10  years.  I decided to write this program in Java because patching Patrician 3 is a pretty simple task, and because I wanted this program to be cross-platform, especially since I'm developing on a Mac.  I decided to use Java 8 because I wanted to get a taste of the new features added to Java since I last used it.  This is probably my first Java GUI program.

I developed this program with IntelliJ IDEA 13.1 Community Edition.


## License

Copyright (C) 2014  Dale Sedivec

P3WideScreen is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

P3WideScreen is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with P3WideScreen.  If not, see <http://www.gnu.org/licenses/>.
