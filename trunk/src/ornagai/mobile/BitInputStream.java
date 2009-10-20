/*
 * This code is licensed under the terms of the MIT License.
 * Please see the file LICENSE.TXT for the full license text.
 */
package ornagai.mobile;

import java.io.*;


/**
 * This class reads bits from an input stream. Although I got the idea for
 *  it while browsing several similar projects online, the code in this
 *  class is my own, and thus released under the terms of the MIT license.
 *
 * @author Seth N. Hetu
 */
public class BitInputStream
{
  private InputStream in;
  private byte[] buffer = new byte[1024];
  private int count;

  private int bitsRead;
  
  private int nextBit = 0;
  private int currByte = 0;

  //Assume we'll be reading to the end of the file
  public BitInputStream( InputStream in ) {
    this.in = in;
    try {
        count = in.read(buffer);
    } catch (IOException ex) {
        throw new RuntimeException("Can't read bit: " + ex.toString());
    }
    this.bitsRead = 0;
  }

  synchronized public int readBit() throws IOException {
    if (in == null)
      throw new IOException( "Already closed" );
    if (bitsRead==Integer.MAX_VALUE)
        throw new RuntimeException("Already read " + Integer.MAX_VALUE + " bits.");

    //Count
    bitsRead++;

    //Free to read?
    if (nextBit==8) {
        currByte++;
        nextBit = 0;
        if (currByte>=count) {
            //Read more bytes
            currByte = 0;
            try {
                count = in.read(buffer);
            } catch (IOException ex) {
                throw new RuntimeException("Can't read bit: " + ex.toString());
            }
        }
    }

    int match = buffer[currByte] & (1<<(7-nextBit++));
    return match == 0 ? 0 : 1;
  }

  //Slow, but at least it works for now.
  // Later, we can add some shifting magic to align multiple bits at once.
  synchronized public int readNumber(int bits) throws IOException {
      int value = 0;
      while (bits>0) {
          int bit = readBit();
          value <<= 1;
          value |= bit;
          bits--;
      }
      return value;
  }

  synchronized public int readNumberAt(int bitID, int bits) throws IOException {
      //Reset
      in.reset();
      in.skip(bitID/8);
      nextBit = bitID%8;

      //Buffer
      currByte = 0;
      count = in.read(buffer);
      return readNumber(bits);
  }

  synchronized public int getBitsRead() {
      return bitsRead;
  }

  public void close() throws IOException {
    in.close();
    in = null;
  }
}

