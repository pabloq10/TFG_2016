package com.example.pques_000.comunicacionbt;

/**
 * Created by pques_000 on 06/04/2016.
 */
public class acelerometro {
    private static final int n_muestras  = 20;
    private int id;
    private int tipo;	//tipo de aceleromtro. Acelerometro =0, Giroscopo=1
    private String direccion;
	private int rango;
    private float[] x;
    private float[] y;
    private float[] z;
    private String rutaImagen;

    public acelerometro(int id,String direccion,int tipo,int rango) {
        this.id=id;
        this.direccion=direccion;
        this.tipo=tipo;
        this.rango=rango;

        if (tipo==0){
            rutaImagen="mipmap/icono_acel";
        }else if(tipo==1){
            rutaImagen="mipmap/icono_giro";}

        this.x = new float[n_muestras];
        this.y = new float[n_muestras];
        this.z = new float[n_muestras];


        for (int i=0; i<n_muestras;i++){
            this.x[i]=0;
            this.y[i]=0;
            this.z[i]=0;
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTipo() {
        return tipo;
    }

    public void setTipo(int tipo) {
        this.tipo = tipo;
    }

    public int getRango() {
        return rango;
    }

    public void setRango(int rango) {
        this.rango = rango;
    }


    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }



    public float[] getX() {
        return x;
    }
    public float getX(int i) {
        return x[i];
    }

    public void setX(float x) {
        for(int i=n_muestras-2;i>=0;i--){
            this.x[i+1]=this.x[i];
        }
        this.x[0]=x;
    }

    public float[] getY() {
        return y;
    }
    public float getY(int i) {
        return y[i];
    }

    public void setY(float y) {
        for(int i=n_muestras-2;i>=0;i--){
            this.y[i+1]=this.y[i];
        }
        this.y[0]=y;
    }

    public float[] getZ() {
        return z;
    }
    public float getZ(int i) {
        return z[i];
    }

    public void setZ(float z) {
        for(int i=n_muestras-2;i>=0;i--){
            this.z[i+1]=this.z[i];
        }
        this.z[0]=z;
    }


    public String getRutaImagen() {
        return rutaImagen;
    }

    public void setRutaImagen(String rutaImagen) {
        this.rutaImagen = rutaImagen;
    }


}