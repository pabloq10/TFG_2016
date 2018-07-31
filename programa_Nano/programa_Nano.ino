#include <Wire.h>
#include "sensor.h"

int n_devices=0;
sensor*  dispositivo[3];

//Control de Strings
String mensaje="";
char c=0;
boolean final= false;
boolean inicio=false;
 
boolean scan_devices(){
  n_devices=0;
  byte error, address;
  
	for(address = 1; address < 127; address++ ){
		Wire.beginTransmission(address);
		error = Wire.endTransmission();
	 
		if (error == 0){
			        (dispositivo[n_devices])=  new sensor();	//Creacion de un objeto sensor tras detectar su direccion
				dispositivo[n_devices]->inicializacion_sensor(address,n_devices);
				n_devices++;
		}		
	}
	if (n_devices == 0){
		delay(250);
		return false;
	}
	else{
		return true;
	}
}

void setup(){
  	//I2C
	Wire.begin();
	
	//BT
        Serial.begin(9600);
	while (!Serial);  
    
	while(scan_devices()==false);	//inicio del escaneo de dispositivos conectados en I2C
}




void loop() {
	float xyz_datos [3][n_devices];
	String texto_a_mandar="";    //Señal de inicio de texto
	for (int i=0; i<n_devices; i++){
		dispositivo[i]->lectura_datos((xyz_datos[i]));
	}
	
	for (int i=0; i<n_devices; i++){
		texto_a_mandar=texto_a_mandar+"{";	//Aviso de inicio de mensaje de datos			
		texto_a_mandar=texto_a_mandar+dispositivo[i]->get_id();	//identificacion del dispositivo
		texto_a_mandar=texto_a_mandar+":";
		texto_a_mandar=texto_a_mandar+xyz_datos[i][0];	//dato en x
		texto_a_mandar=texto_a_mandar+"$";
		texto_a_mandar=texto_a_mandar+xyz_datos[i][1];	//datos en y
		texto_a_mandar=texto_a_mandar+"$";
		texto_a_mandar=texto_a_mandar+xyz_datos[i][2];	//dato  en z
		texto_a_mandar=texto_a_mandar+"}";	//Aviso de fin de mensaje de datos				
        }
        Serial.println((String)texto_a_mandar); //------------------------------------------------------------------------------------------------------------

 //Mensajes de entrada por bluetooth
	while(Serial.available()>0){
                    c=Serial.read();
              		if(c == '<'){			//inicio de mensaje
              			inicio=true;
              			final=false;
              			mensaje="";			//limpieza de la cadena
              			c=0;
                        }
              		else if (c == '>'){	//fin de mensaje
              			final=true;
              			inicio==false;
              			while(Serial.available()){Serial.read();}		//limpieza de buffer de entrada
              			break;
              		}
              		else if (inicio == true){	//obtencion del mensaje
              				mensaje=mensaje+c;
              			}
         }
                
               
          if (final){
          	final=false;
                int primer_caracter=mensaje.indexOf('#');
                int segundo_caracter=mensaje.indexOf('#',primer_caracter+1);
                String subString = mensaje.substring(primer_caracter+1,segundo_caracter);
                if (subString.equals("numdev")){
                	String envio="<";
          		envio = envio + n_devices;
          		envio = envio + ":";
          	   
          		for(int i=0;i<n_devices;i++){
          		   envio = envio + dispositivo[i]->get_id();
          		   envio = envio + ":";
                           envio = envio + dispositivo[i]->get_direccion();
          	           envio = envio + "$";
                           envio = envio + dispositivo[i]->get_tipo();
          		   envio = envio + "$";
          		   envio = envio + dispositivo[i]->get_range();
          		   envio = envio + "#";
          		}
          		envio = envio + ">";
          		Serial.println(envio);
          	}
                else if (subString.equals("rango")){
                       mensaje=mensaje.substring(segundo_caracter+1);      //Parte sobrante del mensaje despues del tipo de mensaje
                       String identificador = mensaje.substring(0,mensaje.indexOf('$'));
                       String rango_nuevo =mensaje.substring(mensaje.indexOf('$')+1);
                       int _id=0;
                       for(int j=0;j<n_devices;j++){
                            if(identificador.toInt()==dispositivo[j]->get_id()){
                                  if(rango_nuevo=="2")dispositivo[identificador.toInt()]->rango_g_cambio('1');
                                  else if(rango_nuevo=="4")dispositivo[identificador.toInt()]->rango_g_cambio('2');
                                  else if(rango_nuevo=="8")dispositivo[identificador.toInt()]->rango_g_cambio('3');
                                  else if(rango_nuevo=="16")dispositivo[identificador.toInt()]->rango_g_cambio('4'); 
                            }
                       }
                }
          }
 
}

