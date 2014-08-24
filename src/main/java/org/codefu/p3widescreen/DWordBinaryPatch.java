package org.codefu.p3widescreen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class DWordBinaryPatch {
    private static final int FILL_IN_DWORD = -1;
    private Logger logger = LoggerFactory.getLogger(DWordBinaryPatch.class);
    private final long offset;
    private final int patchLength;
    private final int numValuesToFillIn;
    private final int pattern[];

    public DWordBinaryPatch(long offset, String patternStr) {
        this.offset = offset;
        String[] patternStrs = patternStr.trim().split("\\s+");
        int patchLength = 0;
        int numValuesToFillIn = 0;
        pattern = new int[patternStrs.length];
        for (int i = 0; i < patternStrs.length; i++) {
            if (patternStrs[i].equals("?")) {
                numValuesToFillIn++;
                patchLength += 4;
                pattern[i] = FILL_IN_DWORD;
            } else {
                patchLength += 1;
                pattern[i] = Integer.parseInt(patternStrs[i], 16);
            }
        }
        this.patchLength = patchLength;
        this.numValuesToFillIn = numValuesToFillIn;
    }

    private void patch(RandomAccessFile file, boolean testOnly,
                       int... patchValues)
            throws IOException {
        byte bytes[] = new byte[patchLength];
        file.seek(offset);
        int numRead = file.read(bytes);
        if (numRead != patchLength) {
            throw new PatchException(String.format(
                "expected %d byte(s) at offset 0x%x but only read %d",
                patchLength, offset, numRead));
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int valuesPos = 0;
        for (int patternByte : pattern) {
            if (patternByte == FILL_IN_DWORD) {
                if (testOnly) {
                    buffer.position(buffer.position() + 4);
                } else {
                    buffer.putInt(patchValues[valuesPos++]);
                }
            } else if ((buffer.get() & 0xff) != patternByte) {
                int errorPosition = buffer.position() - 1;
                throw new PatchException(String.format(
                    ("expected byte 0x%02x at offset 0x%x but found 0x%02x"
                     + " instead"),
                    patternByte, offset + errorPosition,
                    buffer.get(errorPosition)));
            }
        }
        if (!testOnly) {
            file.seek(offset);
            file.write(bytes);
        }
    }

    /**
     * Test whether this patch can be applied to the file.
     * @param file Binary file against which to test this patch.
     * @return A message explaining why the patch cannot be applied, or
     *         else null if the patch can be applied.
     * @throws IOException
     */
    public String testPatch(RandomAccessFile file) throws IOException {
        try {
            patch(file, true);
        } catch (PatchException e) {
            return e.getMessage();
        }
        return null;
    }

    public void patch(RandomAccessFile file, int... patchValues)
            throws IOException {
        if (patchValues.length != numValuesToFillIn) {
            throw new RuntimeException(String.format(
                "expected %d value(s) to fill in but got %d instead",
                numValuesToFillIn, patchValues.length));
        }
        patch(file, false, patchValues);
    }
}
