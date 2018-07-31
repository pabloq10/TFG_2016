#include <Arduino.h>
#include <Wire.h>
#include "sensor.h"
int xyz_datos [3];
char POWER_CTL = 0x2D;	  // Registro de control de Power, permite poner el acelerómetro en Sleep-mode, Stand-by, etc
char DATA_FORMAT = 0x31;  // Permite seleccionar el rango de aceleración +/-2g, +/-4g, +/-8g, +/-16g

char X0 = 0x32;	          // EjeX Byte bajo (Dato 0)
char X1 = 0x33;	          // EjeX Byte alto (Dato 1)
char Y0 = 0x34;	          // EjeY Byte alto (Dato 0)
char Y1 = 0x35;	          // EjeY Byte alto (Dato 1)
char Z0 = 0x36;	          // EjeZ Byte alto (Dato 0)
char Z1 = 0x37;	          // EjeZ Byte alto (Dato 1)


///////////////////////
//  GIROSCOPIO       //
///////////////////////
char PWR_MGMT_1 = 0x6B;    // Programar a 0x00
char SMPLRT_DIV = 0x19;   // Programar a 0x47 para obtener un bit-rate y un ancho de banda similar a los ADXL345
char CONFIG = 0x1A;       // Programar a 0x2B para un Ancho de banda = 44Hz y sincronizado con el eje X
char ACCEL_CONFIG = 0x1C; // Programar a +/-4g = 0x08. +/-2g = 0x00. +/-8g = 0x10 y +/-16g = 0x18
// ¡¡Ojo!! los datos llegan en 16 bits y, al contrario que el ADXL345, se lee primero el MSB
char Xg0 = 0x3B;	  // EjeX Byte alto (Dato 1)
char Xg1 = 0x3C;	  // EjeX Byte bajo (Dato 0)
char Yg0 = 0x3D;	  // EjeY Byte alto (Dato 1)
char Yg1 = 0x3E;	  // EjeY Byte bajo (Dato 0)
char Zg0 = 0x3F;	  // EjeZ Byte alto (Dato 1)
char Zg1 = 0x40;	  // EjeZ Byte bajo (Dato 0)




// PAra los rangos DATA_FORMAT = 0x31 B1 B0 : 2g=00 4g=01 8g=10 16g=11
sensor ::sensor (){		//Creacion de objeto 
	direccion = 0x00;
	tipo_sensor=ACELEROMETRO;
	divisor=1;
	g_range=0x00;
	id=0;
}



void sensor :: inicializacion_sensor(byte address,int id_device){
    id =id_device;
	direccion = address;
    if (direccion ==0x69 ){	                                //direccion =direccion de giroscopio
        tipo_sensor= GIROSCOPO;
        byte registro_control=lectura_registro(ACCEL_CONFIG);	//Configuracion de los 
        programarRegistro( SMPLRT_DIV, 0x47);			//registros del giroscopo
        programarRegistro( CONFIG, 0x2B);
        rango_g_cambio('2');					//Rango +/- 4g	
    }
	else {
		tipo_sensor=ACELEROMETRO;
		rango_g_cambio('2');                            //Rango +/- 4g
	}							
}

String sensor :: get_direccion(){
      return "0x"+String(direccion,HEX);
    }
String sensor :: get_tipo(){
      return String(tipo_sensor,BIN);
    }


//Configuracion del registro del rango de aceleracion 
void sensor :: rango_g_cambio(char rango){
	if(tipo_sensor==GIROSCOPO){
        switch (rango){		
		case '1': g_range=0x00;
                          divisor=1;
					break;
		case '2': g_range=0x08;
                          divisor=2;
					break;	
		case '3': g_range=0x10;
                          divisor=4;
					break;
                          
		case '4': g_range=0x18;
                          divisor=8;
					break;					
	    }
       byte registro_control=lectura_registro(ACCEL_CONFIG);	//Actualizacion del registro
       registro_control=registro_control & ~(0x18);		//	segun el rango deseado
       registro_control=registro_control | g_range;
       programarRegistro(ACCEL_CONFIG, registro_control);
       active_mode();
  }
    else if(tipo_sensor==ACELEROMETRO){
      switch (rango){
		case '1': g_range=0x00;
                          divisor=0.5;
					break;
		case '2': g_range=0x01;
                          divisor=1;
					break;	
		case '3': g_range=0x02;
                          divisor=2;
					break;
                          
		case '4': g_range=0x03;
                          divisor=4;
					break;					
	}
	
	Wire.begin();
	standby_mode();	                                            //Paso a modo standby para poder cambiar el rango dinamico
	byte registro_control = lectura_registro(DATA_FORMAT);	    //Actualizacion del registro segun el rango deseado
	registro_control=registro_control & ~(0x03);                //Limpio los bits 0 y 1
        programarRegistro(DATA_FORMAT,registro_control |(g_range));
	active_mode();	                                            //Paso a modo active para que empiece a obtener aceleraciones
	}
}

//POWER_CTL = 0x2D; el bit 3 =0
  void sensor ::  standby_mode(){ 
    if(tipo_sensor==GIROSCOPO){
        byte registro_control=lectura_registro(PWR_MGMT_1);  //Bit 6 a 0
        registro_control=registro_control | 0x40;
        programarRegistro( PWR_MGMT_1, registro_control);
        }
    if(tipo_sensor==ACELEROMETRO){
  	byte registro_control=lectura_registro(POWER_CTL); //Actualizacion del registro POWER_CTL en modo standby
  	programarRegistro(POWER_CTL,registro_control & ~(0x08));
        }
 	
}

//POWER_CTL = 0x2D; el bit 3 =1
void sensor :: active_mode(){
    if(tipo_sensor==GIROSCOPO){
          byte registro_control=lectura_registro(PWR_MGMT_1);  //Bit 6 a 0
          registro_control=registro_control & ~(0x40);
          programarRegistro( PWR_MGMT_1, registro_control);
          }
    else if(tipo_sensor==ACELEROMETRO){
		byte registro_control=lectura_registro(POWER_CTL); //Actualizacion del registro POWER_CTL en modo activo
		programarRegistro(POWER_CTL,registro_control | (0x08));
	}
}

void sensor :: lectura_datos (float *xyz_datos){
  
	byte xyz_datos_separados[6];	//Bytes de obtencion de las 3 aceleraciones para byte alto y byte bajo
	Wire.beginTransmission(direccion); //Inicio de la transmision
	if(tipo_sensor==GIROSCOPO){	//Envio a los dispositivos de la direccion requerida para leer segun el tipo de dispositivo
		Wire.write(Xg0);
		}
	else if(tipo_sensor==ACELEROMETRO){
		Wire.write(X0);
		}	
	int i=Wire.endTransmission();
                
	
        if(true){
        	Wire.beginTransmission(direccion);
        	Wire.requestFrom(direccion, 6); //Se solicitan 6 bytes al esclavo
                int temporizador=0;
                
        	  while(Wire.available() < 6){  //Tiempo de espera hasta que son recibidos los 6 bytes
                        if (temporizador<50)temporizador++;
                        else{
                            xyz_datos[0]=0;
                            xyz_datos[1]=0;
                            xyz_datos[2]=0;
                            return;
                        }
                  }	
        		
        	for(int x = 0 ; x < 6 ; x++){	//Lectura de los 6 bytes mandados por el esclavo
        		xyz_datos_separados[x] = Wire.read();
        	}
        	Wire.endTransmission();
        
        	if(tipo_sensor==GIROSCOPO){
                    int  xyz_datos_int[3];
                    for(int x = 0 ; x < 3 ; x++){
            		xyz_datos_int[x]=  ( (((int)(xyz_datos_separados[2*x])) << 8) | xyz_datos_separados[2*x+1]);	//Obtencion del dato completo por union del byte alto y bajo
            		xyz_datos_int[x]=xyz_datos_int[x]>> 6;	//Desplazamiento de bits ya que los datos mandados son de 16 bits
                        xyz_datos[x]=xyz_datos_int[x];
                    }
                    
                    
            		//Paso de datos en relacion a fuerzas G	
            		xyz_datos[0]=(xyz_datos[0]-(15/divisor))/(255/divisor);
            		xyz_datos[1]=(xyz_datos[1]-(8.5/divisor))/(254.5/divisor);
            		xyz_datos[2]=(xyz_datos[2]-(13/divisor))/(263/divisor);
        	}
        	else if(tipo_sensor==ACELEROMETRO){
                      
                      for(int x = 0 ; x < 3 ; x++){
            			xyz_datos[x]=  ( (((int)(xyz_datos_separados[2*x+1])) << 8) | xyz_datos_separados[2*x]);   
                      }
                    
                    if (direccion == 0x1d){
                            
                           //Paso de datos en relacion a fuerzas G
                           xyz_datos[0]=(xyz_datos[0]-(-154/divisor))/(133/divisor);
            		   xyz_datos[1]=(xyz_datos[1]-(-267/divisor))/(132/divisor);
                	   xyz_datos[2]=(xyz_datos[2]-(-150/divisor))/(135/divisor);
                             
                              }
                     else if (direccion == 0x53){
            		//Paso de datos en relacion a fuerzas G	
                        xyz_datos[0]=(xyz_datos[0]-(-35/divisor))/(132/divisor);
            		xyz_datos[1]=(xyz_datos[1]-(65/divisor))/(130/divisor);
            		xyz_datos[2]=(xyz_datos[2]-(136/divisor))/(136/divisor);
                      }
                }
        }
}

void sensor :: programarRegistro( byte registro, byte valor) {
  Wire.beginTransmission(direccion);              // Comienza la transmisión con el ADLX345
  Wire.write(registro);                           // Dirección del registro a programar
  Wire.write(valor);                            // Dato a programar
  Wire.endTransmission();                       // Fin de la transmisión
}

//Funcion que devuelve el valor de un registro
byte sensor :: lectura_registro(byte registro){
	Wire.beginTransmission(direccion);
	Wire.write(registro|0x80);
	Wire.endTransmission(false);
	Wire.requestFrom(direccion,1);
	while(!Wire.available()) ;  //tiempo de espera hasta que llega el registro del acelerometro
	byte registro_control = Wire.read();
	
	return registro_control;
}


//Funcion que devuelve el id asignado del dispositivo
int sensor :: get_id(){
  return id;
}

//Funcion que devuelve el valor del rango al cual esta configurado el dispositivo
String sensor :: get_range(){
  if(tipo_sensor==GIROSCOPO){
	  if(g_range==0x00)  return "2";
      else if(g_range==0x08)  return "4";
      else if(g_range==0x10)  return "8";
      else if(g_range==0x18)  return "16";    
  }
  else if (tipo_sensor==ACELEROMETRO){
      if(g_range==0x00)  return "2";
      else if(g_range==0x01)  return "4";
      else if(g_range==0x02)  return "8";
      else if(g_range==0x03)  return "16";
      }
}
