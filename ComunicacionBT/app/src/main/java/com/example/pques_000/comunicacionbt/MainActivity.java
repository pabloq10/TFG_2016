package com.example.pques_000.comunicacionbt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    //BLUETOOTH
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //UUID que define la conexion
    //Estado de la conexion Bluetooth
    protected static final int SUCCESS_CONNECT = 0;
    protected static final int MESSAGE_READ = 1;
    protected static final int ERROR_CONNECTION=2;

    BluetoothAdapter BT_Adapter;    //Adaptador de Bluetooth
    ArrayAdapter<String> arrayadapter_dispositivos;	//texto a mostrar de cada dispositivo BT a mostrar
    Set<BluetoothDevice> array_dispositivos;    //Array de dispositivos Bluetooth
    ArrayList<BluetoothDevice> arrayBT_dispositivos;    //Array de dispositivos Bluetooth que se mostraran en el ListView

    
    BluetoothDevice BT_seleccionado;	//Dispositivo BT conectado

    ThreadConectado connectedThread;    //Hilo de conexion Bluetooth



    int tipo_lectura;
    protected static final int LECTURA_NORMAL=0;    //Tipo de lectura
    protected static final int LECTURA_N_DEVICES=1; //  en el cual se encuentra

    Timer timer;    //Interrupcion cada cierto tiempo para iniciar la lectura de datos
    boolean activo_recibir;		//Habilita la ejecucion del timer

    //Layout activity_main.xml
    Switch boton_switch;    //Switch de ON/OFF de Bluetooth
    Button boton_buscar;    //Boton de inicio de busqueda de dispositivos Bluetooth
    ListView lista_dispositivos;    //ListView en el cual se muestran todos los dispositivos BT

    ProgressDialog progressDialog;  //Ventana de informacion

    //Acelerometros
    ListView lista_lectura_acelerometros;   //Listview donde se mostraran los datos de los acelerometros
    ArrayList<acelerometro> array_lista_lectura_acelerometros;	//Array de los acelerometros conectados
    list_view_acelerometros adapter; //adaptador entre el ListView y el array de los acelerometros
    acelerometro acelerometro_seleccionado;		//acelerometro seleccionado del ListVi
    int n_dispositivos; 		//Numero de acelerometros conectados
    acelerometro acelerometro[];    //Vector de acelerometros


    Activity actividad_actual;

    //Handler de conexion entre el hilo(Thread) y la parte gráfica
    Handler BT_Handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            //Analisis del estado del estado de la conexion BT
            switch(msg.what){
                case SUCCESS_CONNECT:    //Conexion = Conectado
                    progressDialog.cancel(); // Se cierra la ventana de informacion en el que se mostraba que la conexion se estaba llevando acabo
                    connectedThread = new ThreadConectado((BluetoothSocket)msg.obj,BT_Handler); //Se crea un hilo de conexion
                    Toast.makeText(getApplicationContext(), "CONECTADO", Toast.LENGTH_SHORT).show(); //
                    setContentView(R.layout.activity_transmision);		//Paso a la segunda pantalla, al de visualizacion de aceleraciones
                    lista_lectura_acelerometros=(ListView)findViewById(R.id.listaLecturaAcel);
                    lista_lectura_acelerometros.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
						//Pulsacion corta en el ListView
                        public void onItemClick(AdapterView adapterView, View view, int i, long l) {
                            View container = lista_lectura_acelerometros.getChildAt(i);
							//Mostrar u ocultar grafico dinamico
                            if (container.findViewById(R.id.Grafica).getVisibility() == View.GONE) {
                                container.findViewById(R.id.Grafica).setVisibility(View.VISIBLE);
                            } else if (container.findViewById(R.id.Grafica).getVisibility() == View.VISIBLE) {
                                container.findViewById(R.id.Grafica).setVisibility(View.GONE);
                            }
                        }
                    });
					//Pulsacion larga en el ListView
                    lista_lectura_acelerometros.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                                       int pos, long id) {
                            popupMenu(arg1,array_lista_lectura_acelerometros.get(pos).getId());	//Despliegue de un menu donde elegir el rango de escala
                            return true;
                        }

                    });

                    adapter = new list_view_acelerometros(actividad_actual, array_lista_lectura_acelerometros);
                    lista_lectura_acelerometros.setAdapter(adapter);
                    pedir_n_devices();	// Pedir informacion al Arduino sobre los acelerometros conectados
                    break;
                case ERROR_CONNECTION:	//Se produce un fallo durante la conexion
                    Toast.makeText(getApplicationContext(), "error al conectarse. Reintentelo", Toast.LENGTH_SHORT).show(); //Aviso
                    progressDialog.cancel();
                    break;
                case MESSAGE_READ: //Durante las lecturas en la comunicacion BT
                    byte[] readBuf = (byte[])msg.obj; //Lectura del buffer
                    String string = new String(readBuf);	//Paso de la lectura a cadena de caracteres
                    analisis_texto(string);			//Inicio de la evaluacion del la cadena entrante
                    tipo_lectura=LECTURA_NORMAL; 
                    activo_recibir   = true;
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actividad_actual=this;
        setContentView(R.layout.activity_main);
        arrayBT_dispositivos = new ArrayList<BluetoothDevice>();
        arrayadapter_dispositivos = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,0);
        BT_Adapter= BluetoothAdapter.getDefaultAdapter();
		boton_buscar=(Button)findViewById(R.id.botonBuscar);	//Boton de buscar disp Bluetooth
        boton_switch=(Switch)findViewById(R.id.botonSwitch);	//Boton de encender el BT
        boton_switch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                if (boton_switch.isChecked()) {
                    encender_bluetooth();
                } else {
                    apagar_bluetooth();
                }
            }

        });
		//Alerta de que tu movil no tiene BT
        if (BT_Adapter==null){
			//Mostrar ventana de aviso de no módulo BLUETOOTH
            AlertDialog.Builder alerta_no_BT = new AlertDialog.Builder(this); 
            alerta_no_BT.setTitle("Alerta");
            alerta_no_BT.setMessage("Su móvil no es compatible para Bluetooth");
            alerta_no_BT.setCancelable(false);
            alerta_no_BT.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface alerta_no_BT, int id) {
                    finish();
                }
            });

            alerta_no_BT.show();
        }
        else{
            //Comprobacion del bluetooth
            if (BT_Adapter.isEnabled()){
                encender_bluetooth();
            }
            else {

                apagar_bluetooth();
            }
        }

        timer= new Timer();	//Creacion del timer
		timer.scheduleAtFixedRate(new TimerTask() {	//Funcion a realizar cada periodo 
            @Override
            public void run() {
                if(activo_recibir== true) {
                    Refrescar();
                }
            }
        }, 0, 150);

        activo_recibir= false;

        lista_dispositivos= (ListView)findViewById(R.id.ListaDeDispositivos);
        lista_dispositivos.setAdapter(arrayadapter_dispositivos);
        lista_dispositivos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
			//Funcion de conexion tras la pulsacion en algun dispositivo BT del ListView
            public void onItemClick(AdapterView<?> parent, View v, int posicion, long id) {
                
                if (BT_Adapter.isDiscovering()) {
                    BT_Adapter.cancelDiscovery();
                }


                BT_seleccionado = arrayBT_dispositivos.get(posicion);	//Obtencion del dispositivo seleccionado
                Toast.makeText(getApplicationContext(), String.valueOf(BT_seleccionado.getName()), Toast.LENGTH_SHORT).show();
                progressDialog = ProgressDialog.show(MainActivity.this,
                        "Espere", "Intentando conectar");
                ConectarThread connect = new ConectarThread(BT_seleccionado,BT_Adapter,BT_Handler);
                connect.start();	//Inicio del intento de conexion
            }
        });


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // SEGUNDO PANTALLAZO
        array_lista_lectura_acelerometros =  new ArrayList<acelerometro>();
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        
    }
	// //Funcion de tiempo de espera
    private void esperar (int milis) {
        try {
            Thread.sleep(milis);

        } catch (Exception e) {
            Log.e("tiempo", "no se espero");
        }
    }
	
	//Funcion de encender el Bluetooth
    private void encender_bluetooth(){
        boton_switch.setChecked(true);
        boton_buscar.setVisibility(View.VISIBLE);
        BT_Adapter.enable();
    }
	
	//Funcion de apagar el Bluetooth
    private void apagar_bluetooth(){
        boton_switch.setChecked(false);
        boton_buscar.setVisibility(View.INVISIBLE);
        BT_Adapter.disable();

    }
	//Funcion que reinicia el trabajo de lectura del buffer
    private void Refrescar(){
        connectedThread.run();

    }
	
	//Funcion de enviar datos por Bluetooth
    public  void enviar_datos(String texto_a_enviar, int etapa){ //etapa inicial=1 para que inicie el hilo   --- Etapa intermedia=0 para no crear otro hilo
        String texto="<"+texto_a_enviar+">";
        connectedThread.write(texto.getBytes());
        if(etapa==1) {
            esperar(300); //Para el debugging ponerlo a 300
            connectedThread.run();
        }
    }
	
	//Funcion para pedir la informacion sobre los acelerometros conectados
    public void pedir_n_devices(){
        tipo_lectura=LECTURA_N_DEVICES;
        enviar_datos("#numdev#",1);
    }
	
	//Funcion que analiza la cadena entrada por Bluetooth
    private void analisis_texto (String texto){
        int posicion_inicial;
        int posicion_final;
        String sub_string;
        switch (tipo_lectura) {		//Dependiendo del estado en el que se encuentre se hace de un metodo u otro
            case (LECTURA_NORMAL):	//Lectura normal de aceleraciones
                for (int m=0;m<n_dispositivos;m++) {
                    posicion_inicial = texto.indexOf('{');
                    if (posicion_inicial == -1) return;
                    sub_string = texto.substring(posicion_inicial + 1);
                    posicion_final = sub_string.indexOf('}');
                    if (posicion_final == -1) return;
                    texto=sub_string.substring(posicion_final + 1);
                    String sub_string_final = sub_string.substring(0, posicion_final);

                    posicion_final = sub_string_final.indexOf(':');
                    String sub_string_indicador = sub_string_final.substring(0, posicion_final);
                    posicion_inicial = posicion_final;
                    posicion_final = sub_string_final.indexOf('$');
                    String sub_string_x = sub_string_final.substring(posicion_inicial + 1, posicion_final);
                    sub_string_final = sub_string_final.substring(posicion_final + 1);

                    posicion_final = sub_string_final.indexOf('$');
                    String sub_string_y = sub_string_final.substring(0, posicion_final);
                    String sub_string_z=sub_string_final.substring(posicion_final + 1);

                    acelerometro acelerometro_cambiado = array_lista_lectura_acelerometros.get(Integer.parseInt(sub_string_indicador)); //Obtencion del acelerometro a actualizar
                    acelerometro_cambiado.setX(Float.parseFloat(sub_string_x));
                    acelerometro_cambiado.setY(Float.parseFloat(sub_string_y));
                    acelerometro_cambiado.setZ(Float.parseFloat(sub_string_z));
                    array_lista_lectura_acelerometros.set(Integer.parseInt(sub_string_indicador), acelerometro_cambiado);
                    adapter.notifyDataSetChanged(); //Actualizacion del acelerometro
                    esperar(10);
                }
                break;

            case (LECTURA_N_DEVICES):	//Lectura de informacion de los acelerometros
                posicion_inicial = texto.indexOf('<');
                if (posicion_inicial == -1){
                    pedir_n_devices();
                    return;}

                sub_string = texto.substring(posicion_inicial + 1);
                posicion_final = sub_string.indexOf('>');
                if (posicion_final == -1) {
                    pedir_n_devices();
                    return;}
                sub_string = sub_string.substring(0, posicion_final);
                posicion_final = sub_string.indexOf(':');
                String string_num = sub_string.substring(0, posicion_final);
                sub_string = sub_string.substring( posicion_final+1);
                n_dispositivos=Integer.valueOf(string_num);
                acelerometro = new  acelerometro [n_dispositivos];

                for(int q=0;q<n_dispositivos;q++){
                    String individual_device = sub_string.substring(0, sub_string.indexOf('#'));
                    sub_string=sub_string.substring(sub_string.indexOf('#')+1);
                    int id= Integer.valueOf( individual_device.substring(0, individual_device.indexOf(':')) );
                    individual_device=individual_device.substring(individual_device.indexOf(':')+1);
                    String direccion= individual_device.substring(0, individual_device.indexOf('$')) ;
                    individual_device=individual_device.substring(individual_device.indexOf('$')+1);
                    String tipo= individual_device.substring(0, individual_device.indexOf('$')) ;
                    String rango=individual_device.substring(individual_device.indexOf('$')+1);
                    acelerometro[q]= new acelerometro(q,direccion,Integer.valueOf(tipo),Integer.valueOf(rango));
                    array_lista_lectura_acelerometros.add(acelerometro[q]);
                }
                tipo_lectura=LECTURA_NORMAL;

        }

    }

	// Funcion de obtener la informacion de los dispositivos BT emparejados
    public void buscar_dispositivos (View view){
        //Actualizamos la lista de dispositivos
        arrayadapter_dispositivos.clear();
        Toast.makeText(getApplicationContext(), "Buscando dispositivos", Toast.LENGTH_SHORT).show();
        array_dispositivos=BT_Adapter.getBondedDevices();
        if (array_dispositivos.size()>0){
            for (BluetoothDevice usuarios : array_dispositivos){
                arrayadapter_dispositivos.add( usuarios.getName() + "\n" + usuarios.getAddress());
                arrayBT_dispositivos.add(usuarios);
            }
        }
    }


	//Cierre de la aplicacion
    public void apagartransimision (View view){
        timer.cancel();
        connectedThread.cancel();
        finish();

    }

	//Funcion de creacion del menu Popup para la eleccion del rango de escala
    public void popupMenu(View view, int _id) {
        //Crea instancia a PopupMenu
        final int id=_id;
        final acelerometro acelerometro_cambiado = array_lista_lectura_acelerometros.get(id);
        PopupMenu popup = new PopupMenu(this,view );
        popup.getMenuInflater().inflate(R.menu.popupmenu, popup.getMenu());
        //registra los eventos click para cada item del menu
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.accion1) {
                    acelerometro_cambiado.setRango(2);
                    enviar_datos("#rango#" + String.valueOf(id) + "$2",0);
                } else if (item.getItemId() == R.id.accion2) {
                    acelerometro_cambiado.setRango(4);
                    enviar_datos("#rango#"+String.valueOf(id)+"$4",0);
                } else if (item.getItemId() == R.id.accion3) {
                    acelerometro_cambiado.setRango(8);
                    enviar_datos("#rango#"+String.valueOf(id)+"$8",0);
                } else if (item.getItemId() == R.id.accion4) {
                    acelerometro_cambiado.setRango(16);
                    enviar_datos("#rango#"+String.valueOf(id)+"$16",0);
                }
                array_lista_lectura_acelerometros.set(id, acelerometro_cambiado);
                adapter.notifyDataSetChanged();
                return true;
            }
        });
        popup.show();
    }




}