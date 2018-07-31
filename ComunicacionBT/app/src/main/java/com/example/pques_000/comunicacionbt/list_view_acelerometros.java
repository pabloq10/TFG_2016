package com.example.pques_000.comunicacionbt;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by pques_000 on 14/04/2016.
 */
public class list_view_acelerometros extends BaseAdapter {
    protected Activity activity;
    protected ArrayList<acelerometro> items;

    public list_view_acelerometros(Activity activity, ArrayList<acelerometro> items) {
        this.activity = activity;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }

    @Override
	//Funcion que la actualiza
    public View getView(int position, View contentView, ViewGroup parent) {
        View vi = contentView;

        if (contentView == null) {
            LayoutInflater inflater = (LayoutInflater) activity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            vi = inflater.inflate(R.layout.layout_list_view_acelerometros, null);
        }

        acelerometro item = items.get(position);

        ImageView image = (ImageView) vi.findViewById(R.id.imagen);
        int imageResource = activity.getResources()
                .getIdentifier(item.getRutaImagen(), null,
                        activity.getPackageName());
        image.setImageDrawable(activity.getResources().getDrawable(
                imageResource));

        TextView nombre = (TextView) vi.findViewById(R.id.nombre);
        nombre.setText(String.valueOf(item.getDireccion()));

        TextView tx1 = (TextView) vi.findViewById(R.id.acelx);
        tx1.setText(String.valueOf(item.getX(0)));

        TextView tx2 = (TextView) vi.findViewById(R.id.acely);
        tx2.setText(String.valueOf(item.getY(0)));

        TextView tx3 = (TextView) vi.findViewById(R.id.acelz);
        tx3.setText(String.valueOf(item.getZ(0)));

        XYPlot myPlot = (XYPlot) vi.findViewById(R.id.Grafica);
        myPlot.setRangeBoundaries(-1*item.getRango(), item.getRango(), BoundaryMode.FIXED);	//Maximo y minimo en eje Y

        myPlot.setDomainStep(XYStepMode.SUBDIVIDE,10); 	//Incrementos en el eje Y
        myPlot.setRangeStep(XYStepMode.SUBDIVIDE,4);//Incrementos en el eje X


        Number[] seriesXNumbers = new Number[20];
        Number[] seriesYNumbers = new Number[20];
        Number[] seriesZNumbers = new Number[20];
		//Obtencion de la historia de aceleraciones
        for (int i = 0; i < 19; i++) {
            seriesXNumbers[19 - 1 - i] = item.getX(i);
            seriesYNumbers[19 - 1 - i] = item.getY(i);
            seriesZNumbers[19 - 1 - i] = item.getZ(i);
            Log.e("plot", String.valueOf(seriesXNumbers[i]));

        }

        Number[] seriesYXNumbers = new Number[20];
        for (int i = 0; i < 20; i++) {

            seriesYXNumbers[i]=1*i;        }
		
        XYSeries seriesX = new SimpleXYSeries(
                Arrays.asList(seriesXNumbers),
                (SimpleXYSeries.ArrayFormat.Y_VALS_ONLY),
                "X");
        XYSeries seriesY = new SimpleXYSeries(
                Arrays.asList(seriesYNumbers),
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
                "Y");
        XYSeries seriesZ = new SimpleXYSeries(
                Arrays.asList(seriesZNumbers),
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
                "Z");


        //Configuracion de las caracteristicas de visualizacion de los valores en los 3 ejes en la tabla
        LineAndPointFormatter seriesXFormat = new LineAndPointFormatter(
                Color.rgb(0, 200, 0),                   // Color de la línea
                Color.rgb(0, 100, 0),                   // Color del punto
                null, null);              // Relleno

        LineAndPointFormatter seriesYFormat = new LineAndPointFormatter(
                Color.rgb(255, 0, 51),                   // Color de la línea
                Color.rgb(255, 0, 0),                   // Color del punto
                null, null);              // Relleno

        LineAndPointFormatter seriesZFormat = new LineAndPointFormatter(
                Color.rgb(255, 255, 102),                   // Color de la línea
                Color.rgb(255, 255, 0),                   // Color del punto
                null, null);              // Relleno


        // Una vez definida la serie (datos y estilo), la añadimos al panel
        myPlot.clear();
        myPlot.addSeries(seriesX, seriesXFormat);
        myPlot.addSeries(seriesY, seriesYFormat);
        myPlot.addSeries(seriesZ, seriesZFormat);
        myPlot.redraw();
        Log.e("plot", "se carga en el plot");


        return vi;
    }
}
