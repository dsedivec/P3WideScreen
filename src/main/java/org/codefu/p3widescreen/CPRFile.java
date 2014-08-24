package org.codefu.p3widescreen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;

class CPRFile {
    private static final Logger logger = LoggerFactory.getLogger(CPRFile.class);

    private final RandomAccessFile fp;
    private final HashMap<String, IndexEntry> index = new HashMap<>();

    private class IndexEntry {
        // long based on type of RandomAccessFile.seek, but basically
        // a pointless distinction for our little program.
        public final long offset;
        public final int length;

        public IndexEntry(long offset, int length) {
            this.offset = offset;
            this.length = length;
        }
    }

    public CPRFile(File file) throws IOException {
        // TODO: "fp" is a shit name
        fp = new RandomAccessFile(file, "r");
        String signature = readNullTerminatedString();
        if (!signature.equals("ASCARON_ARCHIVE V0.9")) {
            throw new RuntimeException("invalid signature");
        }
        long nextHeader = 0x20;
        while (true) {
            logger.debug("moving to header at {}", nextHeader);
            fp.seek(nextHeader);
            long headerLength;
            try {
                headerLength = readLittleEndianInt();
            } catch (EOFException e) {
                logger.debug("hit EOF trying to read new header, as expected");
                break;
            }
            // This is something like length of the header minus the first
            // four bytes?  We don't appear to need it right now.
            skipBytes(4);
            long numFiles = readLittleEndianInt();
            long nextHeaderOffset = readLittleEndianInt();
            logger.debug("read nextHeaderOffset {}", nextHeaderOffset);
            for (int i = 0; i < numFiles; i++) {
                readIndexEntry();
            }
            nextHeader = nextHeader + headerLength + nextHeaderOffset;
        }
    }

    private void skipBytes(int n) throws IOException {
        int numSkipped = fp.skipBytes(n);
        if (numSkipped != n) {
            throw new RuntimeException(String.format(
                "expected to skip %d byte(s) but only skipped %d",n,
                numSkipped));
        }
    }

    private void readIndexEntry() throws IOException {
        int offset = readLittleEndianInt();
        int length = readLittleEndianInt();
        if (offset <= 0 || length <= 0) {
            throw new RuntimeException("invalid offset and/or length");
        }
        // Don't know what this is, I think it's always 1?
        skipBytes(4);
        String name = readNullTerminatedString();
        logger.debug("read file={} offset={} length={}", name, offset, length);
        index.put(name, new IndexEntry(offset, length));
    }

    private int readLittleEndianInt() throws IOException {
        byte bytes[] = new byte[4];
        int numRead = fp.read(bytes);
        // Other functions don't throw EOFException (they probably should).
        // This one is a special case, since we mostly expect that a
        // proper .CPR file will have a header that points directly to EOF.
        if (numRead == -1) {
            throw new EOFException();
        } else if (numRead != 4) {
            throw new RuntimeException(String.format(
                "expected to read 4 bytes but read %d instead", numRead));
        }
        // b & 0xff is necessary before shifting as it triggers a
        // promotion to int with sign extension but then clears all
        // but the low bits.
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

    public InputStream getFile(String path) throws IOException {
        IndexEntry indexEntry = index.get(path);
        if (indexEntry == null) {
            throw new RuntimeException(String.format(
                "can't find file \"%s\"", path));
        }
        fp.seek(indexEntry.offset);
        byte data[] = new byte[indexEntry.length];
        int numBytes = fp.read(data);
        if (numBytes != indexEntry.length) {
            throw new RuntimeException(String.format(
                "expected to read %d byte(s) but read %d instead",
                indexEntry.length, numBytes));
        }
        return new ByteArrayInputStream(data);
    }
}
