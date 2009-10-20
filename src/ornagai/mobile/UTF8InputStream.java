/*
 * This code is licensed under the terms of the MIT License.
 * Please see the file LICENSE.TXT for the full license text.
 */

package ornagai.mobile;

import java.io.*;


/**
 * A simple stream wrapper that converts bytes into UTF-8-encoded characters.
 * This class cannot handle letters outside the Basic Multilingual Plane.
 * It may or may not (most likely not) be thread-safe.
 *
 * @author Seth N. Hetu
 */
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

    synchronized private int readByte() throws IOException {
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

        int res = buffer[atID++];
        return (res&0xFF);
    }

    synchronized public final char readChar() throws IOException {
        //First, build the character based on its UTF-8 hints
        char c = '\0';
        int b = readByte();
        if ((b&0xF8)==0xF0 || MZMobileDictionary.debug_outside_bmp) {
            //We can't handle anything outside the BMP
            throw new IllegalArgumentException("Can't handle letters outside the BMP");
        } else if ((b&0xF0)==0xE0) {
            //This letter is composed of three bytes
            int b2 = readByte();
            int b3 = readByte();
            if ((b2&0xC0)!=0x80 || (b3&0xC0)!=0x80)
                throw new IllegalArgumentException("Invalid multi-byte UTF-8 stream.");
            c = (char)(((b&0xF)<<12) | ((b2&0x3F)<<6) | (b3&0x3F));
            if (c < 0x0800 || c > 0xFFFF) {
                throw new IllegalArgumentException("Invalid UTF-8 stream.");
            }
        } else if ((b&0xE0)==0xC0) {
            //This letter is composed of two bytes
            int b2 = readByte();
            if ((b2&0xC0)!=0x80)
                throw new IllegalArgumentException("Invalid multi-byte UTF-8 stream.");
            c = (char)(((b&0x1F)<<6) | (b2&0x3F));
            if (c < 0x80 || c > 0x07FF) {
                throw new IllegalArgumentException("Invalid UTF-8 stream.");
            }
        } else {
            //This letter is composed of a single byte
            c = (char)(b&0x7F);
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

