package com.pchauvet.heardreality.MathUtils;

public class Quaternion{
    public float w;
    public float x;
    public float y;
    public float z;

    public Quaternion(float w, float x, float y, float z){
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Quaternion(){
        this.w = 1;
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    public void set(float w, float x, float y, float z){
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Quaternion copy(){
        return new Quaternion(this.w, this.x, this.y, this.z);
    }

    public EulerAngles toEulerAngles() {
        // roll (x-axis rotation)
        float sinr_cosp = 2 * (w * x + y * z);
        float cosr_cosp = 1 - 2 * (x * x + y * y);
        float roll = (float) (Math.atan2(sinr_cosp, cosr_cosp) * 180 / Math.PI);

        // pitch (y-axis rotation)
        float sinp = 2 * (w * y - z * x);
        float pitch;
        if (Math.abs(sinp) >= 1){
            pitch = (float) Math.copySign(Math.PI / 2, sinp); // use 90 degrees if out of range
        }else{
            pitch = (float) Math.asin(sinp);
        }
        pitch = (float) (pitch * 180 / Math.PI);

        // yaw (z-axis rotation)
        float siny_cosp = 2 * (w * z + x * y);
        float cosy_cosp = 1 - 2 * (y * y + z * z);
        float yaw = (float) (Math.atan2(siny_cosp, cosy_cosp) * 180 / Math.PI);

        EulerAngles angles = new EulerAngles(roll, pitch, yaw);
        return angles;
    }

    public Quaternion getConjugate() {
        return new Quaternion(w, -x, -y, -z);
    }

    // We multiply this quaternion by q (this*q) with a Hamilton product
    public Quaternion multiply(final Quaternion q) {
        final float w = this.w * q.w - this.x * q.x - this.y * q.y - this.z * q.z;
        final float x = this.w * q.x + this.x * q.w + this.y * q.z - this.z * q.y;
        final float y = this.w * q.y - this.x * q.z + this.y * q.w + this.z * q.x;
        final float z = this.w * q.z + this.x * q.y - this.y * q.x + this.z * q.w;

        return new Quaternion(w, x, y, z);
    }

    // Decode a quaternion by rotating it backwards by the reference (this * ref-)
    public Quaternion decode(final Quaternion refConjug){
        return (this.multiply(refConjug));
    }

    public String toString(){
        return "W : "+w+" / X : "+x+" / Y : "+y+" / Z :"+z;
    }
}
