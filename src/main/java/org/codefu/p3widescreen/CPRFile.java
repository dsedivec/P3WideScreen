package org.codefu.p3widescreen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;

public class CPRFile {
    private static final Logger logger = LoggerFactory.getLogger(CPRFile.class);

    private final RandomAccessFile fp;
    private final HashMap<String, IndexEntry> index = new HashMap<>();

    private class IndexEntry {
        public final long offset;
        public final long length;

        public IndexEntry(long offset, long length) {
            this.offset = offset;
            this.length = length;
        }
    }

    public CPRFile(File file) throws IOException {
        /// XXX fp is a shit name
        fp = new RandomAccessFile(file, "r");
        String signature = readNullTerminatedString();
        if (!signature.equals("ASCARON_ARCHIVE V0.9")) {
            throw new RuntimeException("invalid signature");
        }
        long nextHeader = 0x20;
        while (true) {
            fp.seek(nextHeader);
            long headerLength = readLittleEndianInt();
            // This is something like length of the header minus the first
            // four bytes?  We don't appear to need it right now.
            fp.skipBytes(4);
            long numFiles = readLittleEndianInt();
            long nextHeaderOffset = readLittleEndianInt();
            logger.debug("read nextHeaderOffset {}", nextHeaderOffset);
            for (int i = 0; i < numFiles; i++) {
                readIndexEntry();
            }
            if (nextHeaderOffset <= 0) {
                break;
            }
            nextHeader = nextHeader + headerLength + nextHeaderOffset;
        }
    }

    private void readIndexEntry() throws IOException {
        long offset = readLittleEndianInt();
        long length = readLittleEndianInt();
        if (offset <= 0 || length <= 0) {
            throw new RuntimeException("invalid offset and/or length");
        }
        // Don't know what this is, I think it's always 1?
        fp.skipBytes(4);
        String name = readNullTerminatedString();
        logger.debug("read file={} offset={} length={}", name, offset, length);
        index.put(name, new IndexEntry(offset, length));
    }

    private long readLittleEndianInt() throws IOException {
        byte bytes[] = new byte[4];
        fp.read(bytes);
        return ((bytes[3] & 0xff) << 24) | ((bytes[2] & 0xff) << 16)
               | ((bytes[1] & 0xff) << 8) | (bytes[0] & 0xff);
    }

    private String readNullTerminatedString() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (true) {
            byte b = fp.readByte();
            if (b == 0) {
                break;
            }
            buffer.write(b);
        }
        // They actually use ISO-8859-1 in at least one file name.
        return buffer.toString("ISO-8859-1");
    }
}
