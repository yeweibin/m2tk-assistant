/*
 *  Copyright (c) M2TK Project. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package m2tk.assistant.api.domain;

import lombok.Data;

@Data
public class StreamDensityBulk
{
    private int id;
    private int pid;
    private int bulkSize;
    private byte[] bulkEncoding;
    private long startPosition;
    private long maxDensity;
    private long minDensity;
    private long avgDensity;

    public final long[] getDensities()
    {
        long[] array = new long[bulkSize];
        int offset = 0, i = 0;
        while (offset < bulkEncoding.length)
        {
            int firstByte = (bulkEncoding[offset] & 0xFF);
            if (firstByte == 0b11111111)
            {
                array[i] = Integer.MAX_VALUE;
                offset += 1;
                i ++;
            } else if ((firstByte & 0b10000000) == 0)
            {
                array[i] = firstByte;
                offset += 1;
                i ++;
            } else if ((firstByte & 0b11100000) == 0b11000000)
            {
                long density = (firstByte & 0b00011111);
                density = (density << 6) | (bulkEncoding[offset + 1] & 0b00111111);
                array[i] = density;
                offset += 2;
                i ++;
            } else if ((firstByte & 0b11110000) == 0b11100000)
            {
                long density = (firstByte & 0b00001111);
                density = (density << 6) | (bulkEncoding[offset + 1] & 0b00111111);
                density = (density << 6) | (bulkEncoding[offset + 2] & 0b00111111);
                array[i] = density;
                offset += 3;
                i ++;
            } else if ((firstByte & 0b11111000) == 0b11110000)
            {
                long density = (firstByte & 0b00000111);
                density = (density << 6) | (bulkEncoding[offset + 1] & 0b00111111);
                density = (density << 6) | (bulkEncoding[offset + 2] & 0b00111111);
                density = (density << 6) | (bulkEncoding[offset + 3] & 0b00111111);
                array[i] = density;
                offset += 4;
                i ++;
            } else if ((firstByte & 0b11111100) == 0b11111000)
            {
                long density = (firstByte & 0b00000011);
                density = (density << 6) | (bulkEncoding[offset + 1] & 0b00111111);
                density = (density << 6) | (bulkEncoding[offset + 2] & 0b00111111);
                density = (density << 6) | (bulkEncoding[offset + 3] & 0b00111111);
                density = (density << 6) | (bulkEncoding[offset + 4] & 0b00111111);
                array[i] = density;
                offset += 5;
                i ++;
            } else if ((firstByte & 0b11111100) == 0b11111100)
            {
                long density = (firstByte & 0b00000001);
                density = (density << 6) | (bulkEncoding[offset + 1] & 0b00111111);
                density = (density << 6) | (bulkEncoding[offset + 2] & 0b00111111);
                density = (density << 6) | (bulkEncoding[offset + 3] & 0b00111111);
                density = (density << 6) | (bulkEncoding[offset + 4] & 0b00111111);
                density = (density << 6) | (bulkEncoding[offset + 5] & 0b00111111);
                array[i] = density;
                offset += 6;
                i ++;
            }
        }
        return array;
    }
}
