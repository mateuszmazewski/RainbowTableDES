package rainbowtable;

import java.nio.ByteBuffer;

public class DESAddNReductionFunctionGenerator implements ReductionFunctionGenerator {

    @Override
    public byte[] reduce(byte[] hash, int n) {
        // input: 64-bit, output: 56-bit

        // big endian
        ByteBuffer buffer = ByteBuffer.wrap(hash);
        ByteBuffer outBuffer = ByteBuffer.allocate(8);
        outBuffer.putLong(buffer.getLong() + n);

        byte[] out = new byte[7];
        // change srcPos when changing endianness
        System.arraycopy(outBuffer.array(), 1, out, 0, 7);
        return out;
    }

}
