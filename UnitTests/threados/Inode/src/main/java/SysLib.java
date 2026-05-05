import java.io.*;

/**
 * Mock SysLib for isolated FileSystem testing.
 * Provides minimal disk I/O functionality without kernel dependencies.
 */
public class SysLib {
    private static byte[] diskData = new byte[1000 * 512]; // 1000 blocks of 512 bytes each
    private static boolean diskInitialized = false;

    public static void initDisk() {
        if (!diskInitialized) {
            // Initialize disk with zeros
            for (int i = 0; i < diskData.length; i++) {
                diskData[i] = 0;
            }
            diskInitialized = true;
        }
    }

    public static void clearCache() {
        // Mock implementation - no cache to clear
    }

    public static int rawread(int blockNumber, byte[] buffer) {
        if (blockNumber < 0 || blockNumber >= 1000) {
            return -1;
        }
        if (buffer.length < 512) {
            return -1;
        }
        
        int offset = blockNumber * 512;
        System.arraycopy(diskData, offset, buffer, 0, 512);
        return 0;
    }

    public static int rawwrite(int blockNumber, byte[] buffer) {
        if (blockNumber < 0 || blockNumber >= 1000) {
            return -1;
        }
        if (buffer.length < 512) {
            return -1;
        }
        
        int offset = blockNumber * 512;
        System.arraycopy(buffer, 0, diskData, offset, 512);
        return 0;
    }

    public static void cerr(String message) {
        System.err.print(message);
    }

    public static void short2bytes(short s, byte[] b, int offset) {
        b[offset] = (byte)(s >> 8);
        b[offset + 1] = (byte)s;
    }

    public static short bytes2short(byte[] b, int offset) {
        short s = 0;
        s += b[offset] & 0xff;
        s <<= 8;
        s += b[offset + 1] & 0xff;
        return s;
    }

    public static void int2bytes(int i, byte[] b, int offset) {
        b[offset] = (byte)(i >> 24);
        b[offset + 1] = (byte)(i >> 16);
        b[offset + 2] = (byte)(i >> 8);
        b[offset + 3] = (byte)i;
    }

    public static int bytes2int(byte[] b, int offset) {
        int n = ((b[offset] & 0xff) << 24) + ((b[offset+1] & 0xff) << 16) +
                ((b[offset+2] & 0xff) << 8) + (b[offset+3] & 0xff);
        return n;
    }
}