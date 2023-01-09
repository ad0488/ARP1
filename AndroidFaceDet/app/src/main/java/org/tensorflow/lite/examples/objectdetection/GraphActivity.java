package org.tensorflow.lite.examples.objectdetection;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

public class GraphActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        LineChart chart = findViewById(R.id.chart);
        TinyDB tinydb = new TinyDB(getApplicationContext());

        ArrayList<Double> yawn = tinydb.getListDouble("yawn");
        ArrayList<Double> eyes = tinydb.getListDouble("eyes");
        ArrayList<Double> drowz = tinydb.getListDouble("drowz");

        List<Entry> entries_y = new ArrayList<>();
        List<Entry> entries_e = new ArrayList<>();
        List<Entry> entries_d = new ArrayList<>();
        int i = 0;

        for (Double value : yawn) {
            entries_y.add(new Entry(i + 1, value.floatValue()));
            i++;
        }

        i = 0;

        for (Double value : eyes) {
            entries_e.add(new Entry(i + 1, value.floatValue()));
            i++;
        }

        i = 0;

        for (Double value : drowz) {
            entries_d.add(new Entry(i + 1, value.floatValue()));
            i++;
        }

        LineDataSet dataSet_y = new LineDataSet(entries_y, "Yawning");
        LineDataSet dataSet_e = new LineDataSet(entries_e, "Eyes");
        LineDataSet dataSet_d = new LineDataSet(entries_d, "Drowziness");
        dataSet_y.setColor(Color.GREEN);
        dataSet_y.setDrawCircles(false);
        dataSet_e.setColor(Color.BLUE);
        dataSet_e.setDrawCircles(false);
        dataSet_d.setValueTextColor(Color.BLACK);
        dataSet_d.setDrawCircles(false);
        dataSet_y.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet_e.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet_d.setAxisDependency(YAxis.AxisDependency.LEFT);

        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(dataSet_y);
        dataSets.add(dataSet_e);
        dataSets.add(dataSet_d);
        LineData lineData = new LineData(dataSets);
        chart.setData(lineData);
        chart.setDescription(null);
        //chart.setNoDataText("No Chart Data");
        chart.invalidate();

    }
}