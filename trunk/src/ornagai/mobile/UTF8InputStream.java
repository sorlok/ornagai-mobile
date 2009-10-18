package ornagai.mobile;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class UTF8InputStream {

    private InputStream in;
    private byte[] buffer = new byte[1024];
    private int atID;
    private int bufferSize;
    private boolean pastStreamEnd;

    public UTF8InputStream(InputStream in) {
        this.in = in;
        this.pastStreamEnd = false;
    }

    synchronized private byte readByte() throws IOException {
        //Impossible to read?
        if (in == null) {
            throw new IOException("Already closed");
        }

        //Invalid?
        if (pastStreamEnd) {
            return 0;
        }

        //Need to read a new
        if (atID >= bufferSize) {
            bufferSize = in.read(buffer);
            atID = 0;
            if (bufferSize == -1) {
                pastStreamEnd = true;
                return 0;
            }
        }

        return buffer[atID++];
    }

    synchronized public char readChar() throws IOException {
        //First, build the character based on its UTF-8 hints
        char c = '\0';
        byte b = readByte();
        if (((((int) (b >> 3)) & 0xFF) ^ 0x1E) == 0 || MZMobileDictionary.debug_outside_bmp) {
            //We can't handle anything outside the BMP
            throw new IllegalArgumentException("Can't handle letters outside the BMP");
        } else if (((((int) (b >> 4)) & 0xFF) ^ 0xE) == 0) {
            //This letter is composed of three bytes
            byte b2 = readByte();
            byte b3 = readByte();
            c = (char)((((int) (b & 0xF)) << 12) | (((int) (b2 & 0x3F)) << 6) | ((int) (b3 & 0x3F)));
            if (c < 0x0800 || c > 0xFFFF) {
                throw new IllegalArgumentException("Invalid UTF-8 stream.");
            }
        } else if (((((int) (b >> 5)) & 0xFF) ^ 0x6) == 0) {
            //This letter is composed of two bytes
            byte b2 = readByte();
            c = (char)((((int) (b & 0x1F)) << 6) | ((int) (b2 & 0x3F)));
            if (c < 0x80 || c > 0x07FF) {
                throw new IllegalArgumentException("Invalid UTF-8 stream.");
            }
        } else {
            //This letter is composed of a single byte
            c = (char)(((int) (b & 0xFF)));
        }

        //Now return that character. The "past stream" variable should be checked later
        return c;
    }

    synchronized public String readUntil(char stopChar, char ignoreChar) throws IOException {
        StringBuffer sb = new StringBuffer();
        for (;;) {
            char c = readChar();
            if (pastStreamEnd || c == stopChar) {
                break;
            } else if (c != ignoreChar) {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    synchronized public boolean isDone() {
        return pastStreamEnd;
    }
}

