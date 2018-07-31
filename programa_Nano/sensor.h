#include <Arduino.h>

#define ACELEROMETRO false
#define GIROSCOPO true
       

class sensor{
private : 
        byte direccion;
        boolean tipo_sensor;
	byte g_range;
	int id;
        float divisor;
        void standby_mode ();
        void active_mode ();
	void programarRegistro( byte registro, byte valor);
	byte lectura_registro(byte registro);
public :
        void inicializacion_sensor(byte address,int n_device);
        sensor(); 
        void lectura_datos (float *xyz_datos);
	void rango_g_cambio(char rango);
        String get_direccion();
        String get_tipo();
        int  get_id();
        String get_range();
};
