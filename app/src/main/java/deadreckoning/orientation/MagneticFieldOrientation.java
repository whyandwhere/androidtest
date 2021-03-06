package deadreckoning.orientation;


import org.ejml.data.SimpleMatrix;

import deadreckoning.extra.ExtraFunctions;

public final class MagneticFieldOrientation {

    private MagneticFieldOrientation() {}

    private static SimpleMatrix m_rotationNED = new SimpleMatrix(new double[][]{{0,1,0},
                                                                                {1,0,0},
                                                                                {0,0,-1}});

    public static float[][] getOrientationMatrix(float[] G_values, float[] M_values, float[] M_bias) {


        //消除磁场初始值的偏差
        double[][] M_init_unbiased = ExtraFunctions.vectorToMatrix(removeBias(M_values, M_bias));

        SimpleMatrix m_M_init_unbiased = new SimpleMatrix(M_init_unbiased);
//        SimpleMatrix m_M_values = new SimpleMatrix(ExtraFunctions.vectorToMatrix(ExtraFunctions.floatVectorToDoubleVector(M_values)));
//        SimpleMatrix m_M_bias   = new SimpleMatrix(ExtraFunctions.vectorToMatrix(ExtraFunctions.floatVectorToDoubleVector(M_bias)));
        SimpleMatrix m_G_values = new SimpleMatrix(ExtraFunctions.vectorToMatrix(ExtraFunctions.floatVectorToDoubleVector(G_values)));

        m_M_init_unbiased =  m_rotationNED.mult(m_M_init_unbiased);
//        m_M_values =  m_rotationNED.mult(m_M_values);
//        m_M_bias   =  m_rotationNED.mult(m_M_bias);
        m_G_values =  m_rotationNED.mult(m_G_values);

//        float[][] M_m_values = ExtraFunctions.denseMatrixToArray(m_M_values.getMatrix());
//        float[][] M_m_bias = ExtraFunctions.denseMatrixToArray(m_M_bias.getMatrix());
        float[][] G_m_values = ExtraFunctions.denseMatrixToArray(m_G_values.getMatrix());

        //根据重力计算横滚角和俯仰角
        double G_r = Math.atan2(G_m_values[1][0], G_m_values[2][0]);
        double G_p = Math.atan2(-G_m_values[0][0], G_m_values[1][0] * Math.sin(G_r) + G_m_values[2][0] * Math.cos(G_r));

        //创建表示横滚角和俯仰角的旋转矩阵
        double[][] R_rp = {{Math.cos(G_p), Math.sin(G_p) * Math.sin(G_r), Math.sin(G_p) * Math.cos(G_r)},
                            {0, Math.cos(G_r), -Math.sin(G_r)},
                            {-Math.sin(G_p), Math.cos(G_p) * Math.sin(G_r), Math.cos(G_p) * Math.cos(G_r)}};

//        double[][] M_init_unbiased = ExtraFunctions.vectorToMatrix(removeBias(M_m_values, M_m_bias));

        SimpleMatrix m_R_rp = new SimpleMatrix(R_rp);
//        SimpleMatrix m_M_init_unbiased = new SimpleMatrix(M_init_unbiased);

        //根据重力读数旋转磁场值
        SimpleMatrix m_M_rp = m_R_rp.mult(m_M_init_unbiased);

        //calc heading (rads) from rotated magnetic field
        double h = -1*(Math.atan2(-m_M_rp.get(1), m_M_rp.get(0)) + 11.0 * Math.PI/180.0);

        //rotation matrix representing heading, is negative when moving East of North
        double[][] R_h = {{Math.cos(h), -Math.sin(h), 0},
                          {Math.sin(h),  Math.cos(h), 0},
                          {0,            0,           1}};

        //calc complete (initial) rotation matrix by multiplying roll/pitch matrix with heading matrix
        SimpleMatrix m_R_h = new SimpleMatrix(R_h);
        SimpleMatrix m_R = m_R_rp.mult(m_R_h);

        //rotate heading by minus pi/2, so that heading == 0 corresponds with North
//        double[][] R_yx = {{0.0, -1.0, 0.0},
//                           {1.0,  0.0, 0.0},
//                           {0.0,  0.0, 0.0}};
//        SimpleMatrix m_R_yx = new SimpleMatrix(R_yx);
//        m_R = m_R_yx.mult(m_R);

        return ExtraFunctions.denseMatrixToArray(m_R.getMatrix());

    }

    public static float getHeading(float[] G_values, float[] M_values, float[] M_bias) {
        float[][] orientationMatrix = getOrientationMatrix(G_values, M_values, M_bias);
        return (float) Math.atan2(orientationMatrix[1][0], orientationMatrix[0][0]);
    }

//    private static double[] removeBias(float[][] M_init, float[][] M_bias) {
//        //ignoring the last 3 values of M_init, which are the android-calculated iron biases
//        double[] M_biasRemoved = new double[3];
//        for (int i = 0; i < 3; i++)
//            M_biasRemoved[i] = M_init[i][0] - M_bias[i][0];
//        return M_biasRemoved;
//    }

    private static double[] removeBias(float[] M_init, float[] M_bias) {
        //忽略M_init的最后3个值，这是android计算的磁偏差
        double[] M_biasRemoved = new double[3];
        for (int i = 0; i < 3; i++)
            M_biasRemoved[i] = M_init[i] - M_bias[i];
        return M_biasRemoved;
    }

}
