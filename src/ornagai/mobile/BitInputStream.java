package ornagai.mobile;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

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
        //System.out.println("On byte: " + Integer.toHexString((0xFF&buffer[currByte])));
    }

    int match = buffer[currByte] & (1<<(7-nextBit++));
    return match == 0 ? 0 : 1;
  }

  //Slow, but at least it works for now.
  synchronized public int readNumber(int bits) throws IOException {
      int value = 0;
      while (bits>0) {
          int bit = readBit();
          //System.out.println(" bit: " + bit);
          value <<= 1;
          value |= bit;
          bits--;
      }
      return value;
  }

  synchronized public int getBitsRead() {
      return bitsRead;
  }

  public void close() throws IOException {
    in.close();
    in = null;
  }
}

