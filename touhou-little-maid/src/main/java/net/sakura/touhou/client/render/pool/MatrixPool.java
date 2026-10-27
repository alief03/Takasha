package net.sakura.touhou.client.render.pool;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * MatrixPool
 * Pool alokasi memori nol (Zero-Allocation Scratch Pool) untuk matriks transformasi dan vektor 3D.
 * Menghilangkan alokasi objek sementara di loop render per-frame untuk mencegah Stop-The-World GC stutter
 * pada spesifikasi RAM 6 GB / Ryzen 3 7320U.
 */
public class MatrixPool {

    private static final int POOL_SIZE = 16;
    private static final Matrix4f[] MATRICES = new Matrix4f[POOL_SIZE];
    private static final Vector3f[] VECTORS = new Vector3f[POOL_SIZE];

    private static int matrixIndex = 0;
    private static int vectorIndex = 0;

    static {
        for (int i = 0; i < POOL_SIZE; i++) {
            MATRICES[i] = new Matrix4f();
            VECTORS[i] = new Vector3f();
        }
    }

    public static synchronized Matrix4f obtainMatrix() {
        Matrix4f m = MATRICES[matrixIndex];
        matrixIndex = (matrixIndex + 1) % POOL_SIZE;
        m.identity();
        return m;
    }

    public static synchronized Vector3f obtainVector(float x, float y, float z) {
        Vector3f v = VECTORS[vectorIndex];
        vectorIndex = (vectorIndex + 1) % POOL_SIZE;
        v.set(x, y, z);
        return v;
    }
}
