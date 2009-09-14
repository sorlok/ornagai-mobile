package ornagai.mobile.tools;

import java.io.*;

public class BitOutputStream
{
  private OutputStream out;
  private int buffer;
  private int bitCount;

  public BitOutputStream( OutputStream out ) {
    this.out = out;
  }

  synchronized public void writeBit( int bit ) throws IOException {
    if (out==null)
      throw new IOException( "Already closed" );

    if (bit != 0 && bit != 1) {
      throw new IOException( bit+" is not a bit" );
    }

    buffer <<= 1;
    buffer |= (bit);
    bitCount++;

    if (bitCount==8) {
      flush();
    }
  }

  synchronized public void writeNumber(int value, int bits) throws IOException {
      String repres = Integer.toBinaryString(value);
      if (repres.length() > bits)
          throw new IllegalArgumentException("Too few bits: " + bits + " for number: " + value);

      //Pad
      int pad = bits - repres.length();
      for (int i=0; i<pad; i++)
          writeBit(0);

      //Write
      for (char c : repres.toCharArray())
          writeBit(c=='1' ? 1 : 0);
  }

  private void flush() throws IOException {
    if (bitCount>0) {
      out.write( (byte)buffer );
      bitCount=0;
      buffer=0;
    }
  }

  public void close() throws IOException {
    flush();
    out.close();
    out = null;
  }
}

