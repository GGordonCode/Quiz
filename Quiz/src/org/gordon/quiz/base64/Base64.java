package org.gordon.quiz.base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Base64 {
    public static void main(String[] args) {
        byte[] msg = "Man is distinguished, not only by his reason, but by this singular passion from".getBytes();
        System.out.println(encode(msg));
        String encoded = "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlzIHNpbmd1bGFyIHBhc3Npb24gZnJvbQ==";
        byte[] decoded = decode(encoded);
        System.out.println(new String(decoded));
    }
    
    /**
     * Base64 encodes a buffer of bytes.  Padding of "=" are added as needed.  Note
     * we don't actually need those to decode, oddly.
     * @param buf the buffer to encode
     * @return the base 64-encoded string
     */
    public static String encode(byte[] buf) {
        int len = buf.length;
        if (len == 0) {
            return "";
        }

        // Set up the results accumulator with an approximate size estimate plus slop.
        StringBuilder encStr = new StringBuilder(((4 * len) + 2) / 3 + 2);
        int ndx = 0;
        byte b;
        byte rem = 0;
        
        // Loop through each of the input bytes.  Given the 3:4 mapping, we essentially
        // have a finite state machine, whose current state number is captured by the
        // modulus of the current loop index.  Each state is pretty much intuitive, in
        // that we need to write out every sextet of contiguous bits, as they become
        // fully available.  For some of the states, the "unused" bytes are remembered
        // to be included in the next iteration in "rem".
        while (ndx < len) {
            byte curByte = buf[ndx];
            switch (ndx % 3) {
            case 0:
                // At start of word, take top 6 bits, shift right 2.
                b = (byte) ((curByte & 0xfc) >>> 2);
                encStr.append(fromByte(b));
                
                // Take last two bits of this word, shift left 4.  Write
                // it out if at the end, else hold it for the next iteration.
                rem = (byte) ((curByte & 0x03) << 4);
                if (ndx == len - 1) {
                    encStr.append(fromByte(rem));
                }
                break;
            case 1:
                // Take the saved part of the previous iteration and
                // add top 4 bits of current word shift right 4.
                b = (byte) (rem | (byte) ((curByte & 0xf0) >>> 4));
                encStr.append(fromByte(b));
                
                // Take last four bits of this word, shift left 2.  Write
                // it out if at the end, else hold it for the next iteration.
                rem = (byte) ((curByte & 0x0f) << 2);
                if (ndx == len - 1) {
                    encStr.append(fromByte(rem));
                }
                break;
            case 2:
                // Take the saved part of the previous iteration,
                // add top 2 bits of current word shift right 6.
                b = (byte) (rem | ((curByte & 0xc0) >>> 6));
                encStr.append(fromByte(b));

                // Take last six bits of this word.
                encStr.append(fromByte((byte) (curByte & 0x3f)));
                rem = 0;
                break;
            default:
                throw new IllegalStateException("Math is broken in Java!");
            }
            ndx++;
        }

        if ((len % 3) == 1) {
            encStr.append("==");
        } else if (len %3 == 2) {
            encStr.append('=');
        }

        return encStr.toString();
    }
    
    /**
     * Decode a Base64 string into a byte array.  While we handle the trailing '='
     * characters, they really aren't needed to decode.  Note unchecked exceptions
     * can be thrown if the format is invalid.  Arguably these could be checked, but
     * there is something seriously wrong if the input data isn't valid.
     * @param str the input Base64 encoded string
     * @return the byte array of the decoded data
     */
    public static byte[] decode(String str) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            char[] c = str.toCharArray();
            int len = c.length;
            byte b;
            byte fragment = 0;

            int ndx = 0;
            while (ndx < len) {
               char curc = c[ndx];
                if (curc == '=') {
                    break;
                }
                byte curb = toBase64Value(curc);

                // Loop through each of the input bytes.  Given the 4:3 mapping, we essentially
                // have a finite state machine, whose current state number is captured by the
                // modulus of the current loop index.  This issomewhat analagous to the encoding
                // algorithm.  Again, each state is pretty much intuitive, in that we need to write
                // out every octet of contiguous bits, as they become fully available.  For some of
                // the states, the "unused" bytes are remembered to be included in the next iteration
                // in "fragment".
                switch (ndx % 4) {
                case 0:
                    // Validity check: the first character in the loop of 4 cannot
                    // yield a complete byte, so it can't be the last datum.
                    if (ndx == len - 1 || c[ndx + 1] == '=') {
                        throw new IllegalArgumentException("Unexpected character at position " + ndx + ": " + curc);
                    }

                    // Grab the 6 bits of the first byte and shift into place.
                    fragment = (byte) (curb << 2);
                    break;
                case 1:
                    // Combine the upper 6 bits from previous iteration with
                    // the two upper bits of this letter shifted right 4.
                    b = (byte) (((curb & 0x30) >>> 4) | fragment);
                    baos.write(b & 0xff);
                    
                    // Save the lower 4 bits of this letter and shift them into place.
                    fragment = (byte) ((curb & 0x0f) << 4);
                    
                    // Validity check: if this is the last character, we can't have
                    // partial (non-zero) data remaining.
                    if ((ndx == len - 1 || c[ndx + 1] == '=') && fragment != 0) {
                        throw new IllegalArgumentException("Unexpected character at position " + ndx + ": " + curc);
                    }
                    break;
                case 2:
                    // Combine the 4 bits from the previous iteration with the upper
                    // four bits of this letter.
                    b = (byte) (((curb & 0x3c) >>> 2) | fragment);
                    baos.write(b & 0xff);
                    
                    // Save the lower 2 bits of this letter and shift them into place.
                    fragment = (byte) ((curb & 0x03) << 6);
                    
                    // Validity check: if this is the last character, we can't have
                    // partial (non-zero) data remaining.
                    if ((ndx == len - 1 || c[ndx + 1] == '=') && fragment != 0) {
                        throw new IllegalArgumentException("Unexpected character at position " + ndx + ": " + curc);
                    }
                    break;
                case 3:
                    // Combine the upper two bits from the previous iteration
                    // with the 6 bits (in their current location) from this one.
                    b = (byte) (curb | fragment);
                    baos.write(b & 0xff);
                    
                    fragment = 0;
                    break;
                default:
                    throw new IllegalStateException("Math is broken in Java!");
                }
                ndx++;
            }
            baos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Byte arrays are broken in Java!");
        }
    }

    // Map from a 6-bit byte value to the Base64 character.
    private static char fromByte(byte val) {
        if (val >= 0 && val < 26) {
            return (char) ('A' + val);
        } else if (val >= 26 && val < 52) {
            return (char) ('a' + (val - 26));
        } else if (val >= 52 && val < 62) {
            return (char) ('0' + (val - 52));
        } else if (val == 62) {
            return '+';
        } else if (val == 63) {
            return '/';
        } else {
            throw new IllegalArgumentException("invalid byte value: " + val);
        }
    }
    
    // Map from the Base64 character to the six bit byte value.
    private static byte toBase64Value(char ch) {
        if (ch >= 'A' && ch <= 'Z') {
            return (byte) (ch - 'A');
        } else if (ch >= 'a' && ch <= 'z') {
            return (byte) (ch - 'a' + 26);
        } else if (ch >= '0' && ch <= '9') {
            return (byte) (ch - '0' + 52);
        } else if (ch == '+') {
            return (byte) 62;
        } else if (ch == '/') {
            return (byte) 63;
        } else {
            throw new IllegalArgumentException("invalid char value: " + ch);
        }
    }
}
