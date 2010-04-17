/*
 * Copyright 2006-2007 The Scriptella Project Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scriptella.driver.ldap.ldif;

import scriptella.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Utility methods from the following apache classes:
 * <ul>
 * <li>org.apache.directory.shared.ldap.util.StringTools</li>
 * <li>org.apache.directory.shared.ldap.util.Base64</li>
 * </ul>
 */
class Utils {
    /** Hex chars */
    private static final byte[] HEX_CHAR = new byte[]
        { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };


    private Utils() {
    }

    /**
     * Test if the current character is equal to a specific character.
     *
     * @param chars
     *            The buffer which contains the data
     * @param index
     *            Current position in the buffer
     * @param car
     *            The character we want to compare with the current buffer
     *            position
     * @return <code>true</code> if the current character equals the given
     *         character.
     */
    public static boolean isCharASCII( char[] chars, int index, char car )
    {
        if ( ( chars == null ) || ( chars.length == 0 ) || ( index < 0 ) || ( index >= chars.length ) )
        {
            return false;
        }
        else
        {
            return chars[index] == car;
        }
    }

    /**
     * Return an UTF-8 encoded String
     *
     * @param bytes
     *            The byte array to be transformed to a String
     * @return A String.
     */
    public static String utf8ToString( byte[] bytes )
    {
        if ( bytes == null )
        {
            return "";
        }

        try
        {
            return new String( bytes, "UTF-8" );
        }
        catch ( UnsupportedEncodingException uee )
        {
            return "";
        }
    }

    /** '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' */
        public static final boolean[] DIGIT =
            {
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                true,  true,  true,  true,  true,  true,  true,  true,
                true,  true,  false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false
            };


    /**
     * Test if the current character is a digit <digit> ::= '0' | '1' | '2' |
     * '3' | '4' | '5' | '6' | '7' | '8' | '9'
     *
     * @param chars
     *            The buffer which contains the data
     * @param index
     *            Current position in the buffer
     * @return <code>true</code> if the current character is a Digit
     */
    public static boolean isDigit( char[] chars, int index )
    {
        if ( ( chars == null ) || ( chars.length == 0 ) || ( index < 0 ) || ( index >= chars.length ) )
        {
            return false;
        }
        else
        {
            return !((chars[index] > 127) || !DIGIT[chars[index]]);
        }
    }


    //BASE64 Section from org.apache.directory.shared.ldap.util.Base64 class

    /**
     * Decodes a BASE-64 encoded stream to recover the original data. White
     * space before and after will be trimmed away, but no other manipulation of
     * the input will be performed. As of version 1.2 this method will properly
     * handle input containing junk characters (newlines and the like) rather
     * than throwing an error. It does this by pre-parsing the input and
     * generating from that a count of VALID input characters.
     *
     * @param a_data
     *            data to decode.
     * @return the decoded binary data.
     */
    public static byte[] base64Decode( char[] a_data )
    {
        // as our input could contain non-BASE64 data (newlines,
        // whitespace of any sort, whatever) we must first adjust
        // our count of USABLE data so that...
        // (a) we don't misallocate the output array, and
        // (b) think that we miscalculated our data length
        // just because of extraneous throw-away junk

        int l_tempLen = a_data.length;
        for (char anA_data1 : a_data) {
            if ((anA_data1 > 255) || s_codes[anA_data1] < 0) {
                --l_tempLen; // ignore non-valid chars and padding
            }
        }
        // calculate required length:
        // -- 3 bytes for every 4 valid base64 chars
        // -- plus 2 bytes if there are 3 extra base64 chars,
        // or plus 1 byte if there are 2 extra.

        int l_len = ( l_tempLen / 4 ) * 3;

        if ( ( l_tempLen % 4 ) == 3 )
        {
            l_len += 2;
        }

        if ( ( l_tempLen % 4 ) == 2 )
        {
            l_len += 1;
        }

        byte[] l_out = new byte[l_len];

        int l_shift = 0; // # of excess bits stored in accum
        int l_accum = 0; // excess bits
        int l_index = 0;

        // we now go through the entire array (NOT using the 'tempLen' value)
        for (char anA_data : a_data) {
            int l_value = (anA_data > 255) ? -1 : s_codes[anA_data];

            if (l_value >= 0) // skip over non-code
            {
                l_accum <<= 6; // bits shift up by 6 each time thru
                l_shift += 6; // loop, with new bits being put in
                l_accum |= l_value; // at the bottom. whenever there
                if (l_shift >= 8) // are 8 or more shifted in, write them
                {
                    l_shift -= 8; // out (from the top, leaving any excess
                    l_out[l_index++] = // at the bottom for next iteration.
                            (byte) ((l_accum >> l_shift) & 0xff);
                }
            }
            // we will also have skipped processing a padding null byte ('=')
            // here;
            // these are used ONLY for padding to an even length and do not
            // legally
            // occur as encoded data. for this reason we can ignore the fact
            // that
            // no index++ operation occurs in that special case: the out[] array
            // is
            // initialized to all-zero bytes to start with and that works to our
            // advantage in this combination.
        }

        // if there is STILL something wrong we just have to throw up now!
        if ( l_index != l_out.length )
        {
            throw new Error( "Miscalculated data length (wrote " + l_index + " instead of " + l_out.length + ")" );
        }

        return l_out;
    }

    /** lookup table for converting base64 characters to value in range 0..63 */
    private static byte[] s_codes = new byte[256];

    static
    {
        for ( int ii = 0; ii < 256; ii++ )
        {
            s_codes[ii] = -1;
        }

        for ( int ii = 'A'; ii <= 'Z'; ii++ )
        {
            s_codes[ii] = ( byte ) ( ii - 'A' );
        }

        for ( int ii = 'a'; ii <= 'z'; ii++ )
        {
            s_codes[ii] = ( byte ) ( 26 + ii - 'a' );
        }

        for ( int ii = '0'; ii <= '9'; ii++ )
        {
            s_codes[ii] = ( byte ) ( 52 + ii - '0' );
        }

        s_codes['+'] = 62;
        s_codes['/'] = 63;
    }

    /**
     * Helper function that dump an array of bytes in hex form
     *
     * @param buffer
     *            The bytes array to dump
     * @return A string representation of the array of bytes
     */
    public static String dumpBytes( byte[] buffer )
    {
        if ( buffer == null )
        {
            return "";
        }

        StringBuilder sb = new StringBuilder(2+buffer.length*2);

        for (byte b : buffer) {
            sb.append("0x").append((char) (HEX_CHAR[(b & 0x00F0) >> 4])).append(
                    (char) (HEX_CHAR[b & 0x000F])).append(" ");
        }

        return sb.toString();
    }

    public static byte[] toByteArray(InputStream is, long maxLength) throws IOException {
        return IOUtils.toByteArray(is, maxLength);
    }








}
